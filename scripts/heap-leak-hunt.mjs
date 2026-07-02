#!/usr/bin/env node
// Heap-snapshot leak hunter for a KiteUI JS app (see scripts/heap-leak-hunt.md for full docs).
//
// Launches headless Chrome, replays a "driver" (a scripted navigation cycle) N times, takes two
// GC-forced heap snapshots (baseline after warmup + after the measured cycles), and reports which
// object classes grew, a retainer path to a GC root for each suspect, and field dumps of the
// leaked instances so you can identify the culprit.
//
// Speaks the Chrome DevTools Protocol over a raw WebSocket (Node >=22 global WebSocket) — no deps.
// Disables the debugger's own async-stack/console retention so it measures real leaks, not the
// inspector pinning cancelled-coroutine exceptions.
//
// Usage:
//   node --max-old-space-size=8192 scripts/heap-leak-hunt.mjs [driverFile]
//   DRIVER=scripts/drivers/home-docs.json node --max-old-space-size=8192 scripts/heap-leak-hunt.mjs
//
// A driver file (.json or .mjs) describes one navigation cycle as a list of steps. See the docs and
// scripts/drivers/*.json for examples. Step types:
//   { "click": "Documentation" }         click the <a>/<button>/[role] whose text or aria-label matches
//   { "clickSelector": "button.foo" }    click the first element matching a CSS selector
//   { "goto": "/docs?query=x" }          SPA-navigate via history.pushState + popstate (no reload)
//   { "type": { "selector": "input", "text": "hi" } }  set an input value + fire 'input'
//   { "waitFor": ".result", "timeout": 5000 }          wait until a selector appears
//   { "wait": 80 }                       sleep N ms
// Driver shape: an array of steps (= the cycle), or { url?, warmup?, cycle, settleMs? }.
//
// Env (overridden by the driver file where applicable):
//   APP_URL   (default http://localhost:8000/)   page to open (driver.url wins)
//   DRIVER    (path)              driver file; also accepted as the first CLI arg
//   CYCLES    (default 30)        measured navigation cycles between the two snapshots
//   WARMUP    (default 20)        cycles run BEFORE the baseline so V8 JIT/shape warmup isn't
//                                 mistaken for a leak
//   SETTLE_MS (default 500)       settle time before each snapshot
//   NAV_A/NAV_B (Documentation/Home)  the two link texts for the built-in default driver
//   PORT      (default 9222)      Chrome remote-debugging port
//   KEEP      (default 0)         if 1, also write tmp/heap-{baseline,after}.heapsnapshot
//   CHROME    (path)              Chrome/Chromium binary override

import { spawn } from 'node:child_process';
import { mkdtempSync, rmSync, writeFileSync, mkdirSync, readFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join, resolve } from 'node:path';
import { pathToFileURL } from 'node:url';

const APP_URL = process.env.APP_URL || 'http://localhost:8000/';
const CYCLES = +(process.env.CYCLES || 30);
// Warm up V8 (JIT + hidden-class shapes) BEFORE the baseline so its one-time growth doesn't
// masquerade as a leak in the delta.
const WARMUP = +(process.env.WARMUP || 20);
const SETTLE_MS = +(process.env.SETTLE_MS || 500);
const NAV_A = process.env.NAV_A || 'Documentation';
const NAV_B = process.env.NAV_B || 'Home';
const DRIVER_PATH = process.env.DRIVER || process.argv[2] || null;
const PORT = +(process.env.PORT || 9222);
const KEEP = process.env.KEEP === '1';
const CHROME = process.env.CHROME || '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome';

// Load a driver file (.json or .mjs/.js) and normalize it to { url?, warmup, cycle, settleMs }.
async function loadDriver() {
  let d;
  if (DRIVER_PATH) {
    if (DRIVER_PATH.endsWith('.json')) d = JSON.parse(readFileSync(DRIVER_PATH, 'utf8'));
    else { const m = await import(pathToFileURL(resolve(DRIVER_PATH)).href); d = m.default ?? m; }
  } else {
    // built-in default: ping-pong between two nav links
    d = { cycle: [{ click: NAV_A }, { wait: 80 }, { click: NAV_B }, { wait: 80 }] };
  }
  if (Array.isArray(d)) d = { cycle: d };
  if (!Array.isArray(d.cycle)) throw new Error('driver must define a "cycle" array of steps');
  return { url: d.url || APP_URL, warmup: d.warmup || d.cycle, cycle: d.cycle, settleMs: d.settleMs ?? SETTLE_MS };
}

