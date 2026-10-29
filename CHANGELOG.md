# Changelog

## Unreleased

### Element context (breaking)

- **A context value set inside a container is seen by everything in it and nothing outside it.** Containers
  used to share one context with their parent, so setting a value (`context.titleDepth`, a navigator, an
  addon of your own) leaked to siblings and ancestors. Code that set a value in a container and read it
  outside must now set it where both can see it, usually further up.
- `titledSection` headings nested inside content that renders later, such as a `forEach`, now get the right
  level.
- **Popovers are objects.** One popover is open at a time, along with the popovers opened from inside it such as
  submenus; dialogs and overlays don't count. `context.currentPopover` is the open one and
  `context.enclosingPopover` the one an element is in, both `null` when there is none. A `Popover` has
  `close()`, which also closes the popovers opened from inside it, and `child`.
  - A menu item closes the whole menu with `context.currentPopover?.close()`, replacing `closePopovers()`, which
    also works from a dialog the menu opened. `closeThisPopover()` is `context.enclosingPopover?.close()`.
  - A popover now stays open while one opened from inside it is, so `keepPopoverOpen()` does nothing.
  - `popoverCloser`, `popoverParent` and `popoverKeepOpen` are gone; `closePopovers()`, `closeThisPopover()`,
    `closeSiblingPopovers()` and `keepPopoverOpen()` are deprecated.
  - Closing a popover twice now does nothing the second time.
- `ElementContext.pageNavigator`, `externalServices`, `appName`, `appNavFactory` and `ssrResourceRegistry` are global:
  one value for the whole element tree, whichever element sets it. Setting `externalServices` now needs
  `@OptIn(InternalKiteUi::class)`.
- `contextAddon`, `lazyContextAddon` and `lateInitContextAddon` take `global = true` for an addon with one value per
  element tree. In `lazyContextAddon` it comes before `init`, so an `init` passed as a value rather than a trailing
  lambda must be named: `lazyContextAddon(init = ::makeIt)`.
- Each element tree now has its own back-dismissable dialog stack and `appNavFactory`; they were shared by every
  tree in the process.
- `ElementContext.splitExceptionHandlers()` is gone; `split()` now scopes exception handlers and addons
  together.

### Telemetry (breaking)

