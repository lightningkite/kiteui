# KiteUI Codebase Cleanup Review

Generated: 2026-02-13
Branch: version-7-all-features-backup

---

# 1. Working Directory State (Mixed Concerns)

## [Duplicate Implementations] Five competing list virtualization implementations
The working tree has **five** separate list virtualization implementations:
1. **Recycler2** (`Recycler2.kt`) - Production version, committed. Full cell placers, scrollbar, grid support.
2. **Recycler3** (`Recycler3.kt`, untracked, by Claude) - Fixed-size sentinels + CSS transforms. No item recycling.
3. **Recycler4** (`Recycler4.kt`, untracked, by Claude) - Spacers + transform anchoring + settling. More sophisticated but depends on Recycler3's `expect` declarations.
4. **VirtualList** (`VirtualList.kt`, untracked, by Claude) - `expect/actual` with true windowed rendering on HTML but non-virtualizing on Android/iOS.
5. **LazyList** (`LazyList.kt`, modified) - Now `@Deprecated`. Renders all items as fixed-height placeholders.

None of the experimental ones appear fully tested. Decision needed on which approach to keep.

## [Unregistered Test Pages] Recycler4TestPage.kt, VirtualListTestPage.kt
`@Routable` pages NOT linked from RootPage.kt. Only `Recycler3TestPage` was added. Unreachable from UI.

## [Debug Logging Left Enabled] library/src/jsMain/.../direct/modifiers.commonHtml.js.kt
`Log.tag("showHide")` enabled instead of `null`. Will spam the JS console in production.

## [Local-Only Dependency] gradle/libs.versions.toml
Reactive library version is `"5.1.3-25-local"`. Breaks for anyone without the local publish.

## [Dev Port Change] example-app/webpack.config.d/config.js
Webpack port changed from 8086 to 9000. Personal preference change.

## [Experimental Compiler Flag] library/build.gradle.kts
`-Xcontext-parameters` added but no code uses context parameters yet.

## [Mixed Concerns in Working Directory]
At least 5 concerns interleaved: list virtualization, SEO improvements, performance optimizations, memory leak fixes, and build/config changes. Should be separate branches/commits.

## [Recycler4 cleanupCache Never Called] Recycler4.kt
`cleanupCache()` exists but is never called. Height cache will grow unbounded.

## [AnimationBugReplicationPage] Staged but has unstaged modifications. Staged version is out of date.

---

# 2. Android & iOS Platform Code

## [Empty Files - Delete These]
- `library/src/androidMain/.../reactive/ideas.android.kt` - Only package declaration
- `library/src/iosMain/.../models/data.ios.kt` - Only package declaration
- `library/src/iosMain/.../PerformanceInfo.kt` - Package declaration + unused imports
- `library/src/iosMain/.../views/direct/ImageCrop.ios.kt` - Entirely commented out
- `library/src/androidMain/.../views/direct/helpers.kt` - Only commented-out code

## [Dead Code - Commented-Out Blocks]
- `Select.android.kt` lines 163-262: ~100 lines of old implementation
- `directViewActuals.kt` (iOS) lines 121-243: ~120 lines of old HTML-based code
- `KiteUiActivity.kt` lines 206-222, 227: Old keyboard subscriber
- `RView.ios.kt` lines 82-95: Old animation block
- `fetch.ios.kt` lines 38-51: Unfinished CacheStorage with TODO()

## [TODO - Runtime Crash Risks]
- iOS `Geolocation.getCurrentPosition()` throws `TODO()`
- iOS `SoundEffectPool` - no-op stub with `TODO()` in `AudioSource.load()` for AudioLocal/AudioRaw
- iOS `textPopover()` throws `TODO()`

## [TODO - Incomplete Features]
- Colored hint not implemented: `AutoCompleteTextField.ios.kt`, `NumberField.ios.kt`, `FormattedTextInput.ios.kt`
- `TextArea.ios.kt`: Hint text not implemented at all
- `LocalDateTimeField.ios.kt`: No way to clear the field
- `BottomSheet.ios.kt`: Uses popover workaround instead of native UISheetPresentationController
- `ScrollView.ios.kt`: Only one direction at a time
- `Canvas.ios.kt`: Mouse wheel not handled
- `ContainingView.ios.kt`: `spacingOverrideBeforeNext()` acknowledged as broken

