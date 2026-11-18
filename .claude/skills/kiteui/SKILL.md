---
name: kiteui
description: This skill should be used when you are using KiteUI.
version: 4.0.0
---

# KiteUI

Kotlin Multiplatform UI framework: native views per platform, fine-grained reactivity in the style of Solid.js.  The human docs are in [`docs/`](../../../docs/README.md); this skill is the condensed version.  If the two disagree, the source wins, then docs/.

**`render()` runs once, not on every change (unlike React).**  Only bindings (`::content { }`, `remember { }`, `shownWhen { }`, `reactive { }`) rerun, and only when a value they read changes.

## Sub-files: read the relevant one before non-trivial work

| Topic | File |
|---|---|
| Containers, modifiers, sizing, scrolling | [layout.md](layout.md) |
| Text, buttons, inputs, icons, images | [components.md](components.md) |
| Signal/remember/bindings, lists (`colOf`, `recyclerView`) | [reactivity.md](reactivity.md) |
| Labeled fields, validation | [forms.md](forms.md) |
| Pages, `@Routable`, navigator, app shells | [navigation.md](navigation.md) |
| Toast, dialogs, bottom sheets, popovers | [dialogs.md](dialogs.md) |
| Semantic themes, built-in and custom themes | [theming.md](theming.md) |
| Lightning Server data (ModelCache) | [model-cache.md](model-cache.md) |
| Action, exception handlers | [error-handling.md](error-handling.md) |
| UI tests, AI driver | [testing.md](testing.md) |
| Keyboard shortcuts, drag and drop, misc | [advanced-patterns.md](advanced-patterns.md) |

## A page

```kotlin
@Routable("users/{userId}")
class UserPage(val userId: String) : Page {
    override fun ElementWriter.CanAddTheme.render() {
        val user = rememberSuspending { api.user(userId) }      // views reading it show loading automatically
        val name = Signal("")
        col {
            h1 { ::content { user().name } }
            field("Name") {
                textInput { hint = "Name"; keyboardHints = KeyboardHints.title; content bind name }
            }
            important.button {
                text("Save")
                onClick { api.rename(userId, name()); context.toast("Saved") }  // suspend; errors shown automatically
            }
        }
    }
}
```

The app root must be wrapped in `appBase(navigator) { navigatorView(navigator) }`, `appNav`, or a `nav*` shell, and should call `context.exceptionHandlers.installSmartHandlers()`.  Without the shell, toasts and dialogs throw; without handlers, errors are only logged.  See [navigation.md](navigation.md) and [error-handling.md](error-handling.md).

## Pitfalls

### Modifier order is enforced by the compiler

`alignment → weight → shownWhen → sizing → theme → scrolling → element`

```kotlin
expanding.card.scrolling.col { }                 // ✅
expanding.scrolling.card.col { }                 // ❌ theme after scrolling
expanding.centered.text("x")                     // ❌ alignment after weight → expanding.col { centered.text("x") }
shownWhen { a() }.expanding.col { }              // ❌ → expanding.shownWhen { a() }.col { }
```

`render()`'s receiver is `ElementWriter.CanAddTheme`, so at its top level only theme and scrolling modifiers exist.  Put alignment, `expanding`, `shownWhen` and `sizeConstraints` inside a container: `render() { col { expanding.recyclerView { } } }`.

The KiteUI 6 dash syntax (`expanding - col { }`) is gone.

### `context` comes from the enclosing view, in `onClick` and `Action` alike

Neither lambda has a view receiver (`onClick: suspend () -> Unit`, `Action: suspend CoroutineScope.() -> Unit`), but `context` from the surrounding builder is in lexical scope.  Use `context.toast(...)`, `context.pageNavigator`, `context.dialog { }`, `context.confirmDanger(...)`; the receiver-less `toast(...)`, `pageNavigator`, `dialog {}` are deprecated.

### `reactive { }` that builds views duplicates them

```kotlin
reactive { text(user().name) }                                   // ❌ adds a view per rerun
text { ::content { user().name } }                               // ✅ bind a property
shownWhen { advanced() }.card.col { /* ... */ }                  // ✅ toggle visibility
swapping(current = { mode() }) { m -> /* build for m */ }        // ✅ rebuild on change
colOf(items, id = { it.id }) { item -> text { ::content { item().name } } }   // ✅ lists
col { reactive { clearChildren(); items().forEach { text(it.name) } } }       // last resort: isolate in its own col (clearChildren clears siblings)
```

### Lists: `colOf` / `recyclerView`, not `forEach`

`forEach`, `forEachById`, `forEachUpdating` are deprecated and internal.  Use `colOf(items, id) { r -> }` for short keyed lists, `colOfExpensive(items) { t -> }` to rebuild on each change, `expanding.recyclerView { children(items, id) { r -> } }` for long ones (virtualized, scrolls itself, needs a bounded size), and `windowedList` for chats and feeds.