// Runs inside the page. Interprets driver steps (see header) so navigation stays in the SPA and
// the JS heap persists across cycles. Installed once; called as __kdriverRun(steps, repeat).
const IN_PAGE_RUNNER = `window.__kdriverRun = async function(steps, repeat){
  const sleep = ms => new Promise(r => setTimeout(r, ms));
  const norm = s => (s == null ? '' : ('' + s)).trim();
  const findClickable = t => [...document.querySelectorAll('a,button,[role=button],[role=link]')]
    .find(x => norm(x.textContent) === t || norm(x.getAttribute('aria-label')) === t);
  async function run(st){
    if ('click' in st){ const el = findClickable(st.click); if(!el) throw new Error('click: not found: ' + st.click); el.click(); }
    else if ('clickSelector' in st){ const el = document.querySelector(st.clickSelector); if(!el) throw new Error('clickSelector: not found: ' + st.clickSelector); el.click(); }
    else if ('goto' in st){ history.pushState(null, '', st.goto); dispatchEvent(new PopStateEvent('popstate', { state: history.state })); }
    else if ('type' in st){ const el = document.querySelector(st.type.selector); if(!el) throw new Error('type: not found: ' + st.type.selector);
      const set = Object.getOwnPropertyDescriptor(Object.getPrototypeOf(el), 'value')?.set; set ? set.call(el, st.type.text) : (el.value = st.type.text);
      el.dispatchEvent(new Event('input', { bubbles: true })); }
    else if ('waitFor' in st){ const t0 = Date.now(), to = st.timeout || 5000; while(!document.querySelector(st.waitFor)){ if(Date.now() - t0 > to) throw new Error('waitFor timeout: ' + st.waitFor); await sleep(25); } }
    else if ('wait' in st){ await sleep(st.wait); }
    else throw new Error('unknown step: ' + JSON.stringify(st));
  }
  for(let i = 0; i < repeat; i++){ for(const st of steps){ await run(st); } }
  return { ok: true, title: document.title, path: location.pathname };
};`;

const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

const userDataDir = mkdtempSync(join(tmpdir(), 'kiteui-heap-'));
const chrome = spawn(CHROME, [
  '--headless=new',
  `--remote-debugging-port=${PORT}`,
  '--remote-allow-origins=*',
  `--user-data-dir=${userDataDir}`,
  '--no-sandbox', '--disable-gpu', '--disable-dev-shm-usage',
  '--no-first-run', '--no-default-browser-check',
  '--disable-background-timer-throttling',
  '--disable-renderer-backgrounding',
  '--disable-backgrounding-occluded-windows',
  '--js-flags=--expose-gc',
  'about:blank',
], { stdio: 'ignore' });

function cleanup() {
  try { chrome.kill('SIGKILL'); } catch {}
  try { rmSync(userDataDir, { recursive: true, force: true }); } catch {}
}
process.on('exit', cleanup);
process.on('SIGINT', () => { cleanup(); process.exit(1); });

// --- minimal CDP client over WebSocket -----------------------------------------------------
async function getBrowserWs() {
  for (let i = 0; i < 100; i++) {
    try {
      const r = await fetch(`http://localhost:${PORT}/json/version`);
      const j = await r.json();
      if (j.webSocketDebuggerUrl) return j.webSocketDebuggerUrl;
    } catch {}
    await sleep(150);
  }
  throw new Error('Chrome DevTools endpoint never came up');
}

function makeConn(ws) {
  let nextId = 1;
  const pending = new Map();
  const listeners = new Set();
  ws.onmessage = (ev) => {
    const msg = JSON.parse(ev.data);
    if (msg.id && pending.has(msg.id)) {
      const { res, rej } = pending.get(msg.id);
      pending.delete(msg.id);
      msg.error ? rej(new Error(msg.method + ': ' + JSON.stringify(msg.error))) : res(msg.result);
    } else {
      for (const l of listeners) l(msg);
    }
  };
  const send = (method, params = {}, sessionId) =>
    new Promise((res, rej) => {
      const id = nextId++;
      pending.set(id, { res, rej });
      ws.send(JSON.stringify({ id, method, params, sessionId }));
    });
  const on = (fn) => { listeners.add(fn); return () => listeners.delete(fn); };
  return { send, on };
}