## [Duplicate Code]
- `RawImageView.android.kt`: `finish()` and `GlideImageView` duplicated 3 times
- `scrollhelpers.kt` (Android): Three nearly identical `scrollToView()` overloads
- SVG path parsing duplicated between `PathDrawable.kt` (Android) and `vectors.kt` (iOS) - ~150 lines each
- `ContainingView.ios.kt`: `RowOrCol` and `RowCollapsingToColumn` have identical child management

## [Naming Issues]
- `pusedoframe.kt` (iOS): Misspelled filename and function (`setPsuedoframe` -> `setPseudoFrame`)
- `debug.kt` (iOS): Unused `debugMeasuring` variable, possible debug-only code in production

## [Debug println in Production]
- `KiteUiActivity.kt` line 77: `println("OnApplyWindowInsetsListener: $safeInsets")`
- `SwapView.android.kt` lines 50, 58: `println("addChild called with $view")`, `println("Swapping to $newViewHolder...")`
- `RContext.ios.kt` lines 42-60: Multiple `println()` calls during navigation transitions
- `ViewContextExtensions.ios.kt` lines 45, 47: `println("Waiting...")` and `println("Let's go!")`
- `ExternalServices.ios.kt` lines 431, 433, 598, 604, 685: Various debug prints

## [Deprecated API Usage]
- `KiteUiActivity.kt` line 235: `onBackPressed()` deprecated since API 33
- `KiteUiActivity.kt` lines 116-127: `startActivityForResult()` / `onActivityResult()` deprecated
- `Platform.android.kt` lines 32-43: Deprecated `systemUiVisibility` flags

## [Platform Hacks]
- `RView.android.kt` lines 276-287: Reflection for `RippleDrawable.setDrawable()`
- `RView.android.kt` lines 380-384: Touch-blocking via dummy `OnClickListener`
- Multiple iOS files: `@Suppress("SENSELESS_COMPARISON")` checking `this != null` (Kotlin/Native GC workaround) in 6+ files. Should extract to shared helper.

## [Memory Leak Risks]
- iOS `SoundEffectPool.ios.kt` line 156: `keepAlive` HashSet may leak if audioPlayerDidFinishPlaying never called
- Android `RView.android.kt` line 274: Companion-level `activeAnimators` map

## [Bug] modifiers.android.kt line 617
`widthAnimator()`: `animatingSize.add(this@widthAnimator)` called every animation frame instead of once in `apply` block (unlike `heightAnimator` which does it correctly).

## [Stale File] `geolocation.main~1.kt` (Android)
Unusual `~1` suffix suggesting merge conflict artifact.

## [Hardcoded Animation Duration] RView.ios.kt line 612
`UIView.animateWithDuration(0.5)` hardcoded instead of using theme's `transitionDuration`.

## [Complexity]
- `DesiredSizeView` (Android): Extensions on `Int` pollute namespace
- `applyBackgroundChanges()` (iOS): ~150 lines handling shadow, blur, gradient, border, etc.
- `updateTransform()` (Android): 16 repetitive `animateProperty` calls

---

# 3. CommonMain Code

## [TODO - Runtime Crashes]
- `RecyclerViewPagingPlacer.kt` line 84: `prebake()` throws `TODO()`
- `RecyclerViewPlacerHorizontalGrid.kt` line 120: `prebake()` throws `TODO()` for multi-row
- `RecyclerViewPlacerVerticalGrid.kt` line 164: `prebake()` throws `TODO()` for multi-column
- `Navigator.kt` lines 62-70, 80: Deprecated stubs throw `TODO()` - crash-on-access traps

## [Duplicate Code]
- `Paint.kt`: `closestColor()` duplicated in `LinearGradient` and `RadialGradient`
- `RViewHelper.kt` lines 526-582: `listenForWorking` and `listenForStatus` follow identical patterns

## [Commented-Out Code]
- `deprecated.kt` lines 12-28: Old modifier definitions

