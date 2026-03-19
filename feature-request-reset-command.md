# Feature Request: Add `reset` command to AiDriver

## Summary

Add a `reset` command to the AiDriver command handler that calls `navigator.resetUrlLikePath(route)` instead of `navigateUrlLikePath(route)`. This clears the navigator stack to a single page, which is essential for reliable E2E testing.

## Problem

The AiDriver currently only exposes `navigate` (push), `back` (pop one), and `url` (read) commands. There is no way to clear the navigator stack from a test. This causes problems:

1. `navigate` always pushes onto the stack, accumulating pages in the SwapView
2. Old page RViews persist and share reactive state with new instances of the same page
3. `findAll()` returns elements from ALL stacked pages (including hidden/stale ones)
4. Interactive elements (buttons, inputs) found on stale pages may not trigger visible navigation

The `PageNavigator` already has `resetUrlLikePath()` (Navigator.kt:19) — it just needs to be exposed through the AiDriver protocol.

## Requested Change

In `AiDriver.kt`, add a `"reset"` case to `handleCommand()`:

```kotlin
"reset" -> {
    if (navigator == null) throw DriverActionException("no navigator available")
    val route = action ?: throw DriverActionException("no route specified")
    navigator.resetUrlLikePath(route) ?: throw DriverActionException("route '$route' not found")
    "OK"
}
```

Place it right after the `"navigate"` case (line ~100).

## Usage

From test code:
```kotlin
raw("reset\t/landing")  // Clears navigator stack to just LandingPage
```

## File

`library/src/commonMain/kotlin/com/lightningkite/kiteui/views/AiDriver.kt` — add case in `handleCommand()` `when(target)` block.
