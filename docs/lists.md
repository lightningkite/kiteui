# Lists

To show a `Reactive<List<T>>`, pick a container by size: `colOf`/`rowOf` put every item in an ordinary column or row; `recyclerView` and `windowedList` render only the items near the viewport and scroll themselves.  Each one updates in place when the list changes.

## Minimal example

```kotlin
data class Todo(val id: Int, val title: String)
val todos = Signal(listOf(Todo(1, "Write docs"), Todo(2, "Ship")))

colOf(todos, id = { it.id }) { todo ->
    card.row {
        expanding.text { ::content { todo().title } }
        button {
            icon(Icon.delete, "Delete")
            onClick { todos.value = todos.value.filter { it.id != todo().id } }
        }
    }
}
```

`render` receives a `Reactive<Todo>`, not a `Todo`: the view for an id is built once and its bindings update when that item changes.  Read it with `todo()` inside bindings and suspend lambdas.

## Which one

| Use | When |
|---|---|
| `colOf` / `rowOf` | Up to a few hundred items, inside whatever scrolls (`scrolling.col { colOf(...) }`). |
| `recyclerView` | Long lists, grids, horizontal lists, pagers, pull-to-refresh, drag to reorder. |
| `windowedList` | Long vertical lists of cells of any height: chats, feeds.  Cheapest on the web.  See [WindowedList](windowed-list.md). |

## Common tasks

### Keyed, positional, or rebuilt

```kotlin
colOf(todos, id = { it.id }) { todo -> text { ::content { todo().title } } }              // keyed (recommended)
colOf(todos, id = { it.id }, animate = false) { todo -> text { ::content { todo().title } } }
colOf(todos, placeholdersWhileLoading = 3) { todo -> text { ::content { todo().title } } } // positional
colOfExpensive(todos) { todo -> text(todo.title) }                                         // rebuilt on every change
```

- **Keyed** matches items by `id`, so views follow their items through inserts, removals and reorders, animated by default.
- **Positional** (no `id`) reuses views by index.  It can show placeholder rows while the list is loading.
- **Expensive** throws every view away and rebuilds the list on each change.  `render` gets the plain `T`.  Use it when items have no identity and the list is small.

`rowOf` and `rowOfExpensive` are the same in a row.  For another container, `renderListIn(ElementWriter::frame, items, id = { it.id }) { ... }`.

### Different kinds of rows

```kotlin
val rows = RecyclerViewRendererSet.multi<FeedItem, Int>(id = { it.id }) {
    elementsMatching { it is Divider } renderedAs { item -> subtext { ::content { (item() as Divider).label } } }
    elementsMatching { true } renderedAs { item -> card.text { ::content { (item() as Post).body } } }
}
colOf(feed, rows)
```

The first matching rule wins; end with a catch-all.

### A long list

```kotlin
expanding.recyclerView {
    children(items, id = { it.id }) { item ->
        card.text { ::content { item().title } }
    }
}
```

`recyclerView` scrolls on its own and needs its size from outside: give it `expanding` in a column, `sizeConstraints(height = ...)`, or fill a `frame`.  Cells are reused as you scroll, so `render` runs once per cell, not per item; keep per-item state in the item, not in local variables.

### Grid, horizontal, pager

```kotlin
expanding.recyclerView {
    placer = RecyclerViewPlacerVerticalGrid(3)          // 3 columns
    children(items, id = { it }) { n -> card.text { ::content { n().toString() } } }
}

sizeConstraints(height = 8.rem).horizontalRecyclerView {
    children(items, id = { it }) { n -> sizeConstraints(width = 6.rem).card.text { ::content { n().toString() } } }
}

sizeConstraints(height = 12.rem).viewPager {
    children(items, id = { it }) { n -> centered.text { ::content { "Page ${n()}" } } }
}
```

`viewPager` shows one item per page with snapping, and previous/next buttons where there is no touchscreen.  Its `centerIndex` is a `MutableReactive<Int>` you can read or set.

### Different kinds of cells in a recycler