## [Debug println]
- `RViewHelper.kt` lines 287-300: Warning printlns during normal view operations
- `RViewHelper.kt` lines 339-406: Multiple printlns during shutdown
- `forEachReorderable.kt` lines 61-62: Uninformative `println("Exception")` and `println("Not Ready")`

## [Large TODO Block] RViewHelper.kt lines 695-735
40-line design note about tree walking. Should be a GitHub issue or removed.

## [Overly Complex]
- `Recycler2.kt` `measure()`: 270+ lines (547-819), deeply nested
- `Paint.kt` `HSPColor.toRGB()`: 55 lines with 6 nested `when` branches, no comments
- `Recycler2.kt` line 342: Empty `init {}` block

## [Deprecated Wrappers] Theme.m.kt lines 110-168
`MaterialLikeTheme` is deprecated but large (13 parameters). Remove if no consumers remain.

## [Missing @Deprecated] Theme.m3.kt
`M3Theme` is functionally identical to deprecated `MaterialLikeTheme` but not marked `@Deprecated`.

## [Sentinel Value] Theme.kt lines 1475-1499
`LinearGradient.INVALID` used as sentinel. Consider nullable types instead.

## [Stale Deprecated] deprecated.kt
Deprecated `encodeURIComponent`/`decodeURIComponent` wrappers. Remove if deprecated for a full version.

## [Hardcoded Colors] Theme.kt
`WarningSemantic`, `DangerSemantic`, `AffirmativeSemantic` use hardcoded hex values instead of deriving from theme.

## [Memory Leak] VirtualList.kt lines 60-61
`addListener` in `ItemScopeManager` returns removal handle but no cleanup on item recycle.

## [Thread Safety] PerformanceInfo.kt
`HashMap` and mutable state without synchronization in companion object.

## [Magic Number] Recycler2.kt line 339
`val reallyBig = 50_000.0` - unnamed sentinel value.

## [Naming Typos]
- `Recycler2.kt` line 158: `reuseableCells` -> `reusableCells`
- `RecyclerViewPlacerVerticalGrid.kt` line 93: Comment "existin" -> "existing"

## [Inconsistent Error Handling] SwapView.kt line 38
Wraps exception before reporting (`Exception("Failed to render $next", e).report()`) while other code uses `e.report()` directly.

---

# 4. CommonHTML, JS, and JVM SSR Code

## [Empty/Dead Files]
- `jsMain/.../reactive/ideas.js.kt`: Empty file with package declaration + unused import
- `commonHtmlMain/.../views/direct/ImageCrop.commonHtml.kt`: ~324 lines entirely commented out

## [Dead Code - Commented Out]
- `DynamicCss.js.kt` lines 152-256: ~105 lines of old duplicate implementation
- `DynamicCss.js.kt` lines 95-126: ~30 lines of commented-out merged CSS optimization
- `SwapView.commonHtml.kt` lines 27-64: ~38 lines of old implementation
- `Select.commonHtml.kt` lines 79-93: Commented-out helper functions
- `Canvas.commonHtml.kt` lines 110-126: Old implementation
- `DynamicCss.kt` lines 9-16: Old API method signatures
- `dom2.kt` lines 38-42: Commented-out properties

## [Dead Code - Unused Variables]
- `RView.commonHtml.kt` line 194: `private var idCounter = 0` never used
- `DynamicCss.js.kt` lines 90-91: `flushTotal` and `ruleTotal` accumulated but never read
- `KiteUiCss.kt` line 782: `cssGenTotal: Duration` accumulated but never read

## [CSS Bugs]
- `KiteUiCss.kt` line 284: `max-width: 100` missing unit (should be `100%`)
- `KiteUiCss.kt` line 157: Stray semicolon before `!important` - `var(--nearest-background-color); !important` makes `!important` a separate invalid declaration
- `KiteUiCss.kt` line 178: Empty CSS rule `.touchscreenOnly {}` - no-op
- `KiteUiCss.kt` lines 975, 985: Duplicate `line-height` for `.kui` - second (`unset`) overwrites first (`1.2`)
- `KiteUiCss.kt` lines 110-132 vs 690-735: Duplicate CSS rules for progress/input/range (one set in try/catch)

