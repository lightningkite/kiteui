# version-8 Merge Review

Change review for the architecture-cleanup work on `version-8`.
Range: **`0546446d` (base) → `be5d3fb8` (HEAD)** — 19 commits from this work + 1 of your own.
Companion changes in the separate **`reactive`** repo are listed at the end (they are **dormant** until republished — see §5).

---

## 0. How to review this efficiently

**Verification legend** (used in every entry):
| Mark | Meaning |
|---|---|
| ✅ | Has automated tests that pass |
| 🔧 | Compile-verified (jvmSsr and/or android/js/ios) but no dedicated test |
| 🌐 | Exercised in a real browser (Chrome) |
| 📖 | Docs / no executable code |
| ⚠️ | **Not verified** — needs your eyes / manual test |

**Global verification that applies to the whole branch:** at HEAD, `:library:jvmSsrTest` is green (500 tests), and `:library` + `:example-app` compile on **jvmSsr, Android, JS, and iOS**. So every entry is at least 🔧 unless noted otherwise.

**Where to spend your review time (highest risk first):**
1. `02aa06ba` — removes `StatusListener` from `Element` (core type change). *Highest structural blast radius.*
2. `be5d3fb8` — JS browser-history rewrite. *Behavioral, browser-only-verifiable.*
3. `f7b27a59` — list-rendering unification (623 LOC). *Touches a heavily-used primitive.*
4. `e9dfee9e` — route codegen now **fails the build** on malformed/duplicate routes. *Can break a consumer build.*
5. `21d88193` — deletes 12,180 lines. *Large, but verified dead.*
Everything else is small/localized.

**At-a-glance table:**

| Commit | Size (files, ±) | Area | Risk | Verified |
|---|---|---|---|---|
| `7b80b80f` | 1, +224 | roadmap doc | none | 📖 |
| `3ff5cec5` | 2, +188 | build/plugin | Low | 🔧 |
| `205024c0` | 2, +54/-1 | reactive/Action | Low | ✅ |
| `fa8e37ed` | 3, +14/-110 | views core | **Med** | 🔧✅(suite) |
| `4a2eba16` | 3, +2/-19 | views l2 | Low* | 🔧 |
| `c00bbcf0` | 1, +3/-1 | android nav | Low | 🔧 |
| `3efc6e21` | 1, +16/-5 | js nav | Low | 🔧🌐 |
| `ad3e0507` | 5, +50/-43 | docs | none | 📖 |
| `d085fe17` | 5, -275 | file renames | Low | 🔧 |
| `e9dfee9e` | 3, +296/-29 | build codegen | **Med** | ✅ |
| `e676647f` | 1, +407 | tests | none | ✅ |
| `fce55b2e` | 2, +130/-2 | **(yours)** | — | ⚠️ |
| `21d88193` | 99, +4/-12180 | delete dead code | **Med** | 🔧 |
| `9c7fa9c8` | 2, +347 | test harness | Low | ✅ |
| `02aa06ba` | 3, +21/-5 | **Element core** | **High** | 🔧✅(suite) |
| `8119b358` | 3, +37/-2 | dialog/back | Med | 🔧🌐 |
| `f7b27a59` | 9, +623/-77 | list rendering | **Med** | ✅🌐 |
| `d43eb91c` | 1, +1/-1 | example route | Low | 🔧🌐 |
| `03e77717` | 11, +1563 | tests | none | ✅ |
| `be5d3fb8` | 1, +135/-291 | **js history** | **High** | 🔧🌐(partial) |

\* Low risk but contains a **source-compatibility break** (see §4).

---

## 1. Bug fixes (behavioral)

### `205024c0` — DependentAction never cleared error on dependency change ✅
- **What:** `startAction` launched the coroutine without putting the `DependencyChangeListener` (the action itself) in the coroutine context, so `await()`/`state()` never registered dependencies → `onDependencyChange` never fired. Added `+ this` to the launch context. This is the *default* Action type, so it affected every button that should clear its error when inputs change.
- **Risk:** Low. One-line semantic fix; behavior now matches documentation.
- **Behavior change to be aware of:** buttons whose error previously "stuck" will now clear on dependency change (intended).
- **Verified:** ✅ Added `DependentActionTest` (passes on jvmSsr).

