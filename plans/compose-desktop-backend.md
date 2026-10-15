# Compose desktop backend

Goal: a desktop (JVM) KiteUI backend rendered with Compose Multiplatform, built so the same view code can later back Android and iOS as an alternative to the native backends.  Web stays on the HTML backend.

Scope: everything in `commonMain` gets an actual, including video.  Out of scope: WebView (throws `UnsupportedOperationException` on construction), animated theme/screen transitions (`areAnimationsEnabled = false`, `animateIn/Out` complete immediately), gamepads, audio capture (clear "not supported" errors), Compose on Android/iOS (but `composeMain` must not use JVM-only APIs, to keep that door open).

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

**Units.** `DimensionRaw = Float` in dp.  `px` is dp (same as CSS px, which are device-independent), `rem` is 16dp scaled by the font-scale setting.  Desktop density comes from Compose's `LocalDensity`.

**Modifiers** (`align`, `weight`, `dynamicWeight`, `sizedBox`, `dynamicSizeConstraints`, `shownWhen`, `shownForQuery`) store their values on the element (snapshot state) in `beforeSetup`; the parent reads them when laying out.  `sizedBox` applies `sizeIn`/`size`/`aspectRatio` on the element itself (the web approach, no wrapper element).  Scrolling injects a scroll-container element via `lazyInjectModifierWriter`, like Android, because `ScrollingBehaviors` needs an element to live on.  `shownWhen` just sets `shown`.

**Layout rules** (from the web and Android behavior, not Compose's defaults):
- Linear (`RowOrCol`): main axis wraps; `weight` takes a proportional share of the remaining space (flex-basis 0); `Stretch` on the main axis means wrap.  Cross axis defaults to stretch, meaning **the container's cross size**, which for a wrap-content container is the largest non-stretch child.  The gap goes only between shown children.  Hidden (`shown = false`) children are not composed at all.
- `Frame`: children overlap; each one stretches by default, otherwise it's positioned by its alignment.  The container wraps to its largest child.
- Compose's `Row`/`Column`/`Box` stretch differently (`fillMaxWidth` inside a wrap-content parent fills the *grandparent*).  So `RowOrCol` and `Frame` each get one custom `Layout`: measure non-stretch children, settle the container size, then measure stretch and weighted children against it.  The parent reads alignment and weight straight from its child elements by index, with no ParentData plumbing.
- `RowCollapsingToColumn` switches the same layout's orientation from `AppState.windowInfo().width`, ignoring weights in column mode.  `RowWrapping` uses `FlowRow`.  `ProgrammaticLayout` is a `Layout` that calls the delegate's measure/place.  `spacingOverrideBeforeNext` is a no-op (deprecated everywhere).

**Theming.** `nativeApplyTheme` stores the `ThemeAndBack`; `Render` turns it into modifiers:
- With `drawBackground`: `shadow(elevation, shape)`, `background(brush, shape)`, `border(outlineWidth, outline, shape)`, `clip(shape)`.
- Then `padding(appliedPadding)`.
- Corner radii resolve each `CornerRadii` variant against the parent's `spacingForChildCornerRadii`.  Paints map to `SolidColor`, `Brush.linearGradient` (exact angle, which is better than Android) and `Brush.radialGradient`.  `FadingColor` uses its base colour for now.
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

- Phase 0 done: `commonJvmMain` holds the shared JVM actuals (jvmSsrTest unchanged, 819 passing); `library-compose` compiles, with not-yet-implemented views throwing `NotImplementedError("Compose backend: …")` on construction.
- Phase 1 in progress.  Written: base/container/interactive elements, `ElementContext`, theming, `Frame`/`RowOrCol`/`RowCollapsingToColumn`/`RowWrapping`, `TextView` (with basic HTML via the JDK parser), `TextInput`, `Button`, `IconView`, `Space`, `Separator`, `SwapView`, `CoordinatorFrame` (as a frame; sheets and panels TODO), scrolling, modifiers, overlay, `KiteUiRoot`/`kiteUiWindow`, AppState, storage, timers, screenshots.  `RenderTest` drives a real KiteUI form headlessly (type, click, reactive update) and saves a screenshot.
- A review pass fixed: wrapped text clipped in wrapping rows, exponential intrinsic cost (layouts now define intrinsics), SwapView fighting `shownWhen`, invisible elements taking input, stuck hover/press themes, `KiteUiRoot.close()`, focus requests before composition, element rectangles now in dp.  `LayoutTest` covers the first three.
- Decided while building: explicit sizes (`sizedBox`) win over a stretched slot and are centered in it (Android's behavior).  Wrapping containers size stretch axes from Compose intrinsics, since Compose measures each child once.
- Platform services done: ExternalServices, keyboard (`onUniversalKeyboard`, Canvas keys, Escape closes popovers), back from Alt+Left and the mouse back button, `inForeground` from window focus, confirm-before-leaving.  File dialogs, window close, and `inForeground` need a real window, so only the logic around them is tested headless.

- Media written.  In the agent sandbox, AVFoundation loads media (duration, errors, release are tested) but never decodes a frame or advances playback, so `MediaTest`'s playback tests skip themselves there; run them on a real machine.  The player must be opened with `InitialPlayerState.PAUSE`: its default autoplays.
  - `RawVideoView` uses ComposeMediaPlayer.  Its snapshot state is mirrored into signals with `snapshotFlow`.  macOS never calls `onPlaybackEnded`, so `completedPlay` fires when playback stops at the end.  Seeking before the duration is known throws.  `preloadHint` is ignored (as on Android).  `showControls` draws a minimal play/pause and seek bar, since the library has no controls.
  - Audio uses `javax.sound` (WAV/AIFF/AU only; other formats fail with a clear error).  `composemediaplayer-audio` was dropped: no completion event, loop fixed at play time, and its Rust `nucleus.rodio` dependency.  The sandbox has no audio device, so tests that play sound are skipped there.

## Open decisions

1. **How consumer apps add desktop.**  An app module's `commonMain` depends on `kiteui`; a desktop JVM target in the same module would resolve kiteui's `jvmSsr` variant.  So with this layout, apps need a separate desktop module that `srcDir`s their common code, like `example-app-desktop`.  The alternative is to move SSR into its own `library-ssr` module and make `:library`'s single JVM target the desktop one.  That gives apps a normal `jvm()` target, but it's a bigger restructure and affects SSR users.  Plan assumes the separate-module approach for now; revisit after phase 1.
2. **ComposeMediaPlayer** adoption, after the audit.
3. **Sandbox prerequisite.**  kiteui's wrapper needs Gradle 9.6.1, which isn't usable in the agent sandbox.  Either bump the wrapper to 9.7.1, or allow `services.gradle.org`.