## [Memory Leak Bug] RadioToggleButton.commonHtml.kt line 56-62
`checked.addListener` does NOT call `.also(::onRemove)`. Listeners leak on every create/destroy cycle. Compare with `ToggleButton.commonHtml.kt` line 64 which does it correctly.

## [Duplicate Code]
- `RadioToggleButton.commonHtml.kt` and `ToggleButton.commonHtml.kt`: ~90% identical
- `modifiers.commonHtml.kt`: `sizedBox` and `changingSizeConstraints` duplicate CSS property-setting logic
- `forEach` helper duplicated in `RView.commonHtml.js.kt` and `modifiers.commonHtml.js.kt`
- `toBoxShadow()` exists in both `data.commonHtml.kt` and `KiteUiCss.kt` with different implementations

## [Naming/Typo] AutoCompleteTextFIeld.commonHtml.kt
Filename has uppercase `I` in `FIeld` (should be `TextField`).

## [TODO - Runtime Crashes]
- `WebView.commonHtml.kt`: Both `content` getter and setter throw `TODO()`
- `AutoCompleteTextFIeld.commonHtml.kt` line 56: `suggestions` setter throws `TODO()`
- `RawImageView.commonHtml.kt` line 109: `ImageScaleType.Stretch` throws `TODO()` in SizelessRawImageView
- `TextView.commonHtml.kt` line 57: `wordBreak` getter throws `TODO()`

## [JVM SSR Stubs - Runtime Crashes]
- `ExternalServices.jvm.kt`: 5 functions throw `TODO()`
- `SoundEffectPool.jvm.kt`: `play()` and `load()` throw `TODO()`
- `ScrollView.commonHtml.jvm.kt`: TODO stub

## [Overly Complex] modifiers.commonHtml.js.kt - showHideWorker
~210 lines (144-354) with nested conditions, timing, MutationObservers, and animation frames.

## [Swallowed Exceptions] KiteUiCss.kt lines 690-735
CSS insertions in try/catch with `/*squish*/` silently ignore failures.

## [Empty Stubs] CoordinatorFrame.commonHtml.kt lines 148-154
`onLeftSwipe` and `onRightSwipe` silently do nothing.

---

# 5. Example App, Build Config, and Top-Level Files

## [Stale Files to Remove/Gitignore]
- `build.gradle.kts-review.txt` - AI review artifact
- `claude-opinion.md` - AI design critique
- `library/build.gradle.kts-review.txt` - AI review artifact
- `.fork/current-session-id` - IDE/tool session file
- `reactive-performance-suggestions.md` - Belongs in reactive library repo
- `worktrees/` - Git worktree (should be in .gitignore)

## [Build Config Issues]
- `libs.versions.toml`: Duplicate roborazzi versions (1.29.0 vs 1.51.0)
- `library/build.gradle.kts`: Top-level `dependencies { implementation(libs.ktor.client.okhttp.jvm) }` duplicated/misplaced
- `library/build.gradle.kts`: Commented-out code (cocoapods, explicitApi, jvmSwing target)
- `example-app/build.gradle.kts`: Conflicting yarn config (NONE vs WARNING) with root
- `buildSrc/build.gradle.kts`: Kotlin version drift (2.2.0 vs 2.2.20 in libs.versions.toml)
- `buildSrc/build.gradle.kts`: fontbox major version divergence (2.0.27 vs 3.0.6)
- `gradle.properties`: Unused `versionMinor = 3`
- `example-app/webpack.config.d/config.js`: BundleAnalyzerPlugin always on

## [Dead Code - Example Pages]
Unreachable from navigation (not linked from RootPage):
- `AnimationTest2Page.kt`, `AnimationTestPage.kt`, `GamepadTestPage.kt`
- `MarkdownDemoPage.kt`, `ScrollElementTestPage.kt`, `ViewPagerCenterIndexTestPage.kt`
- `TestPage.kt` - Empty page, should be removed
- `FullExamplePage.kt` - Contains `FullScreenPage` class (naming mismatch), half commented out
- `TemplatePage.kt` - Placeholder with generic "Name of Topic" content
- `DataLoadingPatternsPage.kt.wip` and `FormsAndValidationPage.kt.wip` - .wip files in git