### `fa8e37ed` — view-core correctness/leak fixes 🔧 + suite
- **What (3 things in one commit — review each):**
  1. `working`/`loading` extension props were cached per-element into the *subtree-shared* `ElementContext.addons` under a constant key → siblings read each other's process status. Now uncached (the wrapper is trivial).
  2. `labelFor`/`describedBy` (strong cross-element refs) now cleared in `onShutdown` — prevents a dead element retaining a whole subtree.
  3. `checkIsShutdown` `println` → `Log.warn` with view path (kept the no-op-return; a teardown race can legitimately reach it).
- **Risk:** **Med** — touches `NativeElement` hot-path lifecycle. The `working`/`loading` change alters identity semantics (returns a fresh wrapper per access; reactivity unaffected). No dedicated unit test for the sibling-cache fix — reasoned + covered by the full suite.
- **Note:** the deletion of the stale `Navigator.kt-review.txt` artifact landed in this commit (its message doesn't mention it).
- **Verified:** 🔧 jvmSsr compile + full `:library:jvmSsrTest` green.

### `4a2eba16` — small l2 defects 🔧
- **What:** ColorPicker compared `.green` twice and never `.alpha` (alpha-only changes dropped) → fixed. Graph printed debug lines on *every* canvas draw → removed. Removed two **empty no-op public functions** `navLayout`/`navSideBar` (kept the functional `navBottomBar`).
- **Risk:** Low, **but** the `navLayout`/`navSideBar` removal is a **source-compat break** (§4) — zero in-repo users, but external callers won't compile.
- **Verified:** 🔧 jvmSsr compile.

### `c00bbcf0` — Android deep-link query decoding 🔧
- **What:** `onNewIntent` decoded the whole `key=value` before splitting on `=`, so an encoded `=` (`%3D`) in a value corrupted the split. Now splits on the raw delimiter first, then decodes each half (matches JS `urlLike()`).
- **Risk:** Low. Correctness fix, Android only.
- **Verified:** 🔧 Android compile. No unit test (Android intent path); **⚠️ worth a manual deep-link check if you use encoded `=` in query values.**

### `3efc6e21` — bound the default web history-stack cache 🔧🌐
- **What:** `PageNavigatorBehavior.Separate` (the default) kept an unbounded `HashMap<UrlLikePath, List<Page>>`, strongly retaining every Page of every stack ever visited. Now a 50-entry LRU with oldest-first eviction; beyond the cap an old URL reparses into a fresh single-page stack.
- **Risk:** Low. Behavior change only for back-navigation to URLs visited >50 entries ago (acceptable degradation).
- **Verified:** 🔧 JS compile; 🌐 normal navigation exercised in browser (one history entry per navigate confirmed). *Note: this file is fully rewritten later by `be5d3fb8`.*

---

## 2. Structural / core changes

### `3ff5cec5` — rescue `aiDriverTasks.kt`; wire into plugin 🔧
- **What:** The file existed **only in the gitignored buildSrc mirror** (one `git clean` from permanent loss). Moved into `gradle-plugin/src/main/kotlin/` (the mirror's source of truth) and wired `registerAiDriverTasks(project)` into `KiteUiPlugin.apply` (its own doc already named that call site).
- **Risk:** Low. Registers 3 lazy Gradle tasks for consumers (no-op without an `:ai-driver-server` project).
- **Verified:** 🔧 `:gradle-plugin:compileKotlin`.

### `d085fe17` — rename leftover `RView`/`RContext`-prefixed files 🔧
- **What:** Content-neutral renames (Kotlin filename ≠ class name): `RContext.{ios,android,commonHtml}.kt` → `ElementContext.*`; `RView.commonHtml.jvm.kt` → `FutureElement.commonHtml.jvmSsr.kt`. Deleted `RViewTest.kt` (100% commented-out dead code).
- **Risk:** Low. No code content changed → cannot affect compilation or consumers.
- **Verified:** 🔧 all platforms compile.

### `21d88193` — delete dead code (swing modules + orphans) 🔧
- **What:** Removed the 4 Swing modules (`library-swing` etc., 80+ files that still referenced the deleted `RView` and could not compile), orphaned `example-app/src/wasmJsMain` (no wasmJs target), unwired `test-utilities/src/jvmDesktopMain`, a stray misplaced `commonJvmMain/Example.kt`, and the commented includes in `settings.gradle.kts`. **-12,180 lines.**
- **Risk:** **Med** by size, but everything deleted was already unbuilt/uncompilable. Recoverable from git history.
- **What to check:** that you truly don't want the Swing code revived (it's on branch history if so).
- **Verified:** 🔧 `:library` + `:example-app` jvmSsr compile green after removal.

### `02aa06ba` — remove `StatusListener` from `Element`'s supertypes 🔧 + suite  ⟵ **scrutinize most**
- **What:** `Element` extended both `KiteUiCoroutineScopeHelpers` (→ `CoroutineScope`) and `StatusListener` (→ `CoroutineContext.Element`), so every element was simultaneously a scope **and** an entry in its own coroutine context → `element.job` ambiguity, mis-parented child jobs (the long-standing `Element.kt` TODO). Now `Element` is *only* a `CoroutineScope`; `NativeElement` puts a **separate** `StatusListener` object into its context that delegates to the element's own `watch{Background,Foreground}Process`.
- **Risk:** **High** structurally, but the actual diff is small and contained. I verified **no code consumes `StatusListener` via the `Element` type** — all consumers read it from the coroutine context (`coroutineContext[StatusListener]`), which still works.
- **Bonus bug fixed:** surfaced that `TelemetryContext.viewPath()` had been silently binding to the wrong extension (because `Element` used to be a `CoroutineContext`) and **always returning empty**; now correct.
- **Breaking:** `Element` is no longer a `StatusListener` (type change) — breaks only external code that treated an element as a `StatusListener` (none found in-repo).
- **Verified:** 🔧 full `:library:jvmSsrTest` green + android/js/ios compile. *Note: I first shipped a weaker "wrapper" version of this fix, then reverted it and did this correct version — the wrapper commit is **not** in this history (removed before push).*

---

## 3. Features

### `8119b358` — dialog back-to-dismiss: core + Android 🔧🌐
- **What:** App-global stack of dismiss lambdas on `ElementContext` (shared via the root context). The new `dialog()` registers when `dismissable=true`; the Android back handler dismisses the top dialog before `mainNavigator.goBack()`. iOS unchanged (modal `UIViewController`, no hardware back).
- **Risk:** Med. New back-handling path. The deprecated `dialogPageNavigator` dialogs correctly **do not** participate.
- **Verified:** 🔧 jvmSsr + Android compile. 🌐 **Browser-verified end-to-end** (with the JS half in `be5d3fb8`): opened the new `dialog {}`, pressed browser Back → dialog dismissed, page stayed; next Back left the page. Works with exactly-one-back semantics.

### `f7b27a59` — unify list rendering into `renderList` ✅🌐
- **What:** One `renderList` family (keyed with ID-diffing + `animate`; unkeyed with positional slot reuse, `placeholders`, and a new `poolCap` that **bounds the previously-unbounded hidden-view pool**). Recycler2 gets `renderList`/`renderListMultipleTypes` aliases (virtualization stays separate). The six old functions (`forEach`, `forEachUpdating`, `forEachById`, `forEachByIdWithoutAnimation`, `forEachAnimated`, `childrenLazyLoading`) + `Recycler2.children` become `@Deprecated(ReplaceWith(...))` delegating to `renderList` — behavior identical, all were `@InternalKiteUi`.
- **Risk:** **Med** — heavily-used primitive. Mitigations: old funcs kept as delegating deprecations; trivial in-repo callers migrated. **`forEachAnimated` callers with custom `preHidingModifiers` were left on the deprecated function** (can't mechanically migrate — the render lambda type changed `T`→`Reactive<T>`); review those call sites in `example-app` (DragPage, AnimationTestPage) if you rely on them.
- **Verified:** ✅ `RenderListTest` (8 tests incl. poolCap eviction) + full suite + all platforms. 🌐 example-app pages using it render (ForEachById test page).

### `d43eb91c` — fix duplicate route surfaced by the codegen hardening 🔧🌐
- **What:** `Recycler2PullToRefreshTest` and `Recycler2TestPage` both claimed `@Routable("recycler2-test")`, so one page was **silently unreachable**. The new fail-fast duplicate detection (from `e9dfee9e`) caught it; gave the pull-to-refresh page its own route.
- **Breaking:** the pull-to-refresh page's URL changed to `recycler2-pull-to-refresh-test`.
- **Verified:** 🔧 example-app compile + `generateAutoRoutes` passes. 🌐 confirmed the page is now reachable in the browser.

### `be5d3fb8` — simplify JS history; reset clears history; back dismisses dialogs 🔧🌐(partial)  ⟵ **browser-behavioral**
- **What:** Collapsed the three `PageNavigatorBehavior` strategies to just `Separate` (each `navigate()` = one browser entry). `Link`/`Deprecated` kept as `@Deprecated` no-op aliases + `PageNavigatorUseExperimentalBehavior` inert (source-compatible). Removed the dead Link-mode code and cleans up legacy `main-stack*`/`last-stack-id` localStorage keys on startup. `reset()` now clears browser forward/back via `history.go(-N)` + `replaceState`. Adds the browser side of the dialog-dismiss `popstate` guard.
- **Risk:** **High** — browser-behavioral, cannot be fully verified by tests.
- **Verified in browser (🌐):** ✅ normal navigation = one history entry each; ✅ back dismisses an open `dialog {}` then leaves the page on the next back (clean, exactly-one-back).
- **⚠️ NOT yet verified — please manually check before/after merge:**
  - `reset()` clears history (couldn't find a clean reset trigger in the example app).
  - Refresh (F5) lands on the same page; deep-link entry renders.
  - `replace()` does not add a history entry.
  - Legacy localStorage keys are gone after first load (DevTools → Application).
  - **Known caveat in the code:** reset is detected as *stack shrinks to 1*, which can't distinguish a deliberate `reset()` from a user manually pressing Back to root (both clear forward history — acceptable, but noted); and `history.length` is browser-capped (~50), so the clear is best-effort.

---

## 4. Documentation & tests (low-risk, but included)

- **`7b80b80f`** 📖 — the architecture review + prioritized roadmap (`plans/architecture-review-2026-07.md`). No code.
- **`ad3e0507`** 📖 — docs sweep: replaced dead `-`-operator modifier syntax with dot-chaining, fixed stale MIGRATION.md status, Kotlin badge, `RContext`→`ElementContext`, `jvmRun`→`ssrServerRun`. Verified claims against the code. *(Separately noted: `CLAUDE.md` still says `viteRun` — the real dev task is `jsViteDev`; not yet fixed.)*
- **`e9dfee9e`** ✅ — **route codegen hardening** (`generateRoutes.kt`): bounded `@QueryParameter` scan to the class body, word-boundary `val`/`var` match, **fail-fast** on malformed input, deterministic specificity-first parser ordering, and **throw on duplicate route templates**. 5 golden tests. **This is a build-behavior break** (§4) and already caught a real bug (`d43eb91c`).
- **`e676647f`** ✅ — 24 theming-invariant unit tests (the theming core previously had zero).
- **`9c7fa9c8`** ✅ — `elementTree { }` jvmSsr element-level test harness + 6 theme-cascade tests.
- **`03e77717`** ✅ — per-element test suite Phase 1: 67 tests across 9 interactive elements (Button, TextInput, Checkbox, Switch, ToggleButton, Select, Slider, TextArea, NumberInput) via the driver-backed `uiTest {}`; plus `assertEnabled`/`assertDisabled`/`findByDebugName` helpers. (Two test-authoring bugs found and fixed while verifying — the 500ms Action frequency cap swallows a rapid second click; a slider must set `max` before `min`.)

### `fce55b2e` — "Build companion experiments for KBuild" ⚠️ **(your own commit)**
- Authored by you (2026-07-08), sits between the two work rounds. Touches `modifiers.commonHtml.kt` (10 lines) and adds `ReactiveButtonDriverTest.kt` (122 lines). **Not part of this review work — not analyzed by me.** Flagging so it isn't mistaken for agent output.

---

## 4a. Breaking-change summary (for the merge)

**Hard API/source breaks:**
- **`navLayout` / `navSideBar` removed** (`4a2eba16`) — were empty no-ops; external callers won't compile.
- **`Element` no longer implements `StatusListener`** (`02aa06ba`) — breaks external code using an element *as* a `StatusListener` (none in-repo).
- **Route URL change**: `recycler2-test` → `recycler2-pull-to-refresh-test` for one example page (`d43eb91c`).

**Build-behavior breaks (can fail a consumer build — by design, they surface latent bugs):**
- **Route codegen now throws** on malformed `@Routable` or duplicate/ambiguous route templates, and route match order is now deterministic (`e9dfee9e`). Only bites a project that already had a malformed/ambiguous route.

**Deprecations (not breaking yet — warnings; removable next minor):**
- Six `forEach*` variants + `Recycler2.children`/`childrenMultipleTypes` (`f7b27a59`).
- `PageNavigatorBehavior.Link/Deprecated` + `PageNavigatorUseExperimentalBehavior` now inert (`be5d3fb8`).

**Runtime behavior changes (not API breaks):**
- `DependentAction` clears error on dependency change (`205024c0`); `working`/`loading` return fresh instances (`fa8e37ed`); default web history bounded to 50 (`3efc6e21`); `reset()` clears browser history, back dismisses dialogs (`be5d3fb8`).

---

## 5. Companion `reactive` repo (SEPARATE repo, currently DORMANT)

kiteui pins `reactive = "6.0.0-prerelease-45"` (published). These commits are on `reactive`'s `version-6` branch and **do not affect kiteui until you bump/republish that dependency.** I deliberately did not republish.

| Commit | What | Risk | Verified |
|---|---|---|---|
| `20a14a3` | Fix `reactive(onLoad)` unreachable callback (inverted condition) — placeholders never rendered | Low | ✅ full `jvmTest` |
| `b90d368` | *(superseded — see revert below)* Remember dead-read leak fix | — | reverted |
| `b0a18d8` | Detect calculation **reentrancy** → throw clear error instead of stack overflow | Low | ✅ |
| `c0d8e2e` | **Revert** `b90d368` — it regressed `sharedTest3` | — | ✅ |
| `0b88b92` | **Opt-in** thread-confinement assertion (off by default; no behavior change unless enabled) | Low | ✅ |
| `eb498bf` | Correct Remember dead-read leak fix (`skipDependencyRegistration` makes a dead read a zero-side-effect snapshot) | Med | ✅ 108 tests incl. `sharedTest3` + new leak test, independently re-verified |

**Reentrancy fix (`b0a18d8`) is the only one with a behavior change consumers could notice** — code that previously stack-overflowed on reentrancy now throws `ReactiveReentrancyException` (nothing working relied on overflowing).

---

## 6. Residual risks & follow-ups (nothing blocking, for your awareness)

- **`be5d3fb8` browser behaviors** (reset-clears-history, refresh, deep-link, replace, localStorage cleanup) — the ⚠️ items above; recommend a manual pass. The dev server task is `./gradlew :example-app:jsViteDev` (not the stale `viteRun`).
- **`f7b27a59`** — the `forEachAnimated`-with-`preHidingModifiers` call sites left deprecated; migrate deliberately.
- **Deferred (not in this branch):** the small `Theme.applyToSemantic` helper (concluded the larger "ThemeOverlay" abstraction wasn't worth it); per-element tests Phases 2–3 (layout/screenshots on real platforms) and the remaining ~9 elements.
- **Reactive republish** — decide when to bump kiteui off the prerelease pin to pick up §5.
</content>