process.on('unhandledRejection', (e) => { console.error('unhandledRejection:', e); cleanup(); process.exit(1); });

async function main() {
  const driver = await loadDriver();
  console.log(`Launching headless Chrome; target ${driver.url}`);
  console.log(`Driver: ${DRIVER_PATH || `built-in default (${NAV_B} <-> ${NAV_A})`} — ${driver.cycle.length}-step cycle`);
  const browserWs = await getBrowserWs();
  console.log(`DevTools up: ${browserWs.slice(0, 55)}...`);
  const ws = new WebSocket(browserWs);
  await new Promise((res, rej) => { ws.onopen = res; ws.onerror = rej; });
  const { send, on } = makeConn(ws);

  const { targetId } = await send('Target.createTarget', { url: 'about:blank' });
  const { sessionId } = await send('Target.attachToTarget', { targetId, flatten: true });
  const S = (method, params) => send(method, params, sessionId);

  await S('Page.enable');
  await S('Runtime.enable');
  await S('HeapProfiler.enable');
  // Prevent the debugger session itself from retaining objects: CDP otherwise captures async
  // stack traces (pinning cancelled-coroutine exceptions + their detached element trees) and
  // retains console-referenced objects. Without this the snapshot measures a debugger artifact.
  await S('Runtime.setAsyncCallStackDepth', { maxDepth: 0 }).catch(() => {});
  await S('Debugger.setAsyncCallStackDepth', { maxDepth: 0 }).catch(() => {});

  const loaded = new Promise((res) => {
    const off = on((m) => { if (m.method === 'Page.loadEventFired' && m.sessionId === sessionId) { off(); res(); } });
  });
  await S('Page.navigate', { url: driver.url });
  await loaded;
  await sleep(1500);

  const evalAsync = async (expr) => {
    const r = await S('Runtime.evaluate', { expression: expr, awaitPromise: true, returnByValue: true });
    if (r.exceptionDetails) throw new Error(JSON.stringify(r.exceptionDetails));
    return r.result.value;
  };
  await evalAsync(IN_PAGE_RUNNER); // install the step interpreter once
  const runSteps = (steps, repeat) => evalAsync(`window.__kdriverRun(${JSON.stringify(steps)}, ${repeat})`);

  async function takeSnapshot() {
    // Release anything the console/inspector is holding so it doesn't masquerade as a leak.
    await S('Runtime.discardConsoleEntries').catch(() => {});
    await S('Runtime.releaseObjectGroup', { objectGroup: 'console' }).catch(() => {});
    await S('HeapProfiler.collectGarbage');
    await S('HeapProfiler.collectGarbage');
    const chunks = [];
    const off = on((m) => { if (m.method === 'HeapProfiler.addHeapSnapshotChunk' && m.sessionId === sessionId) chunks.push(m.params.chunk); });
    await S('HeapProfiler.takeHeapSnapshot', { reportProgress: false, captureNumericValue: false });
    off();
    return JSON.parse(chunks.join(''));
  }

  console.log(`Warming up ${WARMUP} cycles + baseline snapshot...`);
  await runSteps(driver.warmup, WARMUP);
  await sleep(driver.settleMs);
  const snapA = await takeSnapshot();
  console.log(`Running ${CYCLES} measured cycles...`);
  await runSteps(driver.cycle, CYCLES);
  await sleep(driver.settleMs);
  const snapB = await takeSnapshot();

  if (KEEP) {
    mkdirSync('tmp', { recursive: true });
    writeFileSync('tmp/heap-baseline.heapsnapshot', JSON.stringify(snapA));
    writeFileSync('tmp/heap-after.heapsnapshot', JSON.stringify(snapB));
    console.log('Wrote tmp/heap-baseline.heapsnapshot and tmp/heap-after.heapsnapshot');
  }

  report(snapA, snapB, CYCLES);

  ws.close();
  cleanup();
  process.exit(0);
}