## [TODO - User Visible] HomePage.kt line 133
`text("TODO")` displayed on the landing page under "Getting Started".

## [Naming Mismatches]
- `FullExamplePage.kt` -> class `FullScreenPage`
- `GraphExample.kt` -> class `GraphExamplePage`
- `SliderExample.kt` -> class `SliderExamplePage`

## [RootPage Debug Code]
Lines 124-136: "GC" and "Cause Leak" buttons in user-facing page. Line 139: `println("Left root screen")`.

## [Missing .gitignore Entries]
`.fork/`, `*.wip`, `worktrees/`, `*-review.txt`, `claude-opinion.md`

## [Stale Documentation]
- `WISHLIST.md`: Single bullet point
- `ThemeRules.md`: 340 bytes, last modified Nov 2023
- `docs/`: 17 AI review artifacts from Nov 25, 2024

## [Redundancy] buildSrc <-> gradle-plugin
Circular copying: `buildSrc/build.gradle.kts` copies from gradle-plugin at config time; `deploy-buildSrc.sh` copies back.

---

# 6. Additional Library Modules

## [Stale Module] processor/
Included in `settings.gradle.kts` but contains NO source files. Only stale build artifacts with old `com.lightningkite.rock` package name. Route generation moved to `gradle-plugin`.

## [Dead Code in library-swing]
- `TestApp.kt` and `WeightTest.kt` are manual `main()` programs in the library source set (shipped with library)

## [Stub Implementations - Runtime Crashes]
- `library-swing` SoundEffectPool: `play()` and `load()` throw `TODO()`
- `library-swing` ExternalServices: 5 functions throw `TODO()` (requestFile, requestFiles, requestCaptureSelf, requestCaptureEnvironment, setClipboardText)
- `library-swing` Geolocation: `getCurrentPosition()` suspends forever (never resumes)
- `library-swing` audio: Complete no-op stubs

## [Build Issues]
- Fragile `kotlin.srcDir()` source sharing in library-swing, library-camera-swing, library-lottie-swing
- Inconsistent JVM targets: library-swing (1.8) vs camera-swing/lottie-swing/example-swing (11)
- test-utilities `compileSdk` 35 vs library-camera/library-lottie at 36
- example-app-swing points `iosProjectRoot` to example-app-ios (confusing workaround)
- example-app-swing manually rewrites font code via string manipulation (fragile)

## [Missing Tests]
- library-camera: No tests
- library-lottie: No tests (has pure logic in LottieInterpolation/LottieRenderer that's easy to test)
- library-swing: No tests (manual mains don't count)
- test-utilities: No tests for its own utilities
- gradle-plugin: 1 test file, 1 test, 2 assertions

## [TODOs in Lottie]
- `LottieRenderer.kt`: Handle skew, implement trim path, apply line cap/join

## [TODOs in Swing]
- Dark mode detection, overlay support, pull-to-refresh, hint/popover, reactive size constraints, reactive visibility, circular progress, app version

## [Potential Bug] library-lottie iOS LottieView.ios.kt:47
Creates `CoroutineScope(Dispatchers.Main + SupervisorJob())` not connected to lifecycle.

## [Debug Logging] gradle-plugin KiteUiPlugin.kt lines 61, 91
`println("CONFIGURE kspKotlinJs")` on every Gradle build.

## [Stale iOS Tests] example-app-ios/
Swift test files still use "Rock" naming. Contain only template/placeholder tests.

## [Serialization Gap] LottieSerializers.kt
All three serializers throw `NotImplementedError` in `serialize()`.

## [Feature Gap] library-camera-swing and library-lottie-swing
Camera-swing shows "no camera". Lottie-swing loads from CDN (requires internet, no offline support).

## [Misleading Comment] library-swing AppState.jvm.kt:44
Says "we're server-side" but this is the desktop Swing module.

## [Empty Directories] library-lottie
Empty `models/` directories in 5 platform source sets.
