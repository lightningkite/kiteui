# Changelog

## Unreleased

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
