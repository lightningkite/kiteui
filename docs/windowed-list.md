# WindowedList

A vertical list for long or growing collections whose cells can be any height: chat transcripts, feeds, search results.
It renders only the items near the viewport and reuses their cells as you scroll.

```kotlin
val messages = Signal(listOf<Message>())
lateinit var list: WindowedList

expanding.windowedList {
    list = this
    children(items = messages, id = { it.id }) { message ->
        card.col {
            subtext { ::content { message().author } }
            text { ::content { message().body } }
        }
    }
}
```

`windowedList` is experimental: opt in with `@OptIn(ExperimentalKiteUi::class)` (or `@file:OptIn`).

Runnable versions are in the example app: `WindowedListPage` (route `windowed-list`), and `WindowedListTypesPage`
(route `windowed-list/types`) for more than one kind of cell.

## API

| Member | What it does |
|---|---|
| `children(items, id, render)` | Sets the items.  `id` must be unique and stable per item; `render` builds one cell from a `Reactive` of its item.  Set the children once, with this or the next. |
| `childrenMultipleTypes(items, id) { ... }` | The same, for items that don't all look alike.  See below. |
| `scrollToIndex(index, align, animate = true)` | Puts the item at the start, center or end of the viewport, rendering it first if it isn't on screen. |
| `startAtEnd` | Starts at the end instead of the top, and rests there, following new items, until scrolled away: a chat. |
| `firstIndex`, `lastIndex` | The range of items currently rendered, as `Reactive<Int>`.  Useful for loading more as the end approaches. |

## More than one kind of cell

When items render differently enough that one cell can't sensibly become another, give each kind its own renderer.
The first matching rule wins, and a cell is only ever reused for items of its own kind.

```kotlin
windowedList {
    childrenMultipleTypes(items = entries, id = { it.id }) {
        elementsMatching { it is DateDivider } renderedAs { divider -> subtext { ::content { divider().label } } }
        elementsMatching { true } renderedAs { message -> messageCell(message) }
    }
}
```

This is the same builder `Recycler2.childrenMultipleTypes` takes.

## Behavior

- **Cells are reused.**  `render` runs once per cell, not once per item; the `Reactive` it receives changes as the cell is
  given another item.  Keep per-item state in the item or keyed by its id, not in variables inside `render`.
- **Same id, same cell.**  When the list changes, an item that is still shown keeps its cell, so only its bindings that
  actually changed rerun.
- **It stays at the end.**  A list last scrolled to its end stays there as items are added or the last cell grows, which is
  what a chat wants.  Scrolling away releases it; scrolling back to the end pins it again.  To start at the end, set
  `startAtEnd = true`; it waits there for items that arrive later.
- **Cells can change height on their own.**  Text streaming in, an image loading or a section expanding needs nothing
  from you.  What is in view holds still when a cell above it grows or shrinks, and a list at its end follows its last
  cell.  On the web the list does this itself, in every browser, rather than relying on the browser's scroll anchoring,
  which Safari lacks.
- **Heights are estimated until seen.**  An item that has never been rendered is assumed to be of average height, so the
  scrollbar's length shifts a little as you go.

## Platforms

| Platform | Implementation |
|---|---|
| Web | Cells are ordinary flex children laid out by the browser, between two spacers that stand in for the items not rendered. |
| Android, iOS, Compose desktop, server-side rendering | A [`Recycler2`](../library/src/commonMain/kotlin/com/lightningkite/kiteui/views/l2/Recycler2.kt) behind the same interface. |

On the web this costs roughly a third of what `Recycler2` does to scroll the same list, because nothing is measured or
positioned from Kotlin.  Elsewhere it performs exactly as `Recycler2` does.

`scrollToIndex` may be called before the item exists; the list goes there once it does.

## Accessibility

On the web:

- The list is `role="list"`; each cell is wrapped in a `role="listitem"` carrying `aria-posinset` and `aria-setsize`, so
  assistive technology knows the size of the whole list and the position of each item, rendered or not.  The element you
  render keeps its own role.
- Rendered cells are kept in item order in the DOM, so reading order and Tab order match what is on screen.
- The list is focusable, so it can be scrolled from the keyboard.  If the cell holding focus scrolls out of the window and
  is reused, focus moves to the list rather than landing on a different item.

Give the list a name where its purpose isn't obvious from context.  New items are not announced; a chat that should
announce them needs a live region of its own.

On other platforms accessibility is whatever `Recycler2` provides, which has not been audited.

## When to use something else

- A short list that fits in memory comfortably: `colOf` is simpler.  See [Lists](lists.md).
- A grid, a horizontal list, snapping, drag-to-reorder or pull-to-refresh: `recyclerView` / `Recycler2` has these and
  `WindowedList` does not.
