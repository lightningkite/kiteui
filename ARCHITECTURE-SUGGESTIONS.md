# KiteUI Architecture Suggestions

> **Status: suggestions only — nothing here has been implemented.** This document synthesizes
> four subsystem design reviews (reactivity, theming, views/layout/modifiers, navigation/SSR)
> conducted 2026-07-21 on branch `version-8-cleanup`. The reviewers read the code but did not
> run it; every finding carries a confidence marker, and "Confident" means grounded directly in
> cited source, not runtime-verified. Full per-subsystem reviews live in
> `tmp/arch-{reactivity,theming,views,navigation}.md`. Mechanical follow-ups from the
> explicit-API/narrowing migration are tracked separately in `tmp/AGENT-TODO.md`; this document
> is the deeper architectural companion and references that file rather than repeating it.

The core of this codebase is genuinely well-built, and the reviews say so independently: the
RView → Element split "has landed cleanly and is a genuine improvement," the semantic
`ThemePipeline` is "the strongest single piece of design in the module," the URL-first
`PageNavigator`/`Routes` core is "coherent and defensible," and the fine-grained reactive graph
is "coherent... the layering is clean." The suggestions below are about the edges — platform
divergence, half-finished migrations, and a handful of concrete bugs — not the central ideas.

---

## 0. Implementation status (2026-07-22)

Most of this document has now been implemented on branch `api-cleanup` (kiteui) and
`reactive-bugfixes` (reactive), each finding as its own reviewable commit, all verified green
(jvmSsr + Robolectric + JS/iOS compile) and the web target spot-checked live in a browser
(render, navigation, dialogs/popovers, zero console errors).

**Landed — kiteui (`api-cleanup`):** N3 dialog-navigator removal · B10/N5 URL encoding · T2
`themeAndBack` unification + V4 view cleanups · V2 AOSP prune + V1/B2 weight-align capability
interfaces · B3/B4/B5 CSS+Color · TH1 theme-id collision debugger · N2 SSR viewport guardrail +
N1 hydration golden test/counter · R1 thread guard activation (Android/iOS debug) · R4 docs
vocabulary · N7 part-1 deprecation caller-migration.
Plus a bug caught during the work: system-default `Font` singletons (a `Theme.Debugger` false
positive on Android/iOS).

**Landed — reactive (`reactive-bugfixes`):** B1/R4 DependencyTracker · B6 async cache/cancel · B7
CancellationException rethrow · B8 doc fix · R1 thread-confinement guard (consumed via mavenLocal
`6.0.0-prerelease-52-local`).

**Reverted (N6 — SSR quiescence signal):** implemented and verified, then reverted on request. It
replaced SSR's `delay(1)` settle with a `QuiescenceTracker` counting in-flight reactive work, but
review found it *duplicated instrumentation* with the existing `StatusListener` (both are
`CoroutineContext.Element` begin/end trackers of reactive activity, wired at adjacent/identical call
sites — e.g. `load{}` announced to both). They answer subtly different questions (work-in-flight vs
value-readiness; the never-ready case is the one that genuinely differs), so a proper fix would
*extend* `StatusListener` rather than run parallel to it. Since SSR is not yet in production (Q19),
this was deferred rather than kept. If revisited: build the quiescence counter as a `StatusListener`
consumer so reactive activity is announced once, and add explicit tracking only at the genuine gaps
(debounce timers, `bind` syncs). SSR is back on `delay(1)`.

**Deliberately not done (maintainer):** R2, R3 (reserved for co-design), V3, N4.

