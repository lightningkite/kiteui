# KiteUI Reactivity & Lists

Condensed from [docs/reactivity.md](../../../docs/reactivity.md) and [docs/lists.md](../../../docs/lists.md); read those for detail.

## Execution model (not React)

`render()` runs **once**.  Only bindings (`::prop { }`, `remember { }`, `reactive { }`) rerun when what they read changes.  `launch` in `render()` runs once and is cancelled when the view goes away.  There are no re-renders to avoid.

## Types (package `com.lightningkite.reactive.core`)

- `Signal(x)`: mutable, always ready; `.value` read/write anywhere.
- `Reactive<T>`: watchable; may be loading or failed.  `MutableReactive<T>`: also `suspend set(x)`.
- `LateInitSignal<T>()`: loading until set; `unset()`.  `Constant(x)`: never changes.
- `remember { }` → derived `Reactive`; lazy, shared.  `rememberSuspending { }` → async derived; loading until done, reloads when deps change; `useLastWhileLoading = true` keeps the old value.
- `x()` reads and subscribes.  Works in reactive blocks and in any suspend lambda (`onClick`, `Action`, `launch`).  `signal.value` inside a binding does NOT subscribe.

```kotlin
val count = Signal(0)
val doubled = remember { count() * 2 }
val user = rememberSuspending { api.fetchUser(userId()) }   // views show loading automatically

text { ::content { "Count: ${count()} / ${user().name}" } }
button { ::enabled { count() > 0 }; text("Go") }            // bind any var
text { ::content bind someReactiveString }                   // copy from a Reactive
textInput { content bind email }                             // two-way, same type
```

Don't store derived values in a second Signal and sync by hand; use `remember`.  Don't use `Signal + launch` for loading; use `rememberSuspending`.

## Lenses (`com.lightningkite.reactive.extensions`)

```kotlin
radioButton { checked bind size.equalTo(Size.Small) }
checkbox { checked bind (tags contains "featured") }         // Set or List
textInput { content bind nickname.nullToBlank() }            // String? ↔ String
numberInput { content bind age.asDouble() }                  // numberInput.content is MutableReactiveValue<Double?>; Int? → Double?
textInput { content bind user.lens(get = { it.first }, modify = { u, v -> u.copy(first = v) }) }
val writable = someReactive.withWrite { v -> api.save(v) }   // Reactive → MutableReactive
```

Also: `notNull(default)`, `String.asInt()/asLong()/asDouble()` (String ↔ number?), `asIntHex()…`, `toggle()`, `modify { }`.

## Conditional views

```kotlin
shownWhen { showAdvanced() }.card.col { /* ... */ }          // views persist; visibility toggles
swapping(current = { signedIn() }) { s -> if (s) text("Hi") else button { text("Sign in") } }
// swapping rebuilds on change; transition = { ScreenTransition.Push } for another animation.
// There is NO swapView(reactive) { } overload: use swapView { swapping(...) } or the swapping shortcut.
```

## Other tools

- `query.debounce(400.milliseconds)` inside a view (the `Long` overload is deprecated).  Also `tailThrottle`, `fullThrottle`.
- `reactive { sideEffect(tab()) }`: reruns on change.  For side effects, not for building views.
- `load { }`: like `launch` but shows the view's loading state.
- `PersistentProperty("key", default)` (`com.lightningkite.kiteui.reactive`): Signal saved to platform storage **as JSON** (a `String` is stored as `"\"value\""`; inject the JSON form from test harnesses).  Keep it top-level.
- `AppState.windowInfo / inForeground / softInputOpen / systemLanguages` (`com.lightningkite.kiteui.reactive`).
- `withLoadTracking().col { }` + `shownWhen { section.context.loadTracker.loading() }.activityIndicator()` for one indicator over a region.

## Lists

```kotlin
colOf(todos, id = { it.id }) { todo -> card.text { ::content { todo().title } } }  // keyed, animated; ≤ few hundred
rowOf(todos, id = { it.id }) { todo -> /* ... */ }
colOf(todos, placeholdersWhileLoading = 3) { todo -> /* ... */ }                  // positional
colOfExpensive(todos) { todo -> text(todo.title) }                                // rebuild each change; plain T
renderListIn(ElementWriter::frame, items, id = { it.id }) { /* ... */ }           // other container

expanding.recyclerView {                         // virtualized; scrolls itself; needs bounded size
    placer = RecyclerViewPlacerVerticalGrid(3)   // optional grid
    children(items, id = { it.id }) { item -> card.text { ::content { item().title } } }
}
// horizontalRecyclerView { }, viewPager { } (centerIndex), recyclerView(refreshAction = Action(...)) { }
// childrenMultipleTypes(items, id) { elementsMatching { it is X } renderedAs { ... }; elementsMatching { true } renderedAs { ... } }
// list.scrollToIndex(i, Align.Start); firstIndex / lastIndex are Reactive<Int>
// windowedList { children(...) } (@ExperimentalKiteUi): variable-height chat/feed; see docs/windowed-list.md
```

`forEach`, `forEachById`, `forEachAnimated`, `forEachUpdating` are **deprecated and `@InternalKiteUi`**.  Use `colOfExpensive`, `colOf(items, id)`, `colOfExpensive(items, animate = true)`, `colOf(items)`.

Load more: grow a `limit` Signal and re-query (`rememberSuspending(useLastWhileLoading = true) { api.list(limit()) }`), triggered from `reactive { if (lastIndex() >= items().size - 5 && items().size == limit()) limit.value += 50 }` inside the recyclerView.  Or `expanding.lazyColumn(items, id, loadMore = { ... }) { }` when you append pages yourself.

## Gotchas

```kotlin
// ❌ reactive that builds views duplicates them on every rerun, and clearChildren() clears siblings too
col { h2("Items"); reactive { for (i in items()) text(i) } }
// ✅ isolate and clear (or just use colOfExpensive)
col { h2("Items"); col { reactive { clearChildren(); for (i in items()) text(i) } } }

// ❌ return is prohibited here      ✅ labeled return
reactive { val x = s() ?: return }   reactive { val x = s() ?: return@reactive }

// ❌ does not compile: shownWhen returns CanAddSizing, expanding needs CanAddWeight
shownWhen { ready() }.expanding.recyclerView { }
// ✅ modifier order: alignment → weight → shownWhen → sizing → theme → scrolling → element
expanding.shownWhen { ready() }.recyclerView { }
```

- `expanding`, `weight`, `shownWhen` and alignment don't exist at the top level of `render()` (receiver `ElementWriter.CanAddTheme`).  Wrap the page in `col { }`.
- Don't build tabs from parallel `shownWhen`s: every tab stays alive, and on the web each adds a wrapper that can stop `expanding` children from stretching.  Use pages or `swapping`.
- Reading a reactive needs a reactive block or suspend context.  Render-time lambdas (a list's `render`, `col { }`) are neither: read inside `::content { }`.
- Old names (deprecated): `Property`→`Signal`, `shared`→`remember`, `LateInitProperty`→`LateInitSignal`, `LazyProperty`→`MutableRemember`, `Readable`/`Writable`→`Reactive`/`MutableReactive`.