// --- snapshot analysis ---------------------------------------------------------------------
function parse(snap) {
  const meta = snap.snapshot.meta;
  const nf = meta.node_fields.length;
  const nodeTypeField = meta.node_fields.indexOf('type');
  const nameField = meta.node_fields.indexOf('name');
  const sizeField = meta.node_fields.indexOf('self_size');
  const edgeCountField = meta.node_fields.indexOf('edge_count');
  const nodeTypeEnum = meta.node_types[nodeTypeField];

  const ef = meta.edge_fields.length;
  const edgeTypeField = meta.edge_fields.indexOf('type');
  const edgeNameField = meta.edge_fields.indexOf('name_or_index');
  const edgeToField = meta.edge_fields.indexOf('to_node');
  const edgeTypeEnum = meta.edge_types[edgeTypeField];

  return {
    snap, nf, ef, nodeTypeField, nameField, sizeField, edgeCountField, nodeTypeEnum,
    edgeTypeField, edgeNameField, edgeToField, edgeTypeEnum,
    nodes: snap.nodes, edges: snap.edges, strings: snap.strings,
    nodeCount: snap.nodes.length / nf,
    className(ordinal) {
      const base = ordinal * this.nf;
      const type = this.nodeTypeEnum[this.nodes[base + this.nodeTypeField]];
      const name = this.strings[this.nodes[base + this.nameField]];
      return { type, name };
    },
  };
}

function countByClass(p) {
  const map = new Map();
  const { nodes, nf, nodeTypeField, nameField, sizeField, nodeTypeEnum, strings } = p;
  for (let base = 0; base < nodes.length; base += nf) {
    const type = nodeTypeEnum[nodes[base + nodeTypeField]];
    // only care about actual JS objects / strings / arrays for a class-level view
    const name = strings[nodes[base + nameField]];
    const key = `${type} ${name}`;
    let e = map.get(key);
    if (!e) { e = { type, name, count: 0, size: 0 }; map.set(key, e); }
    e.count++;
    e.size += nodes[base + sizeField];
  }
  return map;
}

// Build first-edge offset per node ordinal, then reverse retainer adjacency.
function buildRetainers(p) {
  const { nodes, nf, edgeCountField, edges, ef, edgeToField, nodeCount } = p;
  const firstEdge = new Uint32Array(nodeCount + 1);
  let cum = 0;
  for (let i = 0; i < nodeCount; i++) { firstEdge[i] = cum; cum += nodes[i * nf + edgeCountField]; }
  firstEdge[nodeCount] = cum;
  // retainer counts
  const retCount = new Uint32Array(nodeCount);
  for (let ei = 0; ei < edges.length; ei += ef) {
    const to = edges[ei + edgeToField] / nf;
    retCount[to]++;
  }
  const retStart = new Uint32Array(nodeCount + 1);
  let c = 0;
  for (let i = 0; i < nodeCount; i++) { retStart[i] = c; c += retCount[i]; }
  retStart[nodeCount] = c;
  const retFrom = new Uint32Array(c);   // retaining node ordinal
  const retEdge = new Uint32Array(c);   // edge index (ei/ef) that connects them
  const fill = retStart.slice();
  // walk edges grouped by owner node
  for (let owner = 0; owner < nodeCount; owner++) {
    const start = firstEdge[owner], end = firstEdge[owner + 1];
    for (let e = start; e < end; e++) {
      const ei = e * ef;
      const to = edges[ei + edgeToField] / nf;
      const slot = fill[to]++;
      retFrom[slot] = owner;
      retEdge[slot] = e;
    }
  }
  return { retStart, retFrom, retEdge, firstEdge };
}

// Collect the set of node ids of a given class (to distinguish new-in-after instances).
function idsOfClass(p, type, name) {
  const { nodes, nf, nameField, nodeTypeField, nodeTypeEnum, strings } = p;
  const idField = p.snap.snapshot.meta.node_fields.indexOf('id');
  const set = new Set();
  for (let ord = 0; ord < p.nodeCount; ord++) {
    const base = ord * nf;
    if (nodeTypeEnum[nodes[base + nodeTypeField]] === type && strings[nodes[base + nameField]] === name)
      set.add(nodes[base + idField]);
  }
  return set;
}

