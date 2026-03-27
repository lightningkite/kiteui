# Changelog

## Unreleased

### AI Driver / Test Utilities

- **`find` and `findClickable` now filter hidden views by default.** Previously, both functions walked the entire view tree including pages hidden in `SwapView`/navigator stacks, causing tests to interact with stale off-screen views. Pass `includeHidden = true` (or the `--hidden` flag in raw commands) to restore the old behavior.

- **`findAll()` paths now work directly with `click()`, `setValue()`, etc.** Previously, paths returned from `findAll()` on an anonymous (un-named) root had an extra `0/` prefix that misaligned with `resolveDriverPath`, requiring a workaround. Path construction now starts from the root's children so returned paths can be passed directly to any driver command.

- **Added `reset` driver command.** Resets the navigator stack to a single page at the specified route, giving each test a clean known starting state. Available as `navigator.reset(route)` in `UiTestScope` and as the `reset` target in raw driver commands.

- **Added `startRoute` parameter to `remoteUiTest()`.** Automatically calls `reset(startRoute)` before the test block when provided, so remote tests always start from a known page without manually calling `reset()`.

- **`UiTestScope.find`, `findAll`, `findClickable`, `findWithAction`** each accept a new `includeHidden: Boolean = false` parameter.
