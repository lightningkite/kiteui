# KiteUI Architecture Review & Roadmap — July 2026

Produced by a multi-agent review of the version-8 branch covering: core view architecture,
reactivity (including the external `reactive` library), theming/models, navigation/l2
components, and platforms/build/testing. File:line references were verified at review time.

---

## Overall Judgment

KiteUI's core ideas are genuinely strong — several are better than what mainstream
frameworks offer:

- **Type-enforced modifier ordering** (`ElementWriter.kt:159-227`): invalid modifier orders
  are unrepresentable at compile time, with a deliberate `@UnsafeModifier` escape hatch.
  Neither Compose nor SwiftUI does this.
- **Semantic theming with `ThemeAndBack`** (`Theme.kt:22-56`): the card/background rules from
  ThemeRules.md are encoded in the type system, with id-keyed caching enabling cheap CSS
  dedupe and cascade short-circuits.
- **Reactive state as `@JvmInline` value class** (`ReactiveState.kt`): loading/error as
  first-class states that propagate automatically through the graph — better integrated with
  async than Solid.js itself.
- **The commonHtml source-set layer**: ~7k lines shared between JS and SSR, with thin
  platform leaves. Exactly how SSR should be layered.
- **Minimal navigation core**: `PageNavigator` is ~60 lines over a `Signal<List<Page>>`.

The risk is concentrated at the **edges**: shipped-but-dead code paths in high-traffic
reactive APIs, regex-based route codegen, browser history sync, Recycler2's flag-driven
layout, a buildSrc mirroring scheme that can destroy work, and documentation that teaches
syntax that no longer compiles. Test depth is inversely proportional to how much the code
matters (11 of 27 commonTest files test telemetry; the theming core and Recycler2 have zero
behavioral tests).

---

## P0 — Do Today (data loss / broken-by-default)

1. **Commit `buildSrc/src/main/kotlin/aiDriverTasks.kt`.** It exists ONLY in the gitignored
   buildSrc mirror (not in `gradle-plugin/src` or `build-companion/src`, not in git). One
   `git clean -fdx` deletes it permanently. Move to `gradle-plugin/src/main/kotlin/` and commit.
2. **Fix `reactive(onLoad)` dead code** (`reactive` repo, `ReactiveContext.kt:736-752`):
   `wasLoadingLastTime` starts `false` and is only set inside `if (wasLoadingLastTime)` —
   unreachable, so `onLoad` can never fire and `forEachUpdating` loading placeholders never
   render. Invert the condition; add a test.
3. **Fix `DependentAction` dependency tracking** (`kiteui/reactive/Action.kt:187-224`): the
   action coroutine is launched without `+ this`, so the `DependencyChangeListener` is never
   in context and error-clear-on-dependency-change (the *default* Action behavior, i.e. every
   button) never fires. Compare `TypedReactiveContext.coroutineContext` which correctly adds
   `+ this`. Add a test.

## P1 — Correctness Bugs & Landmines (this sprint)

### Reactivity (mostly in the `reactive` repo)
4. **`Remember.state` on an inactive remember leaks subscriptions permanently**
   (`Remember.kt:97-101` via `runOnceWhileDead`): registers listeners on sources that are
   never released, and the next source change resurrects the "dead" context into a
   permanently-computing node. Fix + regression test.
5. **Add reentrancy detection**: writing to a signal read inside your own `reactive {}`
   currently gives a stack overflow or livelock with no diagnostic. An `isCalculating` flag
   that throws is cheap.
6. **Decide and document glitch semantics**: propagation is eager/synchronous with no
   batching or topological ordering — diamond dependencies observe stale intermediate states
   (can flash error theming). Minimum: document it. Better: a `batch {}` / deferred
   notification queue.
7. **Threading is assumed main-thread but never enforced** — unsynchronized ArrayLists
   everywhere, `Dispatchers.Unconfined` default in `remember`. Add debug-mode thread
   assertions in `BaseListenable.invokeAllListeners` / `Signal.value`.

### Views
8. **`NativeElement.working`/`loading` cache collides across siblings**
   (`Element.ext.kt:38-44`): caches a per-element value into the subtree-shared
   `context.addons` under a constant key. Second element in a subtree gets the first's
   status. Fix keying or remove the API.
9. **Dead-container writes silently no-op** (`NativeElement.kt:728-734`): elements written
   into a shut-down container vanish with only a `println`. Violates fail-fast — throw, or
   at least route through `Log`.
10. **`forEachUpdating` never removes views** (`foreach.kt:53-58, 79-84` — removal branches
    commented out): shrinking a list only hides children; `currentViews` grows monotonically.
    Real memory cost on JS, exactly where leaks were just fixed. Restore or document loudly.
11. **`labelFor`/`describedBy` hold strong cross-element references never cleared at
    shutdown** (`NativeElement.kt:707-708`) — retained-subtree leak class. Clear in
    `onShutdown`.