### Derived values: `remember`, not a second Signal

```kotlin
val fullName = remember { "${first()} ${last()}" }                 // ✅ recomputes, propagates loading/errors
val details = rememberSuspending { api.details(userId()) }        // ✅ async; not Signal + launch
```

### Reading a reactive needs a reactive or suspend context

`x()` works in `::prop { }`, `remember`, `reactive`, `shownWhen { }`, and suspend lambdas (`onClick`, `Action`, `launch`).  Render-time lambdas (a list's `render`, `col { }`) are neither: read inside a binding.  Use labeled returns: `?: return@reactive`, `?: return@remember`.

### Buttons have no text property

`button { text("Save"); onClick { } }`, or `icon(Icon.send, "Send")` inside.  Same for `link { }`.

### Helpers take `ElementWriter.CanAddTheme`

```kotlin
private fun ElementWriter.CanAddTheme.userCard(user: Reactive<User>) = card.col { text { ::content { user().name } } }
```

Plain `ElementWriter` has no theme modifiers.  Callers can prefix: `expanding.userCard(u)`.

### Themes go on containers

Semantics (`card`, `important`, `danger`, ...) draw a background and padding.  Write `card.col { text("A"); text("B") }`, not `card.text("A"); card.text("B")` (two cards).

### `expanding` needs an expanding chain

`expanding` fills its parent only.  A page whose list should fill the screen needs `col { header(); expanding.recyclerView { } }`, and every ancestor that should stretch must itself be `expanding`.

### Tabs: pages or `swapping`, not parallel `shownWhen`

Each `shownWhen` branch stays alive, and on the web its wrapper can stop `expanding` children from stretching.  Make each tab a `@Routable` page (state on an `object` page survives switching) or use `swapping`.

### Inputs

- Always set `keyboardHints` (`KeyboardHints.email`, `.password`, `.title`, `.paragraph`, `.integer`, `.decimal`, `.phone`, `.newPassword`, `.oneTimeCode`, ...; there is no `url`).
- `numberInput.content` is `Double?`: bind a `Signal<Double?>`, or `intSignal.asDouble()`.
- `select { bind(value, Constant(options)) { it.label } }`: the options must be a `Reactive<List<T>>`.
- `field("Label") { }` applies `fieldTheme`, wires the label, and shows validation errors.  Modifiers go outside: `sizeConstraints(width = 20.rem).field("Email") { }`.
- `debounce(400.milliseconds)`, only inside a view scope; the `Long` overload is deprecated.

### `PersistentProperty` stores JSON

`PersistentProperty<String?>("token", null)` stores `"\"abc\""`, not `abc`.  Test harnesses injecting localStorage must write the JSON form.

## Imports

Star imports cover nearly everything:

```kotlin
import com.lightningkite.kiteui.*
import com.lightningkite.kiteui.models.*          // Icon, KeyboardHints, rem, Align, Theme, semantics
import com.lightningkite.kiteui.navigation.*
import com.lightningkite.kiteui.reactive.*        // Action, PersistentProperty, AppState
import com.lightningkite.kiteui.exceptions.*      // PlainTextException, ExceptionHandler
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.l2.*        // field, toast, appBase, nav shells
import com.lightningkite.reactive.context.*       // invoke (x()), reactive { }, bind
import com.lightningkite.reactive.core.*          // Signal, Reactive, remember, Constant
import com.lightningkite.reactive.extensions.*    // lenses, debounce, withWrite
import com.lightningkite.reactive.lensing.*
import com.lightningkite.reactive.lensing.validation.*
import kotlinx.coroutines.*
```

Not covered: `com.lightningkite.kiteui.locale.renderToString` (dates for display), and the generated `AutoRoutes`/`Resources` in your `kiteui { packageName }`.  If many valid-looking symbols are unresolved inside `onClick { }`, `com.lightningkite.reactive.context.*` is missing.

## Lightning Server

- Read and write models through ModelCache (`session.todos.item(id)`, `.list(query)`, `.add`, `.modify`).  Call `session.api.x.y()` directly only for non-CRUD endpoints and `count`.  See [model-cache.md](model-cache.md).
- Filter on the server: `Query(limit = 50) { it.name.contains(q(), ignoreCase = true) }`, not `remember { all().filter { } }`.
- Service Abstractions 1.4+: fields are paths (`Todo.title`), builders are `Condition<T> { }` / `Modification<T> { }`, and "no filter" is `Condition.Always`.  `Model_field` and `Model.path` are gone.
- `context.exceptionHandlers.installLsError()` at the root turns server errors into readable messages.
- `./gradlew :server:generateSdk` regenerates the client SDK (`Api`, `CachedApi`) from the server definition.

## Resources

- Example app, every component live with source: https://kiteui.cs.lightningkite.com/ (code in `example-app/`)
- Maven: https://lightningkite-maven.s3.us-west-2.amazonaws.com
