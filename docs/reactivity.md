# Reactivity

KiteUI's state comes from Lightning Kite's Reactive library (`com.lightningkite.reactive.*`).  A page's `render()` runs **once** and builds the views.  After that, only the bindings that read a changed value rerun.  There is no re-render and no diffing, so `launch` and other side effects in `render()` run once, as you would expect.

## Minimal example

```kotlin
object CounterPage : Page {
    override fun ElementWriter.CanAddTheme.render() {
        val count = Signal(0)
        col {
            text { ::content { "Count: ${count()}" } }  // reruns when count changes
            button {
                text("Add one")
                onClick { count.value++ }
            }
        }
    }
}
```

`count()` reads the value *and* subscribes the surrounding block to it.  `::content { ... }` is a binding: it sets the text's `content` now and again whenever anything it read changes.

## The types

| Type | What it is | Create with |
|---|---|---|
| `Reactive<T>` | A value you can watch.  It may be loading or failed. | `remember { }`, `rememberSuspending { }` |
| `MutableReactive<T>` | A `Reactive<T>` you can `set(value)` (suspending). | lenses, `withWrite { }` |
| `Signal<T>` | A plain mutable value; always ready.  Read or write `.value` anywhere. | `Signal(initial)` |
| `LateInitSignal<T>` | Loading until first set; `unset()` makes it load again. | `LateInitSignal<T>()` |
| `Constant<T>` | A value that never changes, for APIs that want a `Reactive`. | `Constant(value)` |

Reading with `()` works inside a reactive block (`::prop { }`, `remember { }`, `reactive { }`) and inside any suspend function (`onClick { }`, `Action { }`, `launch { }`).  Outside both, use `.value` on a `Signal`, or `state` on any `Reactive`.

## Common tasks

### Derive a value

```kotlin
val firstName = Signal("Ada")
val lastName = Signal("Lovelace")
val fullName = remember { "${firstName()} ${lastName()}" }

text { ::content { fullName() } }
```

`remember` recalculates when what it reads changes, shares its result among all readers, and only runs while something is listening.  Don't keep a derived value in a second `Signal` and sync it by hand.

### Load data

```kotlin
val userId = Signal(1)
val user = rememberSuspending { api.fetchUser(userId()) }

text { ::content { user().name } }
```

`rememberSuspending` takes a suspending block.  Until it finishes, `user` is loading, and every view bound to it shows a loading state on its own.  If it throws, the error flows to the views that read it (see [error handling](error-handling.md)).  When `userId` changes, it reloads.  Pass `useLastWhileLoading = true` to keep showing the old value during a reload.

Prefer this to `Signal` + `launch`, which shows no loading state and doesn't reload.

### Bind a property

```kotlin
button {
    ::enabled { agreed() && email().isNotBlank() }   // calculated
    text("Continue")
}
text { ::content bind email }                       // straight from a Reactive
```

Any `var` on an element can be bound.  `::prop { }` calculates; `::prop bind reactive` copies.

### Two-way binding

```kotlin
val email = Signal("")
textInput {
    hint = "Email"
    keyboardHints = KeyboardHints.email
    content bind email
}
checkbox { checked bind agreed }
```

`a bind b` keeps two `MutableReactive<T>` of the same type in sync, starting from `b`.

### Lenses: bind to part of a value

```kotlin
val size = Signal(Size.Small)
radioButton { checked bind size.equalTo(Size.Small) }

val tags = Signal(setOf<String>())
checkbox { checked bind (tags contains "featured") }

val nickname = Signal<String?>(null)
textInput { content bind nickname.nullToBlank() }

val age = Signal<Int?>(null)
numberInput { content bind age.asDouble() }   // numberInput holds a Double?

val user = Signal(User(first = "Ada"))
textInput { content bind user.lens(get = { it.first }, modify = { u, v -> u.copy(first = v) }) }
```

All of these are in `com.lightningkite.reactive.extensions`.  Also there: `notNull(default)`, `asInt()`/`asLong()`/`asDouble()` (from a `String`), `asIntHex()` and friends, `withWrite { }` (make any `Reactive` writable by supplying the write), `toggle()`, `modify { }`.

### Show or hide

```kotlin
switch { checked bind showAdvanced }
shownWhen { showAdvanced() }.card.col {
    text("Advanced settings")
}
```

The views exist the whole time; `shownWhen` only toggles visibility, with an optional `transition`.

### Swap between different views

```kotlin
swapping(current = { signedIn() }) { isSignedIn ->
    if (isSignedIn) text("Welcome back")
    else button { text("Sign in"); onClick { signedIn.value = true } }
}
```