- **Telemetry now goes through your own server.** `Telemetry` is built with an `upload` function (normally
  `telemetryUpload { api.clientTelemetry.ingest(it) }`, with lightning-server-kiteui's helper and Lightning
  Server's `ClientTelemetryEndpoints`) instead of an OTLP endpoint and headers, so no telemetry token ships in the app. **Revoke any Grafana/OTLP token that shipped in a
  client build.** The server adds `user.id` and forwards to its own backend. KiteUI now depends on
  `com.lightningkite.services:telemetry-shared` (the client-safe upload format, not `basis`).
- Records are saved to device storage and uploaded oldest first, so telemetry recorded offline or just
  before a crash is sent on a later run. The `sendBeacon` crash path is gone.
- Spans are short: requests, actions (`action.<title>`, with their requests as children) and screen loads
  (`screen.load`, ending when the screen is usable). The page-long `page: X` spans and foreground session
  span are gone. Logs and errors are labelled with the current screen rather than a guessed trace.
- **Clients record no metrics.** There is no metrics API; `http.client.request.duration`,
  `navigation.page_views`, `action.invocations` and `connectivity.issues` are gone (connectivity issues remain
  WARN logs), and the server derives no metrics from client spans. Clients are untrusted, and metric
  cardinality drives observability cost. Unsampled sessions record no spans.
- **`Telemetry` no longer takes `serviceName`.** The server names client records `<projectName>-client` and
  keeps only `service.version`, `os.type`, `session.id` and `app.debug` from the client, so a client can't
  pose as the server. Uploads are rate limited server-side; a 429 is retried later.
- `span(name) { }` is now `span(owner, operation, attributes) { }`, recording into the installed `Telemetry`
  (it just runs the block when none is installed), and throws for an owner the server would refuse.
- `Telemetry.flush()` saves and uploads now, waiting for an upload already running, and throws if the upload
  fails. `Telemetry.clear()` deletes unsent records, for when a flush at sign-out fails.
- Removed: `TelemetryConfig`, `exceptionAttributes` (use the new
  `ElementContext.telemetryAttributes`), and `ActionInstrumentation`/`actionInstrumentors` (replaced by
  `Action.Interceptor`, which wraps the action's run). `crash.fingerprint` is now `error.fingerprint`, and
  `page.name` is `app.screen.name`. See `docs/TELEMETRY.md` for setup and dashboard migration.
- **Interceptors are registered, not added to public lists.** `fetchInterceptors`, `Log.interceptors` and
  `actionInterceptors` were mutable lists anyone could `clear()`. They are now `HttpFetcher.Interceptor.global`,
  `Log.Interceptor.global` and `Action.Interceptor.global`, each a `LiveRegistry` whose `register(...)` returns a
  `Release` that removes only that interceptor. `LogInterceptor` is now `Log.Interceptor`. `HttpFetcher.Interceptor` and
  `Action.Interceptor` are `fun interface`s; wrap lambdas in
  `HttpFetcher.Interceptor { ... }` where a bare lambda was passed before.
- **`Throwable_report` is now the `ThrowableReporter.global` registry.** Assigning the old variable replaced
  whatever was there, so a second reporter silently disconnected the first. Register a `ThrowableReporter`
  instead; `Throwable.report(context)` sends to every one. Stack traces are still printed by default; call
  `ThrowableReporter.removeDefaultHandler()` to stop that.
- **Crash fingerprints changed**, so existing groups don't continue: they no longer include exception messages
  or JS column numbers, which split one crash into many groups.
- Request spans replace path segments that look like ids, emails or tokens with `{id}`, and are `Error` for
  status 400 and above. Relative URLs (same-origin requests on the web) get a `traceparent` whenever
  `tracePropagationHosts` isn't empty.
- Attributes are fitted to the server's limits before they're saved, since one record over them made the server
  refuse its whole batch. `Telemetry.install` does nothing during server-side rendering.
- **`HttpFetcher.platform` and `HttpFetcher.default` are now the objects `HttpFetcher.Platform` and
  `HttpFetcher.Default`.** `platformFetch(...)` and `platformWebSocket(...)` are gone; call
  `HttpFetcher.Platform.fetch(...)` and `HttpFetcher.Platform.webSocket(...)`. `HttpFetcher.intercepted` takes a
  `List`, and `HttpFetcher.Intercepted(fetcher, interceptors)` is the class behind it.

### Navigation

- **Showing a page no longer focuses its first field.** `navigatorView` used to request focus on the
  new page's first interactive element after every swap, which pops the soft keyboard on Android and
  iOS the moment a screen appears. Nothing focuses page content automatically now; a page that wants
  a field ready to type in calls `requestFocus` (or `requestFocusOrDescendant`) itself.

- **Screen changes are announced to assistive technology instead.** `navigatorView` now calls the new
  `Element.announceAsNewScreen(title)` on the page container after every swap, so a screen reader
  follows the navigation without keyboard focus moving. Each platform uses its own idiom: DOM focus
  on the container on web (via `tabindex="-1"`), a screen-changed notification on iOS, and an
  accessibility pane title - taken from `Page.title` - on Android.

### WebSockets

Found by the new cross-platform networking tests, all in the Ktor-backed implementations (JVM/SSR,
Android, iOS). The browser implementation was already correct, which is what the tests compared
against.

- **`close()` was silently ignored when called soon after connecting.** The close request went to a
  rendezvous channel via `trySend`, which fails unless the coroutine that owns the session happens to
  be parked on a receive at that instant — and it is not yet parked while the `onOpen` handlers run.
  Closing a socket as soon as it opened, as `retryWebSocket` does when it reconnects, did nothing.
  The channel now buffers the first request.

- **`onClose` reported code `0` instead of the real close code.** Ktor's session performs the closing
  handshake itself and never hands the `Close` frame to `incoming`, so the code was read from a frame
  that never arrived. It now comes from the session's own close reason, which is the code both peers
  settled on in either direction.

- **`send()` silently dropped messages past the tenth queued one.** The outgoing buffer held ten
  frames and `trySend` discarded the rest, so a caller sending faster than the socket drained lost
  messages without being told. The buffer is now unbounded.

- **Binary frames were sent with `fin = false`,** announcing a fragmented message whose continuation
  never came. They are now sent as the single complete frames they are.

### Testing

- **Networking tests now run on all four platforms against a local server.** `networkTest` in
  `commonTest` replaces the per-platform harnesses; `:test-server` provides the HTTP and WebSocket
  endpoints and is started and stopped by the build. See
  [docs/TESTING_GUIDE.md](docs/TESTING_GUIDE.md#networking-tests).

### AI Driver / Test Utilities

- **`find` and `findClickable` now filter hidden views by default.** Previously, both functions walked the entire view tree including pages hidden in `SwapView`/navigator stacks, causing tests to interact with stale off-screen views. Pass `includeHidden = true` (or the `--hidden` flag in raw commands) to restore the old behavior.

- **`findAll()` paths now work directly with `click()`, `setValue()`, etc.** Previously, paths returned from `findAll()` on an anonymous (un-named) root had an extra `0/` prefix that misaligned with `resolveDriverPath`, requiring a workaround. Path construction now starts from the root's children so returned paths can be passed directly to any driver command.

- **Added `reset` driver command.** Resets the navigator stack to a single page at the specified route, giving each test a clean known starting state. Available as `navigator.reset(route)` in `UiTestScope` and as the `reset` target in raw driver commands.

- **Added `startRoute` parameter to `remoteUiTest()`.** Automatically calls `reset(startRoute)` before the test block when provided, so remote tests always start from a known page without manually calling `reset()`.

- **`UiTestScope.find`, `findAll`, `findClickable`, `findWithAction`** each accept a new `includeHidden: Boolean = false` parameter.
