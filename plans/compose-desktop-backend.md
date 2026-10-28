# Compose desktop backend

Goal: a desktop (JVM) KiteUI backend rendered with Compose Multiplatform, built so the same view code can later back Android and iOS as an alternative to the native backends.  Web stays on the HTML backend.

Scope: everything in `commonMain` gets an actual, including video.  Out of scope: WebView (throws `UnsupportedOperationException` on construction), gamepads, audio capture (clear "not supported" errors), Compose on Android/iOS (but `composeMain` must not use JVM-only APIs, to keep that door open).

## Proven already

`tmp/compose-spike` (Compose MP 1.12.1, Kotlin 2.4.20): a retained element tree with properties in Compose snapshot state renders and updates when properties are written from outside composition.  Headless `runComposeUiTest` can click, type, assert and screenshot inside the agent sandbox.  An in-app screenshot via `rememberGraphicsLayer().toImageBitmap()` works headless, which is what `driverScreenshot()` will use.  Real windows can't open inside the sandbox (fine: everything agents need is headless).

## Module layout

Kotlin ≥ 2.1 rejects two JVM targets in one module, and `:library` already has `jvm("jvmSsr")`.  So:

- **`library/src/commonJvmMain`** becomes a real directory.  Move the plain-JVM actuals out of `jvmSsrMain` into it: hashing, clockMillis, platformSynchronized, Blob/FileReference/fetch/websocket (ktor okhttp), LocalTimeRender, ImageCompression, TelemetryCrashHook, tryAutoStartDaemon.  SSR keeps its stubs.  No behavior change; verified by `jvmSsrTest`.
- **`library-compose`** (new module, artifact `kiteui-compose`): one `jvm()` target for now.
  - `commonMain` adds `../library/src/commonMain` via `srcDir` (the `library-swing` precedent).
  - `composeMain` (own dir): the Compose view implementations, using only multiplatform Compose APIs.
  - `jvmMain`: adds `../library/src/commonJvmMain` via `srcDir`, plus desktop-only actuals (window root, storage, external services, audio, video wiring).
  - Compose deps by plain coordinates (`org.jetbrains.compose.foundation:foundation:1.12.1`, `desktop-jvm-*`), no Material.
- **`example-app-desktop`**: `srcDir`s `example-app/src/commonMain`, depends on `:library-compose`, has `main()`, and desktop versions of `PlatformSpecificScreen` and `widgets/Code`.
- **`test-utilities`**: a desktop `uiTest` actual on `runComposeUiTest`, so driver-command UI tests (`UiTestScope`) run headless against the desktop backend.

Resource codegen: the gradle plugin's JVM output already emits commonHtml-shaped resources (`Font(cssFontFamilyName, direct = FontDirect(...))`, `ImageResource(relativeUrl)`, files under `resources/common/`).  The desktop actuals take the **same constructor shapes** and load those paths from the classpath, so no plugin change is needed.  (The swing branch post-processed the generated file instead; avoid that.)

## Core model

**Element ↔ Compose bridge.** `NativeElement` keeps KiteUI's retained tree and owns snapshot state for everything it renders: `themeAndBack`, `shown`, `visible`, `opacity`, `ignoreInteraction`, layout settings.  Each element has `@Composable fun Render(modifier: Modifier)`.  `NativeContainerElement` keeps a `mutableStateListOf<NativeElement>` that `nativeAddChild/RemoveChild/ClearChildren` edit, and renders children with `key(child)`.  Properties are set before an element is ever composed (during setup), which snapshot state handles naturally.

**Units.** `DimensionRaw = Float` in dp.  `px` is dp (same as CSS px, which are device-independent), `rem` is a fixed 16dp (no font-scale setting on desktop yet).  Desktop density comes from Compose's `LocalDensity`.

**Modifiers** (`align`, `weight`, `dynamicWeight`, `sizedBox`, `dynamicSizeConstraints`, `shownWhen`, `shownForQuery`) store their values on the element (snapshot state) in `beforeSetup`; the parent reads them when laying out.  `sizedBox` applies `sizeIn`/`size`/`aspectRatio` on the element itself (the web approach, no wrapper element).  Scrolling injects a scroll-container element via `lazyInjectModifierWriter`, like Android, because `ScrollingBehaviors` needs an element to live on.  `shownWhen` sets `shown`, animating after setup (see Animations).