Each time `current` changes, the old views are thrown away and new ones built, with a fade (pass `transition = { ScreenTransition.Push }` for another).  Use it when the alternatives are different views rather than the same view with different data.  To swap whole screens, use [navigation](navigation.md) instead.

### Debounce input

```kotlin
val query = Signal("")
val debounced = query.debounce(400.milliseconds)
val results = rememberSuspending { api.search(debounced()) }

textInput { content bind query }
colOf(results, id = { it.id }) { result -> text { ::content { result().title } } }
```

`debounce(duration)` is available inside any view (it uses the view's lifetime).  `tailThrottle` and `fullThrottle` work the same way.

### Run something once

```kotlin
val user = LateInitSignal<User>()
launch { user.value = api.fetchUser(id) }
text { ::content { user().name } }
```

`render()` runs once, so a `launch` in it runs once.  The coroutine is cancelled when the view goes away.  `load { }` is the same as `launch` but also puts the surrounding view into its loading state while it runs.

### Run something on every change

```kotlin
reactive {
    analytics.track("tab", tab())
}
```

`reactive { }` reruns its block whenever anything it read changes.  Use it for side effects, not for building views (see Gotchas).

### Persist a setting

```kotlin
val darkMode = PersistentProperty("darkMode", false)
switch { checked bind darkMode }
```

`PersistentProperty` (`com.lightningkite.kiteui.reactive`) is a `Signal` saved to platform storage (`localStorage`, `SharedPreferences`, `NSUserDefaults`) under its key, as JSON.  Keep it in a top-level `val` so there is one per key.

### React to the app and window

```kotlin
text { ::content { "Width: ${AppState.windowInfo().width}" } }
```

`AppState` (`com.lightningkite.kiteui.reactive`) exposes `windowInfo`, `inForeground`, `softInputOpen`, `systemLanguages` and `animationFrame`.

### Show a loading indicator for a section

```kotlin
val section = withLoadTracking().col { /* content that loads */ }
shownWhen { section.context.loadTracker.loading() }.activityIndicator()
```

Views show their own loading state already.  Use this when you want one indicator for a whole region.

## Gotchas

- **`reactive { }` that builds views adds them again on every rerun.**  Bind properties, use `shownWhen`, `swapping`, or a [list](lists.md) instead.  If you truly must rebuild, clear first, inside a container of its own, because `clearChildren()` clears the whole container, siblings included:

  ```kotlin
  // ❌ Adds another text on every change, and would also clear the h2
  col {
      h2("Items")
      reactive { for (item in items()) text(item) }
  }

  // ✅
  col {
      h2("Items")
      col {
          reactive {
              clearChildren()
              for (item in items()) text(item)
          }
      }
  }
  ```

  `colOfExpensive(items) { item -> ... }` does exactly this for you.
- **Use labeled returns.**  `reactive`, `remember` and `launch` take lambdas: write `?: return@remember null`, not `?: return`.
- **`Signal.value` doesn't subscribe.**  Inside `::content { }`, `count.value` reads once and never updates; use `count()`.
- **Sizing and weight go before `shownWhen`.**  `expanding.shownWhen { }.col { }` compiles; `shownWhen { }.expanding` does not, because modifier order is enforced: alignment → weight → `shownWhen` → sizing → theme → scrolling → element.
- **`shownWhen` needs a container.**  At the top of `render()` the receiver is `ElementWriter.CanAddTheme`, which comes after `shownWhen` in that order.  Put it inside a `col { }` or `row { }`.
- **Don't build tabs out of parallel `shownWhen`s.**  Every tab stays alive and bound, and on the web each `shownWhen` adds a wrapper element that can stop an `expanding` child from stretching.  Make each tab a page, or use `swapping`.
- **`remember` is lazy.**  With nothing reading it, it doesn't calculate, and its `state` reports not-active.  Read it with `()` somewhere, or `await()` it in a coroutine.
- **Names from before KiteUI 7** still compile with deprecation warnings: `Property` → `Signal`, `shared` → `remember`, `LateInitProperty` → `LateInitSignal`, `LazyProperty` → `MutableRemember`, `Readable`/`Writable` → `Reactive`/`MutableReactive`, `debounce(500)` → `debounce(500.milliseconds)`.

## See also

- [Lists](lists.md): rendering a `Reactive<List<T>>`
- [Forms](forms.md): `Draft` and validation
- [Model cache](model-cache.md): server data as `Reactive`s
- Live: [reactive tools](https://kiteui.cs.lightningkite.com/docs/reactive-tools) in the example app