// Print the outgoing fields (and string values) of a few instances of a class — reveals identity.
// If excludeIds is given, only dumps instances whose id is NOT in it (i.e. new/leaked ones).
function dumpInstances(p, firstEdge, type, name, howMany = 3, excludeIds = null, ret = null) {
  const { nodes, nf, nameField, nodeTypeField, nodeTypeEnum, strings, edges, ef, edgeToField, edgeNameField, edgeTypeField, edgeTypeEnum } = p;
  const idField = p.snap.snapshot.meta.node_fields.indexOf('id');
  let shown = 0;
  for (let ord = 0; ord < p.nodeCount && shown < howMany; ord++) {
    const base = ord * nf;
    if (nodeTypeEnum[nodes[base + nodeTypeField]] !== type || strings[nodes[base + nameField]] !== name) continue;
    if (excludeIds && excludeIds.has(nodes[base + idField])) continue;
    shown++;
    if (ret) {
      const path = retainerPathFromSeeds(p, ret, [ord]);
      if (path) {
        path.reverse();
        console.log(`  retained by: ${path.map(s => `${s.cls.name}${s.via ? '.' + s.via : ''}`).join(' -> ')}`);
      }
    }
    console.log(`  instance #${ord} ${type}:${name}`);
    const start = firstEdge[ord], end = firstEdge[ord + 1];
    for (let e = start; e < end; e++) {
      const ei = e * ef;
      const etype = edgeTypeEnum[edges[ei + edgeTypeField]];
      const enm = (etype === 'element' || etype === 'hidden') ? `[${edges[ei + edgeNameField]}]` : strings[edges[ei + edgeNameField]];
      const toOrd = edges[ei + edgeToField] / nf;
      const toBase = toOrd * nf;
      const toType = nodeTypeEnum[nodes[toBase + nodeTypeField]];
      const toName = strings[nodes[toBase + nameField]];
      const val = toType === 'string' || toType === 'concatenated string' ? ` = "${String(toName).slice(0, 60)}"` : ` -> ${toType}:${String(toName).slice(0, 40)}`;
      console.log(`      .${enm}${val}`);
    }
  }
}

function edgeLabel(p, e) {
  const ei = e * p.ef;
  const type = p.edgeTypeEnum[p.edges[ei + p.edgeTypeField]];
  const nameOrIndex = p.edges[ei + p.edgeNameField];
  if (type === 'element' || type === 'hidden') return `[${nameOrIndex}]`;
  return p.strings[nameOrIndex] ?? `#${nameOrIndex}`;
}

// BFS backwards from any node of the target class to node ordinal 0 (the root).
function retainerPath(p, ret, targetType, targetName) {
  const { nodes, nf, nameField, nodeTypeField, nodeTypeEnum, strings } = p;
  // pick target instances
  const targets = [];
  for (let ord = 0; ord < p.nodeCount; ord++) {
    const base = ord * nf;
    if (nodeTypeEnum[nodes[base + nodeTypeField]] === targetType &&
        strings[nodes[base + nameField]] === targetName) {
      targets.push(ord);
      if (targets.length >= 200) break;
    }
  }
  return retainerPathFromSeeds(p, ret, targets);
}

function retainerPathFromSeeds(p, ret, targets) {
  if (!targets.length) return null;
  const visited = new Uint8Array(p.nodeCount);
  const parent = new Int32Array(p.nodeCount).fill(-1);
  const parentEdge = new Int32Array(p.nodeCount).fill(-1);
  const queue = [];
  for (const t of targets) { visited[t] = 1; queue.push(t); }
  let root = -1;
  let qi = 0;
  while (qi < queue.length) {
    const n = queue[qi++];
    if (n === 0) { root = 0; break; }
    const cls = p.className(n);
    // synthetic roots (GC roots) are good stopping points too
    if (cls.type === 'synthetic' && n !== 0) { root = n; break; }
    for (let s = ret.retStart[n]; s < ret.retStart[n + 1]; s++) {
      const from = ret.retFrom[s];
      if (!visited[from]) { visited[from] = 1; parent[from] = n; parentEdge[from] = ret.retEdge[s]; queue.push(from); }
    }
  }
  if (root === -1) return null;
  // reconstruct from root down to a target
  const path = [];
  let cur = root;
  while (cur !== -1) {
    const cls = p.className(cur);
    const via = parentEdge[cur] >= 0 ? edgeLabel(p, parentEdge[cur]) : null;
    path.push({ cls, via });
    cur = parent[cur];
  }
  return path;
}

