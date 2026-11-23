# Platform notes

The same `commonMain` UI runs everywhere, rendered with each platform's own views.  This chapter covers what still differs: entry points, units, insets, back navigation, server-side rendering, and how to drop down to a native view when KiteUI doesn't have the one you need.

| Platform | Artifact | Renders with | Entry point |
|---|---|---|---|
| Web | `library` | DOM elements and generated CSS | `smartRoot(theme) { }` / `root(theme) { }` |
| Android | `library` | Android `View`s | `KiteUiActivity` |
| iOS | `library` | `UIView`s | `UIViewController.setup(theme) { }` |
| Desktop (JVM) | `library-compose` | Compose Multiplatform | `kiteUiWindow(...)`, `kiteUiHeadless(...)` |
| Server-side rendering (JVM) | `library` (`jvmSsr` target) | HTML strings | `SsrRouter` |

Entry-point code for each is in [Getting started](getting-started.md#start-the-app-on-each-platform).

## Minimal example

```kotlin
if (Platform.current == Platform.iOS) text("Hello, iPhone")
```

`Platform.current` is one of `iOS`, `Android`, `Web`, `Desktop`.  Also available: `Platform.isDevelopment` (debug build or localhost), `Platform.probablyAppleUser` (for ⌘ vs Ctrl labels), `Platform.usesTouchscreen`.  For anything bigger than a branch, use `expect`/`actual` in your app.

## Common tasks

### Embed a native view

Declare an element in common code and implement it once per target by extending `NativeElement`.

```kotlin
// commonMain
expect class Gauge(context: ElementContext) : NativeElement {
    var ratio: Float
}

inline fun ElementWriter.gauge(setup: Gauge.() -> Unit = {}): Gauge = write(Gauge(context), setup)
```

```kotlin
// androidMain
actual class Gauge actual constructor(context: ElementContext) : NativeElement(context) {
    override val native = android.widget.ProgressBar(context.activity, null, android.R.attr.progressBarStyleHorizontal)
    actual var ratio: Float = 0f
        set(value) { field = value; native.progress = (value * 100).toInt() }
}
```

```kotlin
// jsMain (and jvmSsr, which shares the web element model)
actual class Gauge actual constructor(context: ElementContext) : NativeElement(context) {
    init {
        native.tag = "progress"
        native.setAttribute("max", "1")
    }
    actual var ratio: Float = 0f
        set(value) { field = value; native.setAttribute("value", value.toString()) }
}
```

```kotlin
// iosMain
actual class Gauge actual constructor(context: ElementContext) : NativeElement(context) {
    override val native = platform.UIKit.UIProgressView()
    actual var ratio: Float = 0f
        set(value) { field = value; native.setProgress(value, animated = false) }
}
```

Then use it like any other element: `expanding.gauge { ::ratio { progress() } }`.

- Android: `native` is any `View`; `context.activity` is the `KiteUiActivity`.
- iOS: `native` is any `UIView`; `context.controller` is the `UIViewController`.
- Web: `native` is a `FutureElement`, a lightweight stand-in that becomes a DOM node when attached (and an HTML string under SSR).  Set `tag`, `classes`, `style`, and attributes on it; use `native.onElement { domElement -> }` when you need the real DOM node.
- Desktop: override `@Composable Content(modifier)` instead of `native`.
- `expect`/`actual` classes need the `-Xexpect-actual-classes` compiler flag.
- Every target the module compiles for needs an `actual`.  [`library-map`](../library-map/src/) is a complete example.

### Respect safe areas

KiteUI publishes the system bar, notch, and keyboard insets as `context.safeInsets`.  The built-in app shells apply them.  For a custom full-screen layout, call `applySafeInsets()` on the element that touches the edge.

```kotlin
col {
    applySafeInsets(bottom = false)
    h1("Edge to edge")
}
```

### Handle back

On the web, back is the browser's history.  On Android, `KiteUiActivity` routes the system back gesture to `mainNavigator.goBack()` and closes the activity when the stack is empty.  On desktop, closing the window asks the current page first, as `beforeunload` does on the web.  See [Navigation](navigation.md) for blocking back on unsaved changes.

### Render on the server

The `jvmSsr` target of `library` renders pages to HTML on the JVM; `smartRoot` on the web hydrates that HTML instead of rebuilding it.  This is experimental.

```kotlin
val router = SsrRouter(routes = AutoRoutes, theme = appTheme, appWrapper = { navigator -> app(navigator) })
val html: String = router.renderOrFallbackWithPreload("/docs/getting-started")
```

Use `ssrResource("key") { load() }` for data that should load on the server and be embedded in the page.  See `SsrServer.kt` and `SsrPrerender.kt` in [`example-app/src/jvmSsrMain`](../example-app/src/jvmSsrMain/kotlin/com/lightningkite/mppexampleapp/).

### Drive or screenshot a desktop app with no display

`kiteUiHeadless(appId, theme = { appTheme }) { app(navigator) }` runs the app offscreen at 1280×800, for CI and the [AI driver](ai-driver.md).  `KiteUiRoot.current?.screenshot()` captures the last frame.

## Element lifecycle

- An element is created, configured by its setup block, started (`onStartup`), and attached.  Bindings and `launch`es inside it start then.
- Removing an element shuts it down: its coroutines are cancelled and its reactive bindings stop.  You never call `onStartup` or `onShutdown` yourself.
- A page's `render()` runs once per page instance; navigating away removes its views.  A `@Routable object` page keeps fields across visits because the object is a singleton.
- Elements must be created and changed on the main thread.  `launch` inside UI code already runs there.

## Gotchas

- `.px` means a CSS pixel on the web (already density-independent) but a physical pixel on Android and iOS.  Use `rem` or `dp` for sizes and breakpoints.
- `rem` is the root font size on the web and `UIFont.systemFontSize × remMultiplier` on iOS; set `remMultiplier` before `setup` to scale an iOS app.
- On Android, the navigation stack is saved as URLs and rebuilt after process death.  Page state that isn't in the URL (constructor parameters or `@QueryParameter`s) is lost then.
- Keep `<base href="/">` in the web `index.html`, as the example app does, so relative asset URLs still resolve on deep links.
- On iOS, the plugin writes resources into the Xcode project at `iosProjectRoot`: images into `Assets.xcassets`, other files into `resourcesFromCommon/`, fonts into `Info.plist`.  `resourcesFromCommon/` must be part of the app target.
- `library-compose` compiles the common sources of `library` itself, so a desktop module depends on `library-compose` instead of `library`, never both.

## See also

- [Getting started](getting-started.md): entry points and Gradle setup
- [Testing](testing.md): running UI tests per platform
- [Telemetry](telemetry.md) and [AI driver](ai-driver.md)
