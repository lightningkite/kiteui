# Error handling

Code that runs for a view (a button's action, a reactive binding, a coroutine launched from a view) can just throw.  The exception walks up the view tree to the nearest **exception handler** that accepts it, which decides how to show it.  An **exception-to-message** converter turns it into a title and body a person can read.  You install both once at the app root and override them for parts of the tree.

## Minimal example

```kotlin
fun ViewWriter.app(navigator: PageNavigator) {
    context.exceptionHandlers.installSmartHandlers()   // install once, at the root
    appNav(navigator) { /* ... */ }
}

// anywhere below it
important.button {
    text("Save")
    onClick {
        if (name().isBlank()) throw PlainTextException("Enter a name first.", title = "Missing name")
        saveProfile(name())
    }
}
```

While `onClick` runs, the button shows its working state and ignores repeat taps.  If it throws, the user sees a dialog titled "Missing name".

**Nothing is shown unless a handler is installed.**  KiteUI installs none by default; an unhandled exception is only logged.

## Common tasks

### Choose the root handlers

| Call | Shows |
|---|---|
| `installStandardHandlers()` | A dialog with the message; `"An unexpected error occurred."` for anything without a converter.  For production. |
| `installDebugHandlers()` | The same dialog, plus the exception, its cause, the stack trace, and Copy/Export buttons for a report. |
| `installSmartHandlers()` | Debug handlers when `Platform.isDevelopment`, standard ones otherwise. |

All three include the converter for `PlainTextException`.  If several errors arrive while the dialog is open, they join it rather than stacking dialogs.

### Show a user-facing message

```kotlin
throw PlainTextException(
    "Couldn't reach the server.",
    title = "Offline",
    actions = listOf(Action("Retry") { sync() }),
)
```

`PlainTextException(message, title = "Error", actions = emptyList())` is shown as-is.  Each action becomes a button in the dialog.  Subclass it (it is `open`) for your own user-facing errors.

### Use an `Action` instead of `onClick`

```kotlin
val save = Action("Save", Icon.done) {
    saveProfile(name())
    context.toast("Saved")
    context.pageNavigator.goBack()
}
important.button {
    text("Save")
    action = save
}
```

`onClick { }` is shorthand for `action = Action(...)`.  Build the `Action` yourself to share it between elements (a button and a text field's keyboard action), give it a title and icon, or change its options:

| Parameter | Default | Effect |
|---|---|---|
| `frequencyCap` | `500.milliseconds` | Taps closer together than this are dropped.  `null` disables. |
| `ignoreRetryWhileRunning` | `true` | Starting it while it runs does nothing. |
| `clearErrorOnDependencyChange` | `true` | The error state clears when a reactive value the action read changes (the user edits the form).  `false` keeps it until the next run. |
| `keepRunningWhile` | `AppScope` | The action keeps running after its view is removed (the user navigated away).  Pass a narrower scope to cancel it instead. |

`a + b` runs both actions concurrently and fails if either fails.

### Turn an exception type into a message

```kotlin
col {
    context.exceptionHandlers += ExceptionToMessage<OfflineException> {
        ExceptionMessage(title = "Offline", body = "Check your connection and try again.")
    }
    button { text("Load"); onClick { api.load() } }
}
```

The converter applies to everything inside the `col`.  Add it to the root context to apply it app-wide.  With Lightning Server, `context.exceptionHandlers.installLsError()` (from `lightning-server-kiteui`) adds one for `LsErrorException` that shows the server's message, or a sentence for the HTTP status.

### Change how errors are shown

```kotlin
context.exceptionHandlers += ExceptionHandler(priority = 1f) { e ->
    toast(exceptionMessage(e)?.body ?: "Something went wrong")
    return@ExceptionHandler {}
}

context.exceptionHandlers += ExceptionHandler<StaleSessionException> {
    pageNavigator.reset(LoginPage)
    return@ExceptionHandler {}
}
```

A handler runs with the `ElementContext` where the error happened as its receiver.  It returns a cleanup function (`{}` if there is nothing to undo) to claim the error; KiteUI calls it when the error clears, for example when a retry succeeds.  Returning `null` passes the error on.

Order: handlers on the nearest container are tried before its parent's.  Within one container, handlers for a specific type (`ExceptionHandler<T>`) come before general ones, then higher `priority` first.  The same rules apply to `ExceptionToMessage`; `exceptionMessage(e)` asks the converters for a message.

### Show errors inline

```kotlin
col {
    textInput { content bind code }
    button { text("Submit"); onClick { redeem(code()) } }
    errorText()          // errors from anything in this col appear here instead of in a dialog
}

field("Email") {         // field() already includes errorText()
    textInput { content bind email; keyboardHints = KeyboardHints.email }
}
```

`errorText()` claims every error raised inside its container and shows each message's body under it.  It does not see errors from outside its container.  For per-field validation, see [Forms](forms.md).

### Catch an error yourself

```kotlin
onClick {
    try {
        api.load()
    } catch (e: OfflineException) {
        context.toast("Offline; showing saved data")
    }
}
```

Catch only what you can recover from, and let the rest propagate to the handlers.

## Loading and working states

Every element tracks two kinds of work and themes itself to match:

- **Background:** reactive bindings such as `::content { }`.  Until the value arrives, the element gets `LoadingSemantic`: a fading background and faded foreground.
- **Foreground:** the element's `action`.  While it runs, the element gets `WorkingSemantic`: a half-transparent foreground.

Both report failures to the exception handlers, so a binding that throws shows an error just like a failed button press.  The element keeps the loading or working look until the error clears.  An exception thrown from a coroutine started with `launch { }` in a view's scope also reaches the handlers.

## Gotchas

- **No handlers, no dialog.**  Call `installSmartHandlers()` (or another `install...`) on the root `context.exceptionHandlers`.  Converters alone (`installLsError()`) produce messages but display nothing.
- **Background errors reach the same handlers.**  A binding that fails to load data opens the error dialog too.  A handler can tell the kinds apart with the two-argument form, `ExceptionHandler { e, metadata -> ... }`, whose `metadata?.foregroundProcess` is `true` for actions and `false` for bindings.
- **Handlers belong to containers.**  `context.exceptionHandlers += ...` inside `col { }` covers that column's subtree only.  There is no `+=` on the element itself.
  ```kotlin
  col { this += ExceptionToMessage<X> { ... } }                    // ❌ does not compile
  col { context.exceptionHandlers += ExceptionToMessage<X> { ... } } // ✅
  ```
- **Claim what you show.**  The first handler to return non-null wins.  A handler that shows something and returns `null` lets the next one show it again.
- **Don't swallow `CancellationException`.**  Inside actions, catch specific exception types rather than `Exception`; cancellation is how a restarted or scoped action stops.
- **Actions outlive their view by default.**  `keepRunningWhile = AppScope` means a save finishes after the user leaves the page, and its `context.toast(...)` still appears.

## See also

- [Forms](forms.md): validation messages on individual fields
- [Dialogs](dialogs.md): `toast`, `dialog`, confirmations
- [Server data with ModelCache](model-cache.md): `installLsError()` and server errors