**Deferred follow-ups:** TH2-iOS interactive-state parity (needs a simulator for visual
verification) · N7 part-2 (delete now-dead deprecated shims: `PageNavigatorBehavior`,
`encodeToStringMap`) · reactive: cut a real release of the `reactive-bugfixes` branch and re-pin
kiteui off the temporary mavenLocal `6.0.0-prerelease-52-local` build before shipping, and fix
`LateInitProperty`'s @Deprecated message (points at a nonexistent `LateInitReactiveValue`) · SSR
preload-mechanism consolidation
(`SsrPreloadable` is public API) · remaining `reactiveScope` uses in some example-app pages · the
`// by Claude` authorship comments · browser-only JS hydration unit tests (couldn't run headless).

---

## 1. Executive summary — cross-cutting themes

Five themes recur across subsystems. Findings that appear independently in two reviews are
marked **corroborated**.

### T1 — Web/native and server/client realizations of the same model diverge silently (corroborated: theming + navigation)
The *model* is single-sourced everywhere; the *realization* is not.
- **Theming:** web precomputes interactive states (hover/focus/down/disabled) as CSS
  pseudo-classes, bypassing the `ThemePipeline` at runtime; Android applies a subset via
  `RippleDrawable`; iOS applies almost nothing (theming F2). Which `Theme` properties each
  platform honors lives only in scattered comments (F9).
- **SSR:** the server renders HTML strings (`FutureElement.commonHtml.jvmSsr.kt`) and the
  client builds DOM (`NativeElement.commonHtml.js.kt`) as two hand-maintained implementations;
  hydration correctness rests on an unstated "structurally identical trees" invariant, and a
  mismatch silently replaces the subtree while reporting success (nav 3.1, §4). Fixed
  1920×1080 SSR viewport breaks any structurally-responsive component on mobile (nav 3.2).

In both cases the failure mode is the same: correct-looking on the platform you tested, wrong
on another, with no contract or test catching the drift. The highest-leverage move in this
document is establishing *shared contracts* (a golden-tree hydration test; a theme-capability
matrix / shared interactive-semantics list).

### T2 — The container `themeAndBack` "do not call super, it breaks everything" workaround (corroborated: views 3.5 + theming F5)
Both reviews independently flagged `NativeContainerElement.kt:524-535`: the container
re-implements the base `themeAndBack` setter body with the comment *"Do not call
super.themeAndBack = value, it breaks everything for some reason."* Two copies of a
lifecycle-critical setter, one an admitted un-understood workaround, in the hottest theming
path. Both reviews suspect the base setter's `if (value == field) return` early-exit interacting
with the cascade. Root-causing this is cheap, and the answer likely reveals (or dissolves) a
latent ordering bug in the theme cascade. This is the single highest-value unknown in the
codebase.

### T3 — Deprecated-but-load-bearing shims and half-finished migrations
- `dialogPageNavigator` lives entirely in `navigation/deprecated.kt` yet is constructed on
  every app boot and every SSR request (`AppNavV2.kt:37-39`, `SsrRouter.kt:127-136`), while its
  replacement (popover/`navClosable`) already exists (nav 3.3).
- `spacingOverrideBeforeNext` is `@Deprecated(level = ERROR)` but its full Signal plumbing
  survives in the iOS/Android engines (views 3.8).
- The reactive vocabulary migration (`Property→Signal`, `shared→remember`) is done in code but
  the docs/CheatSheet/CLAUDE.md still teach the deprecated names, so new code keeps minting
  them (reactivity F9).
- Plus a sweepable inventory: `PageNavigatorBehavior` (vestigial no-op enum),
  `Properties.ext.kt` (95 deprecated lines), `TODO()` hidden companion stubs (nav 3.9).

Each is individually small; together they mean the codebase's deprecation annotations can't be
trusted as a "safe to ignore" signal.

### T4 — Fail-fast ethos violated at the edges by silent failure
The project's stated rule is fail-fast, but several paths swallow errors:
- Android `weight`/`align` cast `parent?.native as SimplifiedLinearLayout` inside
  `catch (ex: Throwable) { ...printStackTrace() }` — a mis-placed modifier silently mis-sizes
  instead of throwing (views 3.1).
- `PageNavigator.navigate/replace/reset` return `Unit` and silently no-op when navigation is
  blocked, while `goBack` returns `Boolean` (nav 3.8; same smell as AGENT-TODO P2's
  `startActivityForResult` silent no-op).
- Hydration mismatches log `console.warn`, replace the subtree, and count as success (nav §4).
- Theme id collisions render the wrong theme with no detection at all (theming F1).
- `reactiveState {}` swallows `CancellationException`, letting cancelled suspend calcs run
  their tail and write stale state (reactivity F4).

### T5 — Unbounded process-lifetime caches and global mutable state
`Theme.themeCache` and the web CSS class registry only grow — dynamically generated themes
(`Theme.random`, per-item accent colors) become permanent stylesheet entries (theming F3, made
worse by F1's id-only identity). `Reactive.reportException`, `Throwable_report`,
`actionInstrumentors` are process-global mutable hooks (reactivity F8). The counter-example
proving the team knows better: `Navigator.js.kt`'s history map is LRU-capped at 50 with a
documented degradation path.

**Related, tracked elsewhere:** concrete platform-type leakage through `override val native:
<ConcreteClass>` is AGENT-TODO **P1**; the views review adds the architectural angle — the
framework's *own* modifiers route through those concrete casts (`modifiers.android.kt:28-110`)
rather than a capability interface, so P1's retype-to-base fix should be paired with
`WeightHost`/`AlignmentHost`-style capability interfaces (views 3.1).

---

## 2. Confirmed bugs — fix independent of any architecture decision

High-confidence, concrete defects worth fixing now. Each is small and self-contained.

| # | Bug | File | Fix direction |
|---|-----|------|---------------|
| B1 | `DependencyTracker.existingDependency` off-by-one: `usedDependencies.add` happens before the probe, so the O(1) ordered fast path checks `dependencies[i+1]` instead of `[i]` and essentially never hits → every rerun degrades to O(n²) linear scans. | `reactive/.../context/DependencyTracker.kt:1506-1513` | Probe `dependencies[usedDependencies.size - 1]`; add a test that asserts the fast path fires. (Same bug is copy-pasted into `DependencyTrackerTest.kt`.) |
| B2 | Android `weight`/`align` modifiers cast `lparams`/`parent?.native` to `SimplifiedLinearLayout*` inside `catch (Throwable) { printStackTrace() }` — wrong-container usage silently mis-sizes the element. | `library/src/androidMain/.../modifiers.android.kt:28-110` | Remove the catch; throw a clear "weight requires a row/col" error (fail-fast), longer-term replace the cast per T4/views 3.1. |
| B3 | CSS: `stroke: var(--nearest-background-color); !important;` — the `!important` follows a stray semicolon and is a no-op fragment. | `KiteUiCss.kt:176` | Move `!important` inside the declaration; add a CSS lint step. |
| B4 | CSS: `.scroll-horizontal * { max-width: 100 }` — unitless, invalid, dead declaration right after a `max-width: unset` on the same selector. | `KiteUiCss.kt:339-341` | Delete or fix the intended unit. |
| B5 | `Color`'s primary constructor defaults `alpha = 0f`, so `Color(red = 1f)` is invisible — inconsistent with `fromHex` (alpha=1) and important enough that the theming skill carries a "CRITICAL" warning about it. | `Paint.kt:122-123` | Default alpha to 1f (audit call sites — behavior-changing) or drop the all-defaulted constructor for named factories. |
| B6 | `TypedReactiveContext.async` keys the cache on the *dependency-value set only* — two different `async` blocks with the same deps return each other's results — and launches on the long-lived scope so stale work is never cancelled on rerun. | `reactive/.../ReactiveContext.kt:833-884` | Fold a per-call-site token into the key; launch on the per-run job (or register a cancel remover), matching what the `Flow` operator already does. |
| B7 | `reactiveState {}` catches `CancellationException` → `notReady`, so a cancelled suspending calc resumes past its suspension point and writes `reportTo.state` / runs `dependencyBlockEnd()` after the fresh run has started. | `reactive/.../core/ReactiveState.kt`; tail at `ReactiveContextSuspending.kt:1313-1317` | Rethrow `CancellationException` in suspending paths, or gate the tail on the run still being current. |
| B8 | KDoc claims "if the calculation has no dependencies... the context is automatically cancelled" — no such logic exists; `dependencyCount` is never read. | `reactive/.../ReactiveContext.kt` (class + `startCalculation` KDoc) | Implement it or delete the claim (doc-correctness bug that misleads leak reasoning). |
| B9 | `informParentOfSizeChange` and `informParentOfSizeChangeDueToChild` have byte-for-byte identical bodies — either redundant or the "DueToChild" variant was meant to differ. | `library/.../layout.kt:8-17` | Confirm intent; delete one or differentiate. |
| B10 | Query-param parsing decodes the value but uses the key raw (`substringBefore('=')`) — an encoded `=`/`&` in a key corrupts parsing. | `navigation/Routes.kt:49-50` | Decode the key symmetrically. Low risk, one-liner. |

---

> **Read §7 alongside the sections below.** The maintainer answered the open questions, and
> several findings here were adjusted as a result — some are deliberate designs (R1, R2, TH1,
> TH2, TH3, TH5, TH6, V3) and one suggestion was reversed (N4/KSP). §7 records each adjustment;
> the original reviewer framing is preserved below for context.

## 3. Reactivity (`~/Projects/reactive` + KiteUI bridge)

**Praised:** clean layering (pure interfaces → impls → context engine → UI bridge); true
fine-grained lazy activation with delayed teardown; loading/error unified as first-class
`ReactiveState`; lifecycle tied to structured concurrency (every `NativeElement` is a
`CoroutineScope`); `ReactiveReentrancyException` fail-fast; the `Processes` → theme bridge.

B1, B6, B7, B8 above originate here. Remaining findings, ranked:

### R1 — Unenforced single-thread assumption × `Dispatchers.Unconfined` default — Sev MED (HIGH under real concurrency) · Effort M · Confidence: high on mechanism
The whole graph mutates unsynchronized `ArrayList`s (`BaseListenable.listeners`,
`DependencyTracker`), the guard (`ReactiveThreadCheck`) is **off by default**, and
`remember`/`shared` default to `Unconfined` (`Remember.kt:71`) — so a `Signal` written from any
background thread runs recalculation *and listener notification* on that thread
(`ReactiveContext.kt:529`, `CalculationContext.kt:281`). Failure mode is silent list
corruption, not an exception.
**Why it matters:** classic works-until-it-doesn't; nothing structurally prevents a network
callback from writing a signal off-main.
**Direction:** enable `ReactiveThreadCheck` automatically in debug builds; reconsider the
`Unconfined` default; document the invariant at the `Signal.value` setter.

### R2 — `DependentAction` lifecycle not tied to the creating element → listener leak — Sev MED · Effort M · Confidence: medium (mechanism traced; no cancel wiring found)
`library/.../reactive/Action.kt`: actions run on `AppScope` and register upstream listeners
against themselves; nothing calls `cancel()` on element shutdown. A Save button whose enabled
state reads a global session signal retains the whole page graph for app lifetime after
navigating away.
**Direction:** first confirm with a navigate-away leak test using the existing
`Element.Debugger.liveInstanceTotal` tooling; then wire `onRemove { action.cancel() }`.

### R3 — Three drifting dependency-tracking engines — Sev LOW-MED (maintenance) · Effort M-L
`TypedReactiveContext` (sync), `ReactiveContextSuspending`, and the suspend `await()/state()`
extensions each re-implement register-and-diff, and have already drifted (B6's `async` cancels
differently from `Flow` differently from the suspending twin). The code self-flags it
(`ReactiveContext.kt:611`). Consolidating on one registration primitive removes the class of
inconsistency.

### R4 — Minor: repeated reads re-add to `usedDependencies` (F7, worsens B1's constant factor, Effort S); global mutable error/instrumentation hooks (F8, acceptable for now); docs still teach the deprecated `Property`/`shared` vocabulary (F9 — a docs sweep, Effort M, prevents new code minting deprecated names).

---

## 4. Theming & styling

**Praised:** semantic derivation ("derive, don't set") is the right primitive and genuinely
differentiating; the ordered float-keyed `ThemePipeline` is a clean, extensible precedence
model; `ThemeAndBack` cleanly encodes the switch-draws-a-card rule; web CSS generation is
diff-based and cached; `revert` correctly models non-cascading themes.

B3, B4, B5 above originate here. Remaining findings, ranked:

### TH1 — `Theme` identity is `id`-only; three caches silently alias colliding ids — Sev HIGH · Effort M · Confidence: confident
`Theme.equals/hashCode` use only `id` (`Theme.kt:1364-1367`); `Theme.themeCache`, the web
`t-{id}` class registry (`KiteUiCss.kt:893-895, 988-990`), and the diff gate all key on it. Ids
are built by string concatenation with several fixed literals (`bold`, `textSize{n}`,
`themeDerivations.kt:102-161`), and `customize(newId=...)` doesn't chain the parent id — two
visually different themes with the same id render as one, invisibly.
**Direction:** content-hash the visual fields for identity (or make equality structural and
keep id as the CSS class name only); at minimum add a debug assertion that a re-registered id
maps to an equal theme.

### TH2 — Two parallel interactive-state models (web CSS pseudo-classes vs native pipeline) — Sev HIGH · Effort L · Confidence: confident
See T1. Web precomputes ~7 sub-rulesets per theme for Hover/Focus/Down/Disabled/Print/Selected
(`KiteUiCss.kt:894-980`), bypassing pipeline steps 0.6/0.8; Android applies hover via ripple
only (`NativeElement.android.kt:448-486`); iOS applies clip+background only. A theme author's
`DownSemantic` override works on web and does nothing on Android.
**Direction:** pick one contract — either drive interactive states through the pipeline
everywhere, or formally declare them presentation-layer and generate native equivalents from
one shared semantic list so no platform silently omits one.

### TH3 — Unbounded theme/CSS growth — Sev MED · Effort M
Every distinct semantic combination permanently adds a `Theme` cache entry and (web) an
injected CSS class; nothing evicts (`KiteUiCss.kt:893`, `Theme.kt:1352`). Dynamic theming
(`Theme.random`, per-item hues) grows the stylesheet for the page's life. **Direction:** LRU
the CSS cache or bound dynamic palettes; first confirm whether real apps generate unbounded ids
(open question Q6).

### TH4 — `refreshTheming` refolds the whole pipeline and reallocates `Theme`s per trigger; cascade is O(subtree) — Sev MED · Effort M
Every processing-state flip (loading/working) re-derives per element with recursive `revert`
copies and id string concat (`Theme.kt:1515-1536`, `NativeElement.kt:578-597`); a top-level
theme swap re-derives the entire tree. The code itself notes "performance actually kinda
matters" (`NativeElement.kt:342`). **Direction:** memoize `(baseThemeId, pipelineSignature) →
ThemeAndBack`; short-circuit the child cascade when the derived child id is unchanged.

### TH5 — The ~22-property `Theme` list is hand-repeated in five places with divergent sentinel rules — Sev MED · Effort M
`withBack`/`withoutBack`/`alter`/`customize`/`copy` (`Theme.kt:219-1537`) each repeat the full
parameter list; `iconOverride`/`separatorOverride` use an `INVALID` sentinel meaning
"keep current" while everything else uses null-means-inherit (F6, F8 —
`LinearGradient.INVALID` doubling as a control value). Adding a theme property is a five-site
edit. **Direction:** a single `ThemePatch` object (nullable = unchanged) applied by one
function.

### TH6 — Lower severity
- **Platform capability gaps are prose, not model** (F9): `blurBackground` ignored on Android,
  squircle web-only, transform units differ. Direction: a capability matrix + a test asserting
  each `Theme` field is consumed or explicitly waived per platform. Effort M.
- **Theme-variant sprawl** (F7): 8 factory files; usage counts say `flat2` 39, `shadCnLike` 20,
  `material3` **0**. Keep one prototype generator + one worked reference, relocate the rest.
  Effort S, gated on confirming no external consumers (open question Q7).
- **Interactive-feedback magic ratios** (F12): Hover/Down/Focus defaults hardcode
  `highlight(0.2f)` etc.; flat2 re-overrides all of them, evidence the defaults don't reuse.
  Direction: theme-level hover/press-delta scalars. Effort M.
- **`KiteUiCss.apply(theme, out)` is an empty public-ish stub** (`KiteUiCss.kt:1287-1289`).

---

## 5. Views / layout / modifiers

**Praised:** the three-layer Element split is coherent and well-documented with one obvious
lifecycle path (`ElementWriter.write()`, `ElementWriter.kt:257`); compile-time modifier
ordering is a legitimately nice zero-runtime-cost use of the type system; the
`outermostElement` delegation mechanism is consistent and documented; `Element.Debugger`'s
GC-independent leak counter is production-useful.

B2 and B9 above originate here. T2 (themeAndBack duplication) is the top finding. Remaining,
ranked:

### V1 — Cast-based modifier coordination + `.native` leakage — Sev MED · Effort M · Confidence: confident
See T4/B2 and AGENT-TODO P1. Beyond the swallowed catch, the architectural issue is that
framework-internal layout coordination routes through concrete-class casts instead of
capability interfaces. **Direction:** `.native` stays as a documented escape hatch typed to
the platform base; add `WeightHost`/`AlignmentHost`-style interfaces the container common code
exposes. Do this *together with* AGENT-TODO P1's retype-to-base, since they touch the same
overrides.

### V2 — Three independently-written layout engines; the Android one is a 1454-line AOSP fork carrying dead weight — Sev MED · Effort M-L · Confidence: confident
Android `SimplifiedLinearLayout.kt` (near-verbatim AOSP `LinearLayout` incl. baseline/
largest-child/RTL machinery, plus `if (false)` at :739, `!false` at :357/:770, and
never-enabled paths), iOS's hand-rolled 301-line engine, and CSS flexbox share no test oracle,
so weight/gap/alignment can drift per platform. **Direction:** prune the fork to the
weight+gap+gravity core KiteUI reaches (likely >50% deletion); add a cross-platform layout
conformance test using the existing SSR/Android/iOS harnesses. Related: the wrapping-flex
algorithm is independently implemented a further two times (iOS `FlexLayout.kt`, Android
`FlexboxLayout`) — the wrap loop could share a platform-agnostic helper taking a
`measure(child): Size` callback (Effort M).

### V3 — Modifier-tier encoding's duplication tax — Sev MED · Effort S (document) / L (rework) · Confidence: confident
The compile-time guarantee costs 8 overloads per writer combinator (`beforeSetup`, `split` —
`ElementWriter.ext.kt:18-39`), exact return-type reproduction across every platform actual, an
inverted-vs-reading-order hierarchy, and an undocumented `CanAddListElementModifier` tier
(`ElementWriter.kt:205`) holding only `asListItem`. **Direction:** short-term, document the
tier rationale and consider generating the per-tier overloads; the open question of a runtime
order-validating builder (Q10) is worth an explicit decision, not a rework by default.

### V4 — Lower severity
- **InteractiveElement action-watch body duplicated 4×** (`InteractiveElement.kt:62-131`);
  collapse via a composed `ActionWatcher`. Effort S.
- **`this as NativeElement` identity invariant** in hot code (`NativeElement.kt:344, 585`) —
  documented but unchecked; add a guardrail comment/assert. Effort S.
- **Half-removed spacing APIs** (`spacingOverrideBeforeNext` ERROR-deprecated with live
  plumbing; `spacingForChildCornerRadii` "will probably be removed" but overridden 3×) —
  decide in-or-out. Effort S. (See T3.)
- **Exception attribution gap:** `Processes.recalculateState` blames the inner element with a
  `// TODO: Get wrapper element somehow` despite `outermostElement` being available
  (`NativeElement.kt:634`). Effort S.
- The iOS `willRemoveSubview` "cursed GC crash" null-guard (`LinearLayout.kt:144-147`) should
  get a tracked issue link.

---

## 6. Navigation / pages / routing / SSR

**Praised:** genuinely URL-first — real `<a href>` anchors, crawlable, address bar
authoritative; `PageNavigator` is ~65 legible lines of pure reactive stack; routing cleanly
separated from rendering; history integration is unusually careful (LRU-capped, documented
degradation); `PageMeta`/`SsrDocument` handle SEO and `</script>` escaping properly;
per-request `SsrContext` isolation with correct shutdown ordering.

B10 above originates here. Remaining, ranked:

### N1 — Two divergent render implementations with no shared hydration contract — Sev MED (was HIGH) · Effort L · Confidence: confident
See T1. Server string-serializer (`FutureElement.commonHtml.jvmSsr.kt:75`) vs client DOM
builder, matched positionally by `hydrateRecursive`
(`NativeElement.commonHtml.js.kt:113-165`); drift is a runtime warning plus a subtree replace.
**Maintainer (Q16, Q19): mismatch is DESIGNED to be recoverable (replace + continue), just
meant to be infrequent; and SSR is not yet used in production or by any consumer outside this
repo.** So this is not a live correctness bug — the recover-on-mismatch behavior is the
intended contract. **Revised direction (do when SSR productionizes, not before):** a golden-output
test that renders representative pages on jvmSsr and in a JS DOM and asserts tree equality (the
existing `HydrationTest.kt` tests only the JS side), plus a dev-mode counter/warning to keep the
mismatch rate visible. Skip the strict-throw mode — recovery is intended.

### N2 — Fixed-viewport SSR breaks structurally-responsive components — Sev MED (was HIGH; SSR not yet production) · Effort M · Confidence: confident (mechanism)
`SsrContext` assumes 1920×1080 (`SsrContext.kt:38-39`); a phone hydrating a desktop-structured
tree (e.g. `rowCollapsingToColumn`, `appNavFactory` variants) mismatches and recovers via
replace (N1). The codebase already solved the analogous problem for one axis
(`Platform.probablyAppleUser` threaded from User-Agent, `SsrRouter.kt:115-116`) — width has no
equivalent. **Maintainer:** SSR isn't production yet (Q19), and mismatch is recoverable by design
(Q16) — so this is a known limitation to address when SSR ships, not a current defect.
**Direction (for that time):** a documented rule that DOM-*structural* responsiveness must be
CSS-only under SSR, or a coarse width hint threaded from User-Agent like the Apple axis already is.

### N3 — Deprecated-but-load-bearing dialog navigator — Sev MED · Effort M · Confidence: confident
See T3. `navigatorViewDialog` is a near-verbatim copy of `navigatorView` differing only in
semantic + `ignoreInteraction` (`NavigatorView.kt:44-70`); the popover replacement is already
what the JS back button integrates with. **Direction:** finish the migration — remove the
dialog `PageNavigator` from `appBase`/`SsrRouter`, or move it out of `deprecated.kt` and stop
calling it deprecated.

### N4 — Hand-rolled source-scanning route generator — Sev LOW-MED · Effort M · Confidence: confident
`gradle-plugin/.../generateRoutes.kt` (328 lines) reimplements a fraction of the Kotlin
grammar with regex comment-stripping and manual brace tracking, visibly scar-tissued with
fixes for its own parser. **Maintainer (Q18): the KSP path was tried and deliberately
abandoned — it was WAY too expensive at compile time; text parsing was a conscious
compile-performance decision. KSP *is* in the toolchain (used elsewhere) but is off-limits
here.** So the standard "just use KSP" answer is rejected. **Revised direction:** keep the
text approach but harden it — extract the ad-hoc scanner into a small tested tokenizer with a
fixture suite so future grammar edge cases are caught by tests rather than production breakage.
Downgraded from MED: it works and the cost tradeoff is intentional.

### N5 — URL encoding asymmetry — Sev MED · Effort S (test) / M (fix) · Confidence: mixed
Path segments are joined raw by `UrlLikePath.render()` (`Routes.kt:60-63`) while parsing
splits on `/` without decoding (`Routes.kt:47-51`, `Navigator.js.kt:205-210`); whether a
path variable containing `/`, space, or non-ASCII round-trips depends entirely on
`UriFormat`'s behavior, with no reversing decode if it does encode. **Direction:** one
round-trip property test (`render(parse(x)) == x` with awkward path vars) settles it (Q11).
Also fragile: the `basePath` prefix-stripping string surgery for sub-path deployments.

### N6 — SSR quiescence is a timing hack; three overlapping preload mechanisms — Sev MED · Effort M · Confidence: confident
`awaitAllResources()` finishes with `delay(1)` "to ensure all reactive bindings have
propagated" (`SsrContext.kt:93-100`); client-side hydration mirrors it with a
`queueMicrotask` whose ordering is "usually" right (`root.kt:171-201`). And `ssrResource`,
`SsrPreloadable`, and the legacy `preload`/`getPreloaded` map are three mechanisms for one
job, alongside an eight-entry-point render matrix in `SsrRouter`. **Direction:** a
deterministic quiescence/idle signal from the reactive runtime (Q13) replacing both hacks;
consolidate onto `ssrResource` and the preload render variants.

### N7 — Lower severity
- **Ambient navigator coupling** (Sev LOW-MED, Effort L): navigators are context addons read
  from four receiver types via a ~10-accessor deprecated forwarder matrix
  (`deprecated.kt:50-97`); collapse to the single `ElementContext` property.
- **Inconsistent nav API** (Sev LOW, Effort S): `navigate` silently no-ops returning `Unit`
  while `goBack` returns `Boolean` (see T4); `goBack`/`dismiss` are near-duplicates; `Link.to`
  eagerly invokes the page factory per assignment and freezes `href`.
- **Deprecated-shim sweep** (Sev LOW, Effort S-M): batch-delete at the next binary-compat
  break (nav 3.9 inventory; overlaps AGENT-TODO P4's `encodeToStringMap`).
- `Page.title` default leaks the class simpleName into SSR `<title>`/SEO; `Routes.parse` can
  throw out of generated lambdas while only `parseOrFallback` guards; `// by Claude`
  authorship comments throughout the SSR files contradict the project's own comment guidance.

---

## 7. Maintainer answers & resulting adjustments

The maintainer answered all 19 open questions. Several flagged findings turned out to be
*deliberate* designs — recorded here so nobody "fixes" them later. Format: **Q → answer →
adjustment to the finding.**

**Reactivity**
1. *Unconfined default (R1)* → **Deliberate — swapping threads has a real cost that is almost
   never necessary.** Adjustment: R1 downgraded — the default is intended. Residual value is
   only the debug-time `ReactiveThreadCheck` to catch *accidental* off-main writes; keep
   `Unconfined`.
2. *DependentAction.cancel leak (R2)* → **Not a bug — `cancel()` exists but is generally unused;
   most people prefer actions to complete even after navigating away.** Adjustment: R2 is
   intended behavior, not a leak. Reclassify as "documented lifecycle choice," remove from
   defect list. (A doc note that actions outlive their element would help newcomers.)
3. *SSR dispatcher fast-path* → **Don't know; SSR was mostly agent-written and isn't in
   production.** Adjustment: leave as-is; low priority (see Q19).
4. *Hashed DependencyTracker parked or rejected (B1)* → **Don't know.** Adjustment: B1 stays the
   safe **one-line off-by-one fix** (`[size-1]`); the hashed variant remains parked, not needed
   to land B1.

**Theming**
5. *Theme id uniqueness (TH1)* → **Deliberate, with a MASSIVE performance advantage. Derivations
   that don't go through semantics are considered bad practice and have been deprecated by word
   of mouth — though the code may need updating to enforce it.** Adjustment: TH1 is **not a
   hazard by design** — reframe from "correctness bug" to "make the semantics-only-derivation
   convention explicit in code (annotation/lint/deprecation) so ids provably can't collide."
   That enforcement is the real todo, not changing the identity model.
6. *Unbounded theme ids in production (TH3)* → **Theoretical; in practice very bounded.**
   Adjustment: TH3 downgraded to LOW/theoretical. No action unless an app starts minting
   per-item themes.
7. *Which variants have consumers (TH6)* → **flat2 is heavily consumed; the others (material3,
   flat, clean) mostly aren't and need additional work — which I'd rather do than throw away.**
   Adjustment: **reverse the deletion suggestion.** Keep the variants; treat them as unfinished,
   not dead. Only the near-empty stubs (F11: empty `apply()`) are cleanup.
8. *Web pseudo-class interactive states (TH2)* → **Very purposeful — it's what makes SSR great**
   (static CSS renders correct interactive states server-side). Adjustment: the web/pipeline
   split is intended; do NOT converge web onto the runtime pipeline. TH2's real residual is
   **iOS parity** (iOS does ~nothing for interactive states) and a single documented semantic
   contract, not unifying the mechanisms.
9. *INVALID-vs-null sentinel (TH5)* → **Needed — `null` is itself a valid settable value** (an
   explicit "no override," distinct from "inherit"). Adjustment: keep the sentinel; drop the
   "collapse to null" suggestion. The only residual is the 5-site hand-repeated property list.

**Views** (shared unknown: **why does `super.themeAndBack` break?** — still open, top priority)
10. *Modifier ladder worth it? `CanAddListElementModifier` permanent? (V3)* → **Compile-time
    enforcement is very deliberate and worth it. CanAddListElementModifier still used?** →
    **Verified: yes, 27 live uses** (it's `weight()`'s return type and `CanAddWeight`'s
    supertype). Adjustment: V3 resolved — the ladder is intentional; action is docs-only
    (document the tiers, incl. why `CanAddListElementModifier` exists). No rework.
11. *AOSP feature set reachable? (V2)* → **No — deletable.** Adjustment: **confirmed cleanup.**
    Strip the unreachable baseline/largest-child/RTL machinery and `if(false)` blocks from the
    Android `SimplifiedLinearLayout` fork.
12. *spacingOverrideBeforeNext replacement* → **No designed replacement yet; would be good to
    put something like it back.** Adjustment: do NOT just rip out the mechanism — it's a *wanted*
    capability pending a design. Track as a feature-design item, not dead code.
13. *iOS childSizeCache staleness* → **Believe so, worth checking.** → **Checked:** invalidation
    hooks exist (`forceRemeasures()`, `subviewDidChangeSizing` clears the changed index). So it
    only goes stale if a theme/font change alters a child's intrinsic size *without* firing
    `subviewDidChangeSizing`. Adjustment: narrow to a **targeted test** — assert a font/theme
    change that changes intrinsic size triggers re-measure. Not an obvious bug.

**Navigation/SSR**
14. *URL segment encoding round-trip (N5)* → **Just go look.** Adjustment: unchanged — a
    one-line round-trip property test (`render(parse(x)) == x` with `/`, space, non-ASCII)
    settles it. Left as a concrete todo.
15. *Dialog PageNavigator delete or permanent (N3)* → **Probably ready for deletion — deprecated
    a long time.** Adjustment: **confirmed — delete the dialog `PageNavigator` path.**
16. *SSR structural-responsiveness contract (N2)* → **Mismatch is meant to be recoverable, just
    infrequent.** Adjustment: N1/N2 downgraded — recover-on-mismatch is the intended contract,
    not a bug; the todo is a rate-monitoring test for when SSR productionizes.
17. *Reactive "no pending updates" quiescence signal (N6)* → **"Huh? That sounds interesting."**
    Adjustment: **elevated as a promising new idea.** A real idle/quiescence signal from the
    reactive runtime would replace the `delay(1)` / `queueMicrotask` timing hacks *and* give SSR
    a deterministic settle point. Worth a prototype (see §8).
18. *KSP available (N4)* → **KSP was tried and deliberately abandoned for route-gen — WAY too
    expensive at compile time; text parsing was a conscious compile-perf choice.** (KSP *is* in
    the toolchain, used elsewhere — verified.) Adjustment: **reversed** — do not move route-gen
    to KSP; harden the text parser with tests instead (see revised N4).
19. *Non-preload SSR entry points used externally* → **No — SSR is unused outside this repo so
    far.** Adjustment: SSR findings (N1, N2, N6) are all future-facing, not current defects.

---

## 8. If you only do five things (revised after maintainer answers)

Reordered given the answers — the SSR/hydration item dropped (SSR isn't production and mismatch
is recoverable by design), the KSP suggestion was reversed, and several "bugs" turned out to be
deliberate.

1. **Root-cause the `themeAndBack` "do not call super" workaround and unify the setter**
   (`NativeContainerElement.kt:524-535`). Unchanged as #1: two reviews independently flagged it,
   it sits in the hottest styling path, and the maintainer doesn't know why `super` breaks
   either. The answer either dissolves a duplication or exposes a real cascade-ordering bug
   hiding behind copy-paste. Everything else in theming perf (TH4) is easier once understood.

2. **Land the confirmed-bugs batch (§2), especially B1, B2, B6/B7.** Small, high-confidence,
   architecture-independent: B1 is a one-line fix (maintainer confirmed the hashed variant isn't
   needed) restoring an O(n)-intended hot path that runs O(n²); B2 turns a silent mis-layout into
   a fail-fast error per the project's own rules; B6/B7 close real suspend/async correctness traps.

3. **Delete the two confirmed-dead paths — a cheap trust-restoring sweep.** The maintainer
   greenlit both: the dialog `PageNavigator` path (Q15 — still constructed on every app boot and
   SSR request, replacement shipped) and the unreachable AOSP feature machinery in
   `SimplifiedLinearLayout` (Q11). Finishing these re-establishes that `@Deprecated`/dead code in
   this codebase is actually safe to remove, making every future sweep cheaper.

4. **Make the theming conventions the code already assumes *enforceable*** (TH1 + TH2-iOS). The
   id-based identity is deliberate and fast (Q5) — so add the guardrail that makes it safe: an
   annotation/lint/deprecation that flags derivations not going through semantics, so ids can't
   collide in the first place. While there, close the one real cross-platform gap the web-CSS
   design exposes: iOS interactive-state parity (Q8).

5. **Prototype the reactive quiescence signal** (Q17 — the idea that intrigued you). A real
   "no pending updates" signal from the reactive runtime replaces the `delay(1)` and
   `queueMicrotask` timing hacks, and hands SSR a deterministic settle point for when it
   productionizes. Small, self-contained, and it retires two acknowledged hacks at once.

Also cheap and greenlit: enable `ReactiveThreadCheck` in debug builds (Q1 — keep `Unconfined`,
just catch accidental off-main writes), and harden the text-based route generator with a fixture
test suite (Q18 — *not* KSP).

Deferred until SSR productionizes (Q19): the golden-tree hydration test + mismatch-rate monitor
(N1/N2), and the width-hint / CSS-only-structural-responsiveness rule.