**Layout rules** (from the web and Android behavior, not Compose's defaults):
- Linear (`RowOrCol`): main axis wraps; `weight` takes a proportional share of the remaining space (flex-basis 0); `Stretch` on the main axis means wrap.  Cross axis defaults to stretch, meaning **the container's cross size**, which for a wrap-content container is the largest non-stretch child.  The gap goes only between shown children.  Hidden (`shown = false`) children are not composed at all.
- `Frame`: children overlap; each one stretches by default, otherwise it's positioned by its alignment.  The container wraps to its largest child.
- Compose's `Row`/`Column`/`Box` stretch differently (`fillMaxWidth` inside a wrap-content parent fills the *grandparent*).  So `RowOrCol` and `Frame` each get one custom `Layout`: measure non-stretch children, settle the container size, then measure stretch and weighted children against it.  The parent reads alignment and weight straight from its child elements by index, with no ParentData plumbing.
- `RowCollapsingToColumn` switches the same layout's orientation from `AppState.windowInfo().width`, ignoring weights in column mode.  `RowWrapping` uses `FlowRow`.  `ProgrammaticLayout` is a `Layout` that calls the delegate's measure/place.  `spacingOverrideBeforeNext` is a no-op (deprecated everywhere).

**Theming.** `nativeApplyTheme` stores the `ThemeAndBack`; `Render` turns it into modifiers:
- With `drawBackground`: a `graphicsLayer` with the shadow (and opacity and transform), a `drawWithCache` drawing the background and the border (inset like `Modifier.border`; drawn by hand so animated colours are read while drawing), `clip(shape)`.
- Then `padding(appliedPadding)`.
- Corner radii resolve each `CornerRadii` variant against the parent's `spacingForChildCornerRadii`.  Paints map to `SolidColor`, `Brush.linearGradient` (exact angle, which is better than Android) and `Brush.radialGradient`.  `FadingColor` backgrounds fade between their colours; elsewhere it is the base colour.
- Text reads `theme.font` (`FontFamily`, weight, italic, size, line height, letter spacing, all-caps, underline/strike, alignment) via a `TextStyle`.

Interactive states work like the web's CSS: interactive elements watch their `MutableInteractionSource` (hovered, pressed, focused) and render `theme[HoverSemantic]`, `theme[DownSemantic]` and so on.  Those themes are already cached in common code.

**Root.** `kiteUiWindow(theme) { app(navigator) }` opens a Compose `Window` whose content is the root element's `Render()`.  It mirrors `KiteUiActivity`: root `ElementContext`, `themeChoice = SetAsBase(theme)`, `safeInsets = Edges.ZERO`, and `AppState.windowInfo`/`inForeground`/`animationFrame`/keyboard driven from the window.  There is also a headless root for tests.  `Dispatchers.Main` is the Swing EDT (`kotlinx-coroutines-swing`); `assertMainThread` checks for it.

**Navigation.** It uses an in-memory stack.  Back is Alt+Left or the mouse back button, and closes the top dialog before going back a page (as on Android).  `askForConfirmNavigateAway` uses `JOptionPane`, since it must answer synchronously (like the web's `window.confirm`); closing the window asks the same way (like `beforeunload`).

**AI driver.** `driverScreenshot()` uses the graphics-layer capture above.  Each view wires the existing common helpers in `DriverOverrides.kt`; `MenuButton` passes its open-menu function like commonHtml does.  `tryAutoStartDaemon` is shared JVM code.

## Platform services (jvmMain)

| Expect | Desktop actual |
|---|---|
| `Platform` | `os.name`; `usesTouchscreen = false` |
| `onMainThread`, `afterTimeout` | Swing dispatcher / coroutine delay |
| `PlatformStorage` | `java.util.prefs.Preferences` per app |
| `ExternalServices` | `java.awt.Desktop` (open URL, map URL as on the web, `.ics` opened in the calendar app), `FileDialog` pickers (capture falls back to the picker, as desktop browsers do), AWT clipboard, downloads into `~/Downloads` or `~/Pictures` without overwriting; share/geolocation throw unsupported |
| `Font`, `ImageResource`, `VideoResource`, `AudioResource` | commonHtml constructor shapes, loaded from the classpath |
| `KeyCode` | Compose `Key.keyCode` (`Key` is a value class, which can't actualize an expect class).  The window's `onKeyEvent` feeds `onUniversalKeyboard`, so keys a focused text field uses don't reach it (unlike the web) |
| `SoundEffectPool`, `AudioPlayback` | `javax.sound.sampled` (wav; mp3/ogg via the video library if it handles audio, otherwise unsupported) |
| `getAppVersion`, `Build` | from the jar manifest |
| Gamepads, AudioCapture, HydrationContext | stubs (no pads / throws unsupported / not hydrating) |

**Video**: [ComposeMediaPlayer](https://github.com/kdroidFilter/ComposeMediaPlayer) (MIT; native AVFoundation, Media Foundation and GStreamer backends via JNI; no install needed).  It's a niche library: audit it (size, native libs, maintenance, licence of embedded natives) before adopting.  It also covers Android and iOS for the future.  `RawVideoView` maps `VideoRemote`/`VideoLocal`/`VideoResource` to URLs or files, writes `VideoRaw` to a temp file, polls time on `animationFrame` while playing (as Android does), and releases the player on shutdown.

## Phases

0. **Plumbing (1–2 days).**  Get the `commonJvmMain` extraction done, plus an empty `library-compose` that compiles `commonMain` with every actual as `TODO()`, and a CI-able `jvmTest`.  Gate: the existing `jvmSsrTest` and JS tests are unchanged.
1. **Core (1–1.5 weeks).**  Units, base elements, `ElementContext`, theming, window and headless roots, AppState, threading, storage, navigation, `driverScreenshot`.  Views: Frame, RowOrCol, Space, Separator, TextView, TextInput, Button, IconView, scrolling, SwapView.  Gate: a login-style page renders headless.  Its screenshot is compared against the web backend, and the driver can type, click and assert.
2. **Breadth (2–3 weeks).**  All remaining views: toggles/checkbox/switch/radio, slider, select, MenuButton with popovers, overlays and dialogs, TextArea, number/formatted/autocomplete inputs, date/time fields, RowWrapping, RowCollapsingToColumn, ProgrammaticLayout, CoordinatorFrame (bottom sheet, sliding panels), image views (including zoomable), Canvas (`DrawingContext2D` on `DrawScope`), progress/activity indicators, links, drag and drop.  Gate: every example-app CheatSheet page screenshotted against web, differences fixed or listed.
3. **Media and services (1 week).**  Video (after the audit), audio, ExternalServices.
4. **Example app and tests (1 week).**  Build `example-app-desktop`, add the `uiTest` actual, and run example-app's driver-based UI tests on desktop.

Total: roughly 5–7 weeks.

## Status

Merged into version-8.3.  Phases 0–3 are done, and most of phase 4.  Tests: 130 desktop tests (5 skip in the agent sandbox: no audio device, no video decoding; 1 is the ignored Compose leak reproduction), 848 SSR tests, JS compiles.

- **Views:** all of them except WebView (out of scope; throws `UnsupportedOperationException`) and `CameraPreview` (no webcam support yet).  Twins `library-lottie-compose` (LottieView via the common `LottieRenderer`) and `library-camera-compose` (placeholder).
- **Example app:** `example-app-desktop` runs the whole example app; `ExampleAppTest` renders the home page and fails on any element error.
- **AI driver:** `kiteUiHeadless` renders offscreen; `./gradlew :example-app-desktop:run --args=--headless` with `KITEUI_DEVELOPMENT=true` connects to the relay, and `kiteui-drive` snapshot/screenshot/find/navigate/back/url work.
- **Packaging:** the JetBrains Compose Gradle plugin builds the `.app` (135 MB with its runtime).  Building with Homebrew's JDK needs `-Pcompose.desktop.packaging.checkJdkVendor=false`; the `.dmg` step (`hdiutil`) can't run in the agent sandbox.
- **Gradle plugin:** `commonMainDirectory` for twin apps; `iosProjectRoot` optional; JVM resources go on the classpath under `common/`.
- **Common-code changes:** `MutableCoroutineContext.plus` now lets later elements win (flows collected in element scopes failed on every platform); the AI driver reports errors (including `Error`s) instead of timing out, and `url` returns a leading `/`; `allowNavigate` is internal so the desktop window close reuses it.
- Decided while building:
  - Explicit sizes are capped by the parent's space and centered in a stretched slot (Android's behavior).  Wrapping containers size stretch axes from intrinsics, since Compose measures each child once; the layouts define their own intrinsics.
  - One input rule: an element takes input only when it and its ancestors are visible and it doesn't ignore interaction.  Hover is element state; interactions feed KiteUI's theme pipeline as on iOS.
  - `rem` is a fixed 16dp.  `KiteUiRoot` must be built on the main (Swing) thread.
- **Animations** (branch `compose-animations`), following the web where it and Android differ:
  - `animationsEnabled` is a global flag, as on Android and JS; `withoutAnimation` clears it, making property changes (theme, opacity, `shownWhen`, `dynamicWeight`) instant.  Like the web, `animateIn`/`animateOut` play regardless.
  - Animations outside composition run on the root composition's frame clock and dispatcher (`animationContext`, set by `KiteUiRoot`), as Android's run on its Choreographer, so they follow a test's clock.
  - Theme and opacity changes (hover, press and focus themes included) animate linearly over the theme's `transitionDuration` once the element is on screen: background, outline and text colours, opacity, elevation, and `Theme.transform` (now applied, to elements drawing a background as on the web).  Gradients change at once; icon colours change at once (as on the web, whose transition list has no `fill`).  Values are read while drawing, so frames redraw without recomposing or relayout.
  - `animateIn`/`animateOut` play every `ScreenTransition` (fade, slides as fractions of the element's own size like CSS's `translate(%)`, scale, rotation, easing); a transition with nothing to animate finishes at once.  `SwapView` animates the old view out over the new one (clipped, like the web's) and removes it after; overlays and dialogs use the same.
  - `shownWhen` plays its transition and, in a row or column, collapses the element's size and its gap so siblings move smoothly (weighted children shrink their weight, as the web animates `flex-grow`); in other parents only the transition plays.  An interrupted change continues from where it got to.  `shownForQuery` is instant, like CSS media queries.
  - `dynamicWeight` animates linearly; a weight of 0 (natural size) blends into a positive one.
  - `FadingColor` backgrounds fade there and back in 2s.
- **Idle work**: a still page draws no frames (`IdleWorkTest`).  Endless animations (`ActivityIndicator`, `FadingColor`) run only while they can be seen: in the window (`onVisibilityChanged`), visible and not transparent.  Image and video views keep a transparent spinner once loaded, which used to redraw every frame.  `AppState.animationFrame` ticks only while something listens.  Navigating back and forth leaves no element coroutines or listeners behind.
  - Scroll benchmark (5 runs): median 1.41–1.44ms (was 1.30–1.36), p90 1.96–2.05ms (was 1.98–2.03), p99 2.83–3.16ms (was 2.63–2.97).  The small rise is hover transitions running while scrolling, the price of any animation (a bare `withFrameNanos` loop alone accounts for most of it); render and layout counts are unchanged.  A first version on Compose `Transition`s recomposed twice as often and cost twice as much.

- Media: `RawVideoView` uses ComposeMediaPlayer (opened paused: its default autoplays; macOS never calls `onPlaybackEnded`, so completion is detected from position).  Audio uses `javax.sound` (WAV/AIFF/AU only).  Playback can't be verified in the sandbox: run `MediaTest` on a real Mac.  Linux needs GStreamer installed.

- **Desktop vs web comparison** (`docs/compare/index.html`, untracked, 106 routes): layout, theming and components match the web closely.  Remaining differences are expected: font (the web loads Montserrat; desktop uses the system font, so text is slightly narrower and shorter), pre-formatted code blocks scroll instead of wrapping, and remote images can't load in the sandbox.  Fixed from the comparison: weights fill a minimum size (scrolling rows centering their content), bare progress bars.

- **Fix round after an audit** (branch `compose-fixes`, 2026-10-28):
  - Scrolling: animated scrolls run on the frame clock (they threw before); jumps wait for the scroller's next measure and don't cancel a fling; a jump to where it already is does nothing; snapping settles on the nearest child, and `scrollSnapStop` holds a scroll between the stops beside where it started; `userScrollEnabled`; drawn, draggable scrollbars.  `screenRectangle` is unclipped, as on the web.
  - Recycler2 runs its fake scroll (the draggable whole-list scrollbar) on desktop as on the web, and only suppresses the fake scroll's next event when it really moves (the web had that bug too).  `RecyclerTest` covers wheel, jumps, animated scrolls, data changes, cell heights, pager buttons and snapping, and the scrollbar.
  - Views: lost backgrounds stop drawing, slider ranges, closing panels twice, popovers close when their anchor leaves, canvas focal gradients, aspect ratios from a fixed height, coordinator sheet and panel animations, canvas wheel deltas in pixels, `scrollIntoView` alignment, break-all, all-caps in every text view, slider hover, keyboard secondary actions, `SwapView` writing several views.
  - Platform: binary resources load from the classpath; `PlatformStorage` is a JSON file in the app-data folder (no `java.util.prefs` size limits or main-thread flushes; old prefs data is not migrated); headless errors name what was attempted; `canvasUnits` are pixels.
  - Media: video temp files once per resource or blob; audio clips open only while playing, pooled to `concurrency`, decoded to 16-bit PCM so volume works; seeking notifies; GIFs animate while seen.  `AudioTest` runs on a fake mixer.

### Known issues and TODOs

- **Compose modifier retention** (`NavigationLeakTest`): Compose UI 1.12.1 keeps removed nodes' modifiers, and so their elements, reachable after a structural modifier change (adding or removing a modifier).  A node's `NodeChain.cachedDiffer` keeps the lists of its last structural diff; they go back to a pool shared by all nodes, so one can become another node's modifier list, which stays reachable when that node is removed.  `PureComposeRetentionTest` reproduces it without KiteUI (ignored, since it fails): run it after upgrading Compose, and report it upstream.
  KiteUI avoids it two ways.  `NativeElement.Render` keeps an element's modifiers the same shape as it changes: theme (hover, press, loading), visibility, input and animations change modifier values only, and which modifiers there are is decided at setup (`shownWhen`, scrolling), apart from rare changes such as sizing.  And a shut-down container drops its Compose children too, so a stale reference holds one element rather than its subtree.  Of 6,668 elements removed in `NavigationLeakTest`, 606 used to survive (42 MB live heap after the test); either measure alone brings that to 0 (stable modifiers) or 6 (dropping children), together 0 (36 MB), and the test now requires 0.  Still structural: a view's own `Content` modifiers that depend on taking input (the popover layer, zoomable images, canvases), `sizeConstraints` changes, and an element's first `FadingColor` background (watching the window from then on; watching from the start, on every element, slowed scrolling by 7%).
  KiteUI used to turn this into an unbounded leak, and every page ever shown stayed in memory (6,668 of 6,668).  Elements kept their `LayoutCoordinates` after leaving composition.  `ProgrammaticLayout`'s measure pass also kept its layout nodes, and `Recycler2` holds on to the pass.  Through those layout nodes' own stale lists, each retained element reached other removed ones.  Elements now drop their coordinates when they leave composition, and a pass lets go of its layout nodes when it ends.
- Date/time pickers (fields accept typed values only), popover focus and following a scrolling anchor, `keepScreenOn`, webcam, MP3 audio.  Not animated: text colour in views other than `TextView` (inputs, checkboxes and so on change at once), corner radii, outline widths, `RowWrapping` children's `shownWhen` collapse.
- Untested without a real window/devices: file dialogs, window close confirmation, `inForeground`, video playback, audio output (`MediaTest`, and `AudioTest`'s assumptions about real clips).
- Recycler2's fake-scroll change affects the web: retest Recycler View and View Pager in browsers (`jsBrowserTest` needs npm packages the agent sandbox can't download).
- Still to do: the leave-page confirm has no parent window; SSR's binary resources likely have desktop's old fetch bug; Recycler2 hears the viewport a frame late (overdraw hides it at normal speeds); `allCaps` on a container doesn't reach its children (the web's CSS inherits); slider `step = 0f` divides by zero (Android: continuous; web: step 1); break-all's zero-width spaces show up in semantics; `scrollIntoView` moves only the nearest scroller; drag and drop, focus trap, accessibility landmarks, date/time pickers.
- Now structural (see the retention note): an animated GIF's visibility watcher, added once it loads.
- `:example-app:jvmSsrTest` aborts (exit 134) in the agent sandbox after 4 suites pass, independently of this work.
- `test-utilities` has no desktop twin yet, so driver-command `uiTest`s don't run on desktop (the AI driver itself works).

## Open decisions

1. **How consumer apps add desktop**: decided, twin modules (`library-compose`, `library-*-compose`, an app's `-desktop` module with `commonMainDirectory`).  Every add-on library with platform views needs a twin.
2. **Moving `overlay` to common code**: the Android, iOS, HTML and Compose copies are nearly identical, and all silently log and return when there's no overlay frame.  Moving it (and failing fast) touches every platform, including iOS, which can't be built in the agent sandbox.