```kotlin
expanding.recyclerView {
    childrenMultipleTypes(feed, id = { it.id }) {
        elementsMatching { it is Divider } renderedAs { item -> subtext { ::content { (item() as Divider).label } } }
        elementsMatching { true } renderedAs { item -> card.text { ::content { (item() as Post).body } } }
    }
}
```

A cell is only reused for items of its own kind.

### Scroll to an item

```kotlin
lateinit var list: Recycler2
button {
    text("Jump to 100")
    onClick { list.scrollToIndex(100, Align.Start) }
}
expanding.recyclerView {
    list = this
    children(items, id = { it.id }) { item -> text { ::content { item().title } } }
}
```

`firstIndex` and `lastIndex` are `Reactive<Int>`s of the range currently rendered.

### Load more as the user scrolls

```kotlin
val limit = Signal(50)
val items = rememberSuspending(useLastWhileLoading = true) { api.todos(limit = limit()) }

expanding.recyclerView {
    children(items, id = { it.id }) { item -> text { ::content { item().title } } }
    reactive {
        val loaded = items().size
        if (lastIndex() >= loaded - 5 && loaded == limit()) limit.value += 50
    }
}
```

This grows the window: each step re-requests everything up to the new limit, which suits hundreds to low thousands of rows.  `useLastWhileLoading` keeps the old rows on screen during the reload.  `loaded == limit()` stops once the source runs out.

If you append pages yourself instead, `lazyColumn` calls `loadMore` when the user nears the end, and stops when a load doesn't grow the list:

```kotlin
expanding.lazyColumn(
    items = items,                       // a Signal you append to
    id = { it.id },
    loadMore = { items.value += api.todos(offset = items.value.size, limit = 50) },
) { item -> text { ::content { item().title } } }
```

### Pull to refresh

```kotlin
expanding.recyclerView(refreshAction = Action("Refresh") { items.value = api.todos() }) {
    children(items, id = { it.id }) { item -> text { ::content { item().title } } }
}
```

For a plain scrolling column, `scrollingWithRefresh(action)` does the same.

### Empty state

```kotlin
expanding.frame {
    recyclerView {
        children(items, id = { it.id }) { item -> text { ::content { item().title } } }
    }
    centered.shownWhen { items().isEmpty() }.text("Nothing here yet")
}
```

### Drag to reorder

```kotlin
renderReorderableListIn(ElementWriter::col, numbers, reorder = { move -> numbers.value = move.reorder(numbers.value) }) { n ->
    card.text { ::content { n().toString() } }
}

expanding.recyclerView {
    childrenReorderable(numbers, id = { it }, reorder = { move -> numbers.value = move.reorder(numbers.value) }) { n ->
        card.text { ::content { n().toString() } }
    }
}
```

## Gotchas

- **Ids must be unique and stable.**  The id is how a keyed list knows which view belongs to which item.  Don't use the list index as the id of a list that reorders.
- **`forEach`, `forEachById`, `forEachAnimated` and `forEachUpdating` are deprecated** and marked `@InternalKiteUi`.  Replace them with `colOfExpensive`, `colOf(items, id)`, `colOfExpensive(items, animate = true)` and `colOf(items)` respectively (or the `rowOf` forms), which also create the container.
- **A `recyclerView` needs a bounded size.**  It doesn't grow to fit its content.  Give it `expanding`, a fixed height, or a frame to fill.
- **`expanding` and `shownWhen` only work inside a container.**  At the top of `render()` the receiver is `ElementWriter.CanAddTheme`, which has no weight or visibility modifiers, so `expanding.recyclerView { }` there doesn't compile.  Wrap the page in `col { }`; the snippets on this page assume you are inside one.
- **Don't put a `recyclerView` inside `scrolling`.**  It does its own scrolling.
- **Don't filter big lists on the client.**  Filter at the source (for Lightning Server, in the `Query`; see [model cache](model-cache.md)).
- **`reactive { }` that builds rows duplicates them** on every change; see [reactivity](reactivity.md#gotchas).

## See also

- [WindowedList](windowed-list.md) in depth
- [Reactivity](reactivity.md)
- Live: [RecyclerView](https://kiteui.cs.lightningkite.com/docs/recyclerView) in the example app