12. **Container `themeAndBack` override duplicates the base setter** with the comment
    "breaks everything for some reason" (`NativeContainerElement.kt:524-535`). Replace with a
    protected `onThemeApplied(old, new)` hook in the base setter; delete the mystery.

### Navigation / codegen
13. **`generateRoutes.kt` defects** (fix now even if KSP comes later):
    - `@QueryParameter` scan is unbounded to EOF — first class in a file collects the second
      class's params (`generateRoutes.kt:101-133`).
    - `text.indexOf("va", ...)` matches the "va" in `private` (`:120`).
    - Silent `break` on malformed input drops the rest of a file's routes (`:74, :91`) —
      make these build failures.
    - Route precedence is filesystem-order-dependent (`Routes.parse` takes `firstOrNull`) —
      sort deterministically (constant segments before variables), fail on conflicts.
    - Add golden-file tests for `generateAutoroutes`; delete the buildSrc copy once
      build-companion lands.
14. **`Navigator.js.kt` Link-mode dead code** (`:180-184`): `suppressNav = true` set
    immediately before `if (!suppressNav)` guards the whole push/pop/swap body — unreachable.
    Also: `Separate` mode's `lastStackForPath` grows unboundedly and retains every page ever
    visited; `Link` mode's localStorage entries never cleaned. Pick ONE history strategy,
    delete the rest, add tests against a fake History API.
15. **Small shipped defects** (one quick PR): `Graph.kt:321-322` prints on every canvas draw;
    `ColorPicker.kt:266-270` compares `green` twice and never `alpha`; `KiteUiActivity.kt:182-183`
    inconsistent query decoding; empty public stubs `navLayout`/`navSideBar` (`AppNavV2.kt:12-32`);
    stale `Navigator.kt-review.txt` committed inside commonMain; `zoomableImage` bypasses
    `write()` lifecycle (`dsl.kt:67-73`).

### Documentation drift (cheap, high frequency of pain)
16. **MIGRATION.md and project CLAUDE.md teach syntax that doesn't compile**: `-` operator
    chaining examples (the only remaining `minus` is a DeprecationLevel.ERROR shim),
    `withUnrestrictedModifiers` vs real `withUnsafeModifiers`, nonexistent
    `CanAddDynamicTheme`, stale "branch is BROKEN / target version-7" status. Also:
    `docs/rview-basics.md` documents RView as current; TESTING_GUIDE references `RContext`;
    CLAUDE.md says Chrome for JS tests but the build uses Firefox, and documents `jvmRun`
    which doesn't exist (it's `ssrServerRun`); README badges Kotlin 2.2.0 vs actual 2.3.20.
    Rename leftover `RContext.*.kt` / `RView.commonHtml.jvm.kt` files.

## P2 — Structural Work (version-8 milestones)