function report(snapA, snapB, cycles) {
  const pA = parse(snapA), pB = parse(snapB);
  const a = countByClass(pA), b = countByClass(pB);
  const rows = [];
  for (const [key, eb] of b) {
    const ea = a.get(key) || { count: 0, size: 0 };
    rows.push({ type: eb.type, name: eb.name, dCount: eb.count - ea.count, dSize: eb.size - ea.size, count: eb.count });
  }
  const totalDelta = rows.reduce((s, r) => s + r.dSize, 0);
  console.log(`\n=== Heap growth over ${cycles} cycles: ${(totalDelta / 1048576).toFixed(2)} MB retained (after forced GC) ===`);

  console.log(`\nTop classes by RETAINED SIZE growth (self_size delta):`);
  rows.filter(r => r.dSize > 0).sort((x, y) => y.dSize - x.dSize).slice(0, 20).forEach(r => {
    console.log(`  +${(r.dSize / 1024).toFixed(1).padStart(9)} KB  ${String(r.dCount).padStart(6)} objs  ${r.type}:${r.name}`);
  });

  console.log(`\nTop classes by COUNT growth (~proportional to ${cycles} cycles => leak):`);
  const byCount = rows.filter(r => r.dCount > 0).sort((x, y) => y.dCount - x.dCount).slice(0, 20);
  byCount.forEach(r => {
    console.log(`  +${String(r.dCount).padStart(6)}  (${(r.dCount / cycles).toFixed(1)}/cycle)  ${(r.dSize / 1024).toFixed(1)} KB  ${r.type}:${r.name}`);
  });

  // Retainer paths for the strongest ACTIONABLE growers. Skip V8-internal infrastructure
  // (JIT code, hidden-class shapes, synthetic roots, system arrays) that is warmup noise, not
  // an app leak. Prefer detached DOM, closures, and named JS objects — those name real code.
  const isSystem = (t, n) =>
    t === 'code' || t === 'object shape' || t === 'synthetic' || t === 'hidden' ||
    (n || '').startsWith('system /') || n === '(object elements)' || n === '(object properties)' ||
    n === '(concatenated string)' || t === 'string' || t === 'number' || t === 'concatenated string';
  const actionable = byCount.filter(r => !isSystem(r.type, r.name) && r.dCount >= cycles * 0.5);

  const ret = buildRetainers(pB);
  const printPath = (type, name, note) => {
    console.log(`\n=== Retainer path: ${type}:${name} ${note} ===`);
    const path = retainerPath(pB, ret, type, name);
    if (!path) { console.log('  (no retainer path to a root found)'); return; }
    path.reverse(); // root first
    path.forEach((step, i) => {
      const via = step.via ? `  <-- .${step.via}` : '';
      console.log(`  ${'  '.repeat(i)}${step.cls.type}:${step.cls.name}${via}`);
    });
  };

  console.log(`\n########## RETAINER PATHS FOR ACTIONABLE SUSPECTS ##########`);
  for (const r of actionable.slice(0, 6)) {
    printPath(r.type, r.name, `(grew +${r.dCount}, ${(r.dCount / cycles).toFixed(1)}/cycle, ${(r.dSize / 1024).toFixed(1)} KB)`);
  }
  if (!actionable.length) console.log('\n(No non-system suspects grew >= half the cycle count.)');

  console.log(`\n########## NEW (LEAKED) INSTANCE FIELD DUMPS ##########`);
  const toDump = actionable.slice(0, 4).map(r => [r.type, r.name]);
  if (!toDump.some(([t, n]) => t === 'object' && n === '$')) toDump.unshift(['object', '$']);
  for (const [t, n] of toDump) {
    console.log(`\n--- new ${t}:${n} (not present in baseline) ---`);
    const baselineIds = idsOfClass(pA, t, n);
    dumpInstances(pB, ret.firstEdge, t, n, 5, baselineIds, ret);
  }
}

main().catch((e) => { console.error(e); cleanup(); process.exit(1); });
