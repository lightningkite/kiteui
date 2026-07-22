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

## 7. Open questions for the maintainer

These need domain knowledge or runtime verification the reviewers didn't have.

**Reactivity**
1. Is `Dispatchers.Unconfined` the *deliberate* default for `remember`/`shared`? What
   convention (if any) guarantees background loads hop to main before writing signals? (R1)
2. Does anything ever call `DependentAction.cancel()`? A navigate-away leak test would settle
   R2's severity.
3. SSR dispatcher semantics: does `ssrDispatcher` satisfy the `isDispatchNeeded == false`
   fast paths, or does SSR silently take the dispatched path everywhere?
4. Was the hashed `DependencyTracker` (commented-out harness in `DependencyTrackerTest.kt`)
   benchmarked and rejected, or parked? Decides whether B1 is a one-liner or a swap.

**Theming**
5. Are theme ids actually globally unique in practice today — is there any known collision
   (two `customize("x")` calls; `bold`/`textSize` literals)? (TH1)
6. Do production apps generate unbounded theme ids (per-item colors, `Theme.random`)? Decides
   whether TH3 is live or theoretical.
7. Which theme variants (`material3`, `flat`, `clean`) have external consumers? Confirms the
   TH6 deletion set.
8. Is the web pseudo-class approach a deliberate perf choice to formalize, or an accident to
   converge on the pipeline? (TH2)
9. Is `INVALID`-means-keep for `iconOverride`/`separatorOverride` ever actually needed, or
   can it collapse into null-means-inherit? (TH5)

**Views** (plus the shared #1 unknown: **why does `super.themeAndBack = value` break
everything?** — T2)
10. Was a runtime order-validating builder considered instead of the N-interface modifier
    ladder — is the compile-time guarantee worth the per-platform duplication tax? And is
    `CanAddListElementModifier` a permanent tier or transitional? (V3)
11. Is `SimplifiedLinearLayout`'s AOSP feature set (baseline, largest-child, RTL) reachable
    from any KiteUI API, or deletable? (V2)
12. What is the intended replacement for `spacingOverrideBeforeNext`, deprecated to ERROR
    with its mechanism still live underneath?
13. Does the iOS `childSizeCache` go stale on theme/font changes that alter intrinsic size
    without changing the measure input?

**Navigation/SSR**
14. Does `UriFormat.encodeToString` percent-encode path segments, and where is the decode?
    One round-trip test answers N5.
15. Is the dialog `PageNavigator` slated for deletion or permanent? Current state
    (deprecated annotation + active core wiring) is contradictory. (N3)
16. Is structural responsiveness meant to be SSR-safe at all, or is "server renders desktop,
    client reconciles" the accepted contract with a target mismatch rate? (N2)
17. Can the reactive runtime expose a "no pending updates" signal to replace `delay(1)` /
    `queueMicrotask`? (N6, ties to reactivity)
18. Is KSP already in the toolchain? Changes N4's cost materially.
19. Are the non-preload SSR entry points (`render`/`renderPage`) still used by any consumer?

---

## 8. If you only do five things

1. **Root-cause the `themeAndBack` "do not call super" workaround and unify the setter**
   (`NativeContainerElement.kt:524-535`). Cheapest item on this list relative to its value:
   two reviews independently flagged it, it sits in the hottest styling path, and the answer
   either dissolves a duplication or exposes a real cascade-ordering bug that currently hides
   behind a copy-paste. Everything else in theming performance (TH4) is easier to touch once
   this path is understood.

2. **Land the confirmed-bugs batch (§2), especially B1, B2, B6/B7.** These are small,
   high-confidence, architecture-independent fixes: B1 restores an O(n)-intended hot path
   that currently runs O(n²) on every recalculation; B2 turns a silent mis-layout into a
   fail-fast error, per the project's own rules; B6/B7 close real correctness traps in the
   suspend/async reactive paths before more code depends on them.

3. **Put a contract under hydration: golden-tree server/client test + strict mismatch mode**
   (N1, first slice of N2). SSR is the subsystem where silent divergence is both most likely
   (two hand-written renderers in different languages) and most invisible (mismatch = warn +
   replace + report success). A representative-page tree-equality test plus a dev mode that
   throws on mismatch converts every future drift from a shipped FOUC into a red CI run —
   without committing yet to the larger shared-serialization refactor.

4. **Enforce the reactive single-thread invariant in debug builds and decide the `Unconfined`
   default** (R1). The data structures are unsynchronized by design; today the invariant is
   held by convention only, and the failure mode is silent corruption. Turning on
   `ReactiveThreadCheck` in debug is nearly free and converts the whole class of bug into an
   immediate, attributable exception. Answer Q1 while you're there.

5. **Finish one flagship deprecation: delete the dialog `PageNavigator` path** (N3). Of all
   the T3 half-migrations, this is the one still constructed on every app boot and SSR
   request with its replacement already shipped. Finishing it removes a parallel navigator
   system, a near-duplicate `navigatorView`, and — more importantly — re-establishes that
   `@Deprecated` in this codebase means "safe to ignore," which makes every future sweep
   (nav 3.9, views 3.8, AGENT-TODO P4) cheaper.

Honorable mentions that just missed the cut: theme identity (TH1 — do it before any app
ships dynamic per-item theming), the interactive-state contract (TH2 — biggest cross-platform
correctness gap but genuinely large), and the KSP route generator (N4 — high leverage,
check Q18 first).
