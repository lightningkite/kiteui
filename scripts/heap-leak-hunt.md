# `heap-leak-hunt.mjs` — scripted heap-leak hunting for the JS app

A zero-dependency Node script that drives a **headless Chrome** through a scripted navigation
flow, takes two garbage-collected **heap snapshots** (before/after), and tells you exactly which
object classes grew, what is retaining them, and what the leaked instances look like.

It exists because heap-*byte* sampling (`performance.memory`) is far too noisy to see a small
per-navigation leak, and because manual DevTools snapshotting isn't repeatable. This turns
"is this page leaking?" into a one-command, CI-capable answer.

> **Complements the in-app counter.** `Element.Debugger.countInstances` (exposed in the browser as
> `kiteuiLeakEnable()` / `kiteuiLeak()`, see `library/.../leakDebug.js.kt`) is a fast, GC-independent
> check for *un-shut-down elements*. This script is the heavier tool for finding memory that is
> retained *despite* proper shutdown (dangling references, caches, observers).

---

## Prerequisites

- The app dev server running (default `http://localhost:8000/`): `./gradlew :example-app:jsViteDev`.
- Google Chrome installed (override the path with the `CHROME` env var).
- Node ≥ 22 (uses the global `WebSocket` — no `npm install` needed).

## Quick start

```bash
# default driver: ping-pong between the "Home" and "Documentation" nav links
node --max-old-space-size=8192 scripts/heap-leak-hunt.mjs

# a saved driver describing a specific page flow
node --max-old-space-size=8192 scripts/heap-leak-hunt.mjs scripts/drivers/home-docs.json

# tune the run
WARMUP=40 CYCLES=40 KEEP=1 node --max-old-space-size=8192 scripts/heap-leak-hunt.mjs
```

`--max-old-space-size=8192` is recommended: heap snapshots of a real app are large JSON documents.

---

## How it works

1. **Launch** headless Chrome with `--remote-debugging-port` (+ `--no-sandbox`, `--js-flags=--expose-gc`,
   and anti-throttling flags) against a throwaway profile dir.
2. **Connect** to the Chrome DevTools Protocol over a raw WebSocket and attach to a fresh tab.
3. **Neutralize the debugger's own retention** (this is the subtle part — see the warning below):
   `Runtime.setAsyncCallStackDepth(0)` and `Runtime.discardConsoleEntries` /
   `releaseObjectGroup('console')` before each snapshot.
4. **Navigate** to the app and install a tiny in-page step interpreter (`__kdriverRun`).
5. **Warm up**: replay the driver's `warmup` cycle `WARMUP` times so V8's one-time JIT compilation
   and hidden-class creation happen *before* the baseline (otherwise warmup masquerades as a leak).
6. **Baseline snapshot**: force GC (twice), then `HeapProfiler.takeHeapSnapshot`.
7. **Measure**: replay the driver's `cycle` `CYCLES` times.
8. **After snapshot**: force GC, snapshot again.
9. **Diff & report** (see "Reading the output").

Because the snapshots are taken *after a forced GC*, anything still present is genuinely reachable —
i.e. a real leak, not garbage the collector merely hadn't swept yet.

### ⚠️ The debugger-artifact trap (why step 3 matters)

An attached debugger changes what stays alive. With async stack traces enabled, V8 retains the
stacks of pending/cancelled async work — in a Kotlin coroutine UI that means every element
shutdown's `CancellationException` (and, transitively, the just-detached element subtree) is
pinned for the lifetime of the inspector session. The DevTools console likewise retains objects it
has referenced. Measured naively, this looks like a large per-navigation leak that **does not exist
for a normal user with no debugger attached.** Step 3 disables both so the script measures real,
production-representative retention. If you ever adapt this code, keep those calls.

---

## Driver instructions

A **driver** describes one navigation *cycle* as an ordered list of steps. Navigation happens
*inside* the SPA (no full page reload), so the JS heap persists across cycles and a per-cycle leak
accumulates into a measurable delta.

Pass a driver as the first CLI arg or via `DRIVER=<path>`. It may be a `.json` file or a `.mjs`/`.js`
module (default-exporting the same shape). Its shape is either a bare array of steps (= the cycle),
or an object:

