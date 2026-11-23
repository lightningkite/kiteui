# KiteUI Error Handling

Full chapter: `docs/error-handling.md`.  Package: `com.lightningkite.kiteui.exceptions`.

Exceptions thrown from an element's `action` / `onClick`, from reactive bindings (`::content { }`), or from `launch { }` in a view's scope walk up the element tree's `context.exceptionHandlers` until one claims them.

## Install handlers at the root (nothing is installed by default)

```kotlin
fun ViewWriter.app(navigator: PageNavigator) {
    context.exceptionHandlers.installSmartHandlers()   // debug dialog in development, plain dialog in production
    context.exceptionHandlers.installLsError()         // lightning-server-kiteui: LsErrorException → message
    appNav(navigator) { ... }
}
```

Without an `ExceptionHandler` (e.g. only `installLsError()`), errors are logged and **nothing is shown**.  `installStandardHandlers()` / `installDebugHandlers()` choose explicitly.

## Throw user-facing errors

```kotlin
onClick {
    if (name().isBlank()) throw PlainTextException("Enter a name first.", title = "Missing name")
    api.save(name())
}
throw PlainTextException("Couldn't reach the server.", "Offline", actions = listOf(Action("Retry") { sync() }))
```

`import com.lightningkite.kiteui.exceptions.PlainTextException`.  It is `open`; subclass it.

## Action

`onClick { }` = `action = Action(label, icon) { }`.  `Action(title, icon = Icon.send, clearErrorOnDependencyChange = true, keepRunningWhile = AppScope, frequencyCap = 500.milliseconds, ignoreRetryWhileRunning = true) { /* suspend CoroutineScope.() */ }` (`com.lightningkite.kiteui.reactive.Action`).

```kotlin
val save = Action("Save", Icon.done) {
    api.save(name())
    context.toast("Saved")                 // `context` resolves to the enclosing ElementWriter/element
    context.pageNavigator.goBack()
}
important.button { text("Save"); action = save }
```

The lambda's own receiver is `CoroutineScope`, but the enclosing render scope's `context` is in lexical scope, so `context.toast(...)` and `context.pageNavigator` work.  Only an `Action` defined outside any element scope (top-level, in a ViewModel) lacks `context`.

While running: element gets `WorkingSemantic` and repeat taps are ignored.  `a + b` runs both concurrently.

## Custom messages and handlers (scoped to a container)

```kotlin
col {
    context.exceptionHandlers += ExceptionToMessage<OfflineException> {
        ExceptionMessage(title = "Offline", body = "Check your connection.")
    }
    context.exceptionHandlers += ExceptionHandler(priority = 1f) { e ->     // receiver: ElementContext
        toast(exceptionMessage(e)?.body ?: "Something went wrong")
        return@ExceptionHandler {}                                        // claim it; cleanup runs when error clears
    }
    context.exceptionHandlers += ExceptionHandler<SessionExpired> { pageNavigator.reset(LoginPage); {} }
}
```

- ❌ `this += ExceptionToMessage...` on an element doesn't compile; use `context.exceptionHandlers +=`.
- Return non-null (`{}`) to claim; `null` passes on.  Order: nearest container first, then specific type (`<T>`) before generic, then higher priority.
- Two-arg form `ExceptionHandler { e, metadata -> }`: `metadata?.foregroundProcess` is `true` for actions, `false` for bindings.  Background (loading) errors reach handlers too.

## Inline errors

```kotlin
col {
    textInput { content bind code }
    button { text("Submit"); onClick { redeem(code()) } }
    errorText()                        // com.lightningkite.kiteui.views.l2; claims errors from this container
}
field("Email") { textInput { ... } }   // field() includes errorText()
```

## Loading / working states (automatic)

- Binding waiting on data → element gets `LoadingSemantic` (fading background).
- Action running → `WorkingSemantic` (half-transparent foreground).
- When the work fails, the look stays until the error clears.

## Catch only what you can recover from

```kotlin
onClick {
    try { api.load() } catch (e: OfflineException) { context.toast("Offline; showing saved data") }
}
```

Don't catch `Exception` broadly (it swallows `CancellationException`).