### Build & repo hygiene
17. **Replace the buildSrc mirror with `includeBuild`.** The current scheme copies
    gradle-plugin/build-companion sources into buildSrc at configuration time on every Gradle
    invocation — any sync between editing buildSrc and running `deploy-buildSrc.sh` silently
    clobbers your edits, and the plugin compiles twice with two different dependency sets
    (fontbox 2.0.27 vs 3.0.7). One source of truth ends the data-loss risk (see P0 #1).
18. **Delete or branch-archive dead code**: `library-swing` (11k lines, 44 files still
    referencing the deleted `RView` class), orphaned `example-app/src/wasmJsMain` (no wasmJs
    target exists), empty `processor/`, `example-app/src/jvmMain`, `library/src/commonJvmMain`'s
    misplaced `Example.kt`, `test-utilities/src/jvmDesktopMain` (documented uncompilable),
    DeprecationLevel.ERROR items in `deprecated.kt` (dead weight on a major-version branch).
19. **Fail loudly on non-Mac publishing**: iOS targets are gated on `os.name.contains("Mac")`
    in four build files; a Linux publisher would silently ship an iOS-less release. Assert at
    publish time; centralize the gate.
20. **Get off `reactive = 6.0.0-prerelease-45`** before the next release. A production
    framework pinned to a prerelease foundation. Start recording releases in CHANGELOG.md.
21. **Slim the Android `api` surface**: glide, photoview, media3×3, two ktor engines all leak
    to every consumer from core. Move media deps to optional modules (the lottie/camera
    pattern already exists).

### Core architecture
22. **Move AI-driver machinery out of published commonMain** (`AiDriver.kt`, `DriverSupport.kt`,
    `driverValue/driverActions/driverDisplay` on the `Element` interface). It's tooling welded
    into the core contract of a framework whose headline feature is small binaries, and
    `defaultDriverActions` allocates a map per node per snapshot walk. Extract to a
    `DriverInspectable` interface/registry or separate module; add a bundle-size regression
    check to CI either way.
23. **Untangle Element-as-CoroutineScope** (the `Element.kt:167` TODO — the author's own
    "really messed up" flag): the element is baked into its own coroutineContext twice, so
    contexts copied into longer-lived scopes strongly retain elements past shutdown. This was
    the root cause of the Remember job-ordering leak. Give elements a `scope` property
    instead of *being* the scope/context.
24. **Collapse the Theme 19-parameter × 5 duplication** (`withBack`/`withoutBack`/`alter`/
    `customize`/`copy` are five ~40-line identical cascades; adding one theme property means
    five+ edit sites failing silently if missed). Introduce a `ThemeOverlay` (all-nullable
    fields) with one `applyOverlay`; this also kills the `LinearGradient.INVALID` sentinel.
25. **Convert lambda theme derivations (`bold`, `italic`, `textSize`, `withSpacing`) into
    `Semantic`s**: they currently bypass the theme cache, run a full `Theme.copy` on every
    refresh, and allocate a new non-equal derivation per property access, defeating pipeline
    dedupe. Cheapest real theming perf win. Also bound `themeInteractiveHandled` / generated
    CSS for high-cardinality parameterized ids (slow JS leak).
26. **Recycler2 hardening** (1017 lines, 7 coordination flags incl. `stahp`, timing-based
    `afterTimeout(16/100)`, zero behavioral tests, hash-lock "manually test in 3 browsers"
    gate): extract the anchor/suppression state machine into an explicit model, purge
    scheduling side effects from `measure()`, convert the comment-block spec (lines 953-981)
    into headless layout tests, replace silent `as?`-with-default fallbacks with fail-fast,
    implement or message the `prebake` `TODO()`s.
27. **Pick one modal system**: `dialogPageNavigator` is deprecated but still wired into
    `appNav`; the replacement popover system isn't back-button aware (Android back / browser
    back don't close open dialogs). Finish popovers with back-participation, delete the old
    system.
28. **Consolidate the list-rendering family**: six mechanisms (`forEach`, `forEachUpdating`,
    `forEachById`, `forEachByIdWithoutAnimation`, `forEachAnimated`, `childrenLazyLoading`)
    with three ~80-line near-clone bodies. One implementation parameterized by
    animation/identity, plus written guidance on when to use it vs Recycler2. Same for the
    `Remember`/`RememberSuspending` and `TypedReactiveContext`/`ReactiveContextSuspending`
    duplicated pairs (divergence already produced a missing `ensureActive()` guard).
29. **Define page lifecycle semantics**: `Page` has no appear/disappear/disposal hooks;
    pages retained in the stack (and JS `lastStackForPath`) can't release resources. Also
    fix/document object-page query-param singleton mutation, and add Android saved-state
    stack restoration.

### Testing (do before/alongside the refactors above)
30. **Add theming-invariant unit tests first**: `ThemeAndBack.plus` flag OR-ing,
    `SemanticOverrides` precedence, `copy` revert recursion, pipeline step ordering,
    "same cascading theme → no child refresh". Locks in ThemeRules.md before refactors #24-25.
31. **Rebalance test coverage toward views/navigation**: extend the `PageRenderSmokeTest`
    pattern to render every CheatSheet component on all platforms; add behavioral parity
    tests for the top ~10 input components (content binding, enabled/loading) using the
    existing test-utilities harness. Platform drift (e.g., Button loading indication differs
    Android vs iOS) is currently undetectable.
32. **Reactive debuggability**: optional names on contexts, an `onRerun(context, changedDep)`
    debug hook, graph dump. Given the leak history, "why did this rerun" tooling pays for
    itself.

## P3 — Features (after the foundation is solid)

From WISHLIST.md, in suggested order (all are well-specced already):
33. **Entry/exit animations via `shownWhen`** + common `ScreenTransition` data class
    (plans/entry-exit-animations.md exists) — most user-visible payoff; the `ScreenTransition`
    redesign also improves SwapView/overlays.
34. **Nested corner radius** (`CornerRadii.Nested`) — infrastructure (`--parentSpacing`
    pattern) already proven.
35. **Corner shape / squircle support** (`cornerShape` enum on Theme) — note this interacts
    with #24; do after the ThemeOverlay refactor so it's a one-place change.
36. **Composite backgrounds** (`Paint.Composite`) — near-zero breaking, exhaustive `when`s
    flag platform work.
37. Default align/gravity property for containers (WISHLIST line 3).
38. **wasm target** — currently README-roadmap only; delete the orphaned wasmJsMain sources
    until actually started (#18).

---

## Theme-Author Ergonomics (cross-cutting, worth its own note)

The six bundled theme factories each override a different ad-hoc subset of semantics;
`Theme.clean`'s triple-nested ListSemantic override is a hand-rolled state machine no app
developer will reproduce. Warning/Danger/Affirmative semantics hard-code hex colors that
break on dark themes, and `InteractiveSemantic` sniffs `Platform.probablyAppleUser` inside
the models layer. Recommended: expand ThemeRules.md (currently 6 lines) into the real
contract — pipeline ordering, id-identity rules ("same id ⇒ treated as equal, content
ignored" is an invisible trap), cascade/revert semantics, and a "semantics every theme must
define" checklist; make the status colors theme-relative; move the Apple check to a
platform-installed override.