```jsonc
{
  "url": "http://localhost:8000/docs", // page to open (optional; else APP_URL)
  "warmup": [ ...steps ],              // cycle run before the baseline (optional; defaults to cycle)
  "cycle":  [ ...steps ],              // the measured cycle (required)
  "settleMs": 500                      // wait before each snapshot (optional)
}
```

### Step types

| Step | Effect |
|------|--------|
| `{ "click": "Documentation" }` | Click the `<a>`/`<button>`/`[role=button\|link]` whose trimmed text **or** `aria-label` equals the value. |
| `{ "clickSelector": "button.close" }` | Click the first element matching a CSS selector. |
| `{ "goto": "/docs?query=x" }` | SPA-navigate via `history.pushState` + a `popstate` event (KiteUI's URL router reacts to it) — no reload, heap preserved. |
| `{ "type": { "selector": "input", "text": "layout" } }` | Set an input's value (through the native setter) and dispatch `input`. Use `"text": ""` to clear. |
| `{ "waitFor": ".result", "timeout": 5000 }` | Poll until a selector appears (default 5 s timeout). |
| `{ "wait": 80 }` | Sleep N milliseconds (let async loads / animations settle). |

### Examples

Ping-pong between two pages (`scripts/drivers/home-docs.json`):

```json
{ "url": "http://localhost:8000/",
  "cycle": [ { "click": "Documentation" }, { "wait": 80 }, { "click": "Home" }, { "wait": 80 } ] }
```

Stress a single page's search field (`scripts/drivers/doc-search-typing.json`):

```json
{ "url": "http://localhost:8000/docs",
  "warmup": [ { "goto": "/docs" }, { "waitFor": "input" } ],
  "cycle":  [ { "type": { "selector": "input", "text": "layout" } }, { "wait": 150 },
              { "type": { "selector": "input", "text": "" } }, { "wait": 150 } ] }
```

To test **any** flow, write a new JSON in `scripts/drivers/` describing the click/type/goto steps
that make up one repeatable round-trip, then point the script at it. Keep the cycle *balanced*
(end where it began) so the only growth is a genuine leak.

---

## Reading the output

- **Heap growth over N cycles** — total retained-size delta after forced GC. Divide by `CYCLES` for
  a per-cycle figure. A few KB/cycle that *shrinks as you raise `WARMUP`* is JIT warmup, not a leak.
- **Top classes by RETAINED SIZE / COUNT growth** — what accumulated. A class whose count grows
  ≈ proportionally to `CYCLES` is the leak signature. `code:system / *`, `object shape`, and
  `system / *` entries are V8 infrastructure (usually warmup); real app leaks show up as
  `native:<tag …>` (detached DOM), `closure`, or named `object:*`.
- **Retainer paths for actionable suspects** — the chain of references from a GC root down to the
  leaked object. This *names the code* holding it. Read it root-first; the last hop is the leak.
- **New (leaked) instance field dumps** — the fields (and string values) of instances that are
  present in the *after* snapshot but not the baseline (matched by stable node id), each with its
  own retainer path. This is what lets you identify the exact object.

If a retainer path ends at `(Global handles) / DevTools console`, you are looking at the debugger
artifact described above — not a real leak.

## Environment variables

| Var | Default | Meaning |
|-----|---------|---------|
| `APP_URL` | `http://localhost:8000/` | Page to open (a driver's `url` wins). |
| `DRIVER` | – | Driver file path (also accepted as the first CLI arg). |
| `CYCLES` | `30` | Measured cycles between the two snapshots. |
| `WARMUP` | `20` | Cycles before the baseline (raise it to rule out JIT warmup). |
| `SETTLE_MS` | `500` | Settle time before each snapshot. |
| `NAV_A` / `NAV_B` | `Documentation` / `Home` | Link texts for the built-in default driver. |
| `PORT` | `9222` | Chrome remote-debugging port. |
| `KEEP` | `0` | If `1`, also write `tmp/heap-{baseline,after}.heapsnapshot` (loadable in DevTools → Memory → Load). |
| `CHROME` | macOS Chrome path | Chrome/Chromium binary. |

## Limitations

- Drives the SPA by clicking links / firing `popstate`; a full-reload `goto` would reset the heap and
  hide leaks, so it isn't offered.
- The class-diff and retainer BFS load whole snapshots into memory — hence `--max-old-space-size`.
- Retainer BFS reports one shortest path; a leaked object may be held by several references.
- macOS Chrome path is the default; set `CHROME` on other platforms.
