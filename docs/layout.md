# Layout

Layout is three containers (`col`, `row`, `frame`) plus modifiers written in front of an element: `expanding.card.scrolling.col { }`.  Modifiers are adjectives; the element at the end of the chain is the noun.  Children size to their content along the container's main axis and stretch across the other axis, unless told otherwise.

## Minimal example

```kotlin
col {
    h1("Inbox")
    row {
        expanding.text("Search results")
        button { text("Refresh") }
    }
    expanding.card.scrolling.col {
        repeat(50) { text("Message $it") }
    }
}
```

The header and the row take what they need; the card takes the remaining height and scrolls.

## Modifier order

Modifiers must appear in this order, and the compiler enforces it.  Each modifier returns a narrower type that only offers the modifiers allowed after it.

```
alignment → weight → shownWhen → sizing → theme → scrolling → element
```

| Group | Modifiers |
|---|---|
| Alignment | `centered`, `atStart`, `atEnd`, `atTop`, `atBottom`, `atTopStart`, `atTopCenter`, `atTopEnd`, `atCenterStart`, `atCenterEnd`, `atBottomStart`, `atBottomCenter`, `atBottomEnd`, `centeredHorizontally`, `centeredVertically`, `align(h, v)`, `maxWidthCentered(width)` |
| Weight | `expanding` (= `weight(1f)`), `weight(n)`, `dynamicWeight { }` |
| Visibility | `shownWhen { }`, `shownForQuery(query)` |
| Sizing | `sizeConstraints(...)`, `dynamicSizeConstraints { }`, `maxWidth(w)`, `maxHeight(h)`, `setWidth(w)`, `setHeight(h)` |
| Theme (repeatable) | `card`, `important`, `danger`, `padded`, `unpadded`, `bold`, ... (see [Theming](theming.md)) |
| Scrolling | `scrolling`, `scrollingHorizontally`, `scrollingBoth { }`, `scrollingWithRefresh(action)` |

```kotlin
centered.expanding.sizeConstraints(maxWidth = 40.rem).card.scrolling.col { }   // ✅ every group in order
expanding.scrolling.card.col { }                                                // ❌ theme after scrolling: does not compile
expanding.centered.text("x")                                                     // ❌ alignment after weight: does not compile
```

The receiver decides where a chain may start.  Inside a container (`col { }`, `row { }`, `frame { }`) every group is available.  At the top level of `render()`, whose receiver is `ElementWriter.CanAddTheme`, only theme and scrolling modifiers are, so wrap the page in a `col { }`.  The snippets below assume they are inside a container.

```kotlin
override fun ElementWriter.CanAddTheme.render() {
    expanding.scrolling.col { }      // ❌ no weight at the top of render()
    col {
        expanding.scrolling.col { }  // ✅
    }
}
```

## Common tasks

### Share space with weights

```kotlin
row {
    card.text("A")
    expanding.card.text("B takes the rest")
    card.text("C")
}
row {
    weight(1f).card.text("1/3")
    weight(2f).card.text("2/3")
}
```

A weighted element ignores its natural size and takes a share of the leftover space, so long text in it wraps while unweighted siblings don't.

### Push items to the end

```kotlin
row {
    expanding.space()
    button { text("Cancel") }
    important.button { text("Save") }
}
```

An `expanding.space()` on both sides centers the group.

### Align a child across the axis

```kotlin
row {
    atTop.card.text("atTop")
    centered.card.text("centered")
    atBottom.card.text("atBottom")
}
col {
    atStart.card.text("atStart")
    centered.card.text("centered")
    atEnd.card.text("atEnd")
}
```

Without an alignment, a child stretches across the container (full width in a `col`, full height in a `row`).

### Layer views with `frame`

```kotlin
sizeConstraints(height = 12.rem).frame {
    image { source = Resources.imagesHero; scaleType = ImageScaleType.Crop; description = "" }
    centered.card.text("Centered")
    atTopEnd.button { icon(Icon.close, "Close") }
    atBottom.card.text("Caption across the bottom")
}
```

Children of a `frame` are stacked; unaligned children fill it.  `atBottom` stretches horizontally and sits at the bottom; corner modifiers like `atTopEnd` size to content.

### Constrain size

```kotlin
sizeConstraints(width = 20.rem).card.text("Fixed width")
sizeConstraints(minHeight = 10.rem, maxWidth = 30.rem).card.col { text("Bounded") }
sizeConstraints(aspectRatio = 16 to 9).card.frame { centered.text("16:9") }
maxWidthCentered(40.rem).card.col { text("Readable column, centered") }
```

Units: `rem` (scales with the root font size, preferred), `dp`, and `px`.  Write `Int.px`, `Int.rem`, `Double.rem`, `Int.dp`, `Double.dp`.

### Spacing and padding

```kotlin
col {
    gap = 0.25.rem          // this container only; does not cascade
    text("Tight")
    text("Spacing")
}
padded.text("Theme padding without a background")
unpadded.card.text("Background without padding")
card.frame {
    paddingByEdge = Edges(left = 1.rem, top = 2.rem, right = 0.rem, bottom = 0.5.rem)
    text("Per-edge padding")
}
```

The theme sets `gap` and padding.  An element with a background (`card`, `important`, buttons, fields) gets the theme's padding; one without gets none.  Prefer the defaults: explicit `gap` and `padding` are usually clutter.

### Scroll

```kotlin
scrolling.col { repeat(100) { text("Row $it") } }
scrollingHorizontally.row { repeat(20) { card.text("Chip $it") } }
scrollingWithRefresh(refreshAction).col { text("Pull to refresh") }
```

Nothing scrolls unless you say so; overflowing content is clipped.  `scrolling { }` and the other function forms give access to `ScrollingBehaviors` (`scrollTo`, `snapToElements`, `showScrollBars`).  For long lists use [`recyclerView` or `windowedList`](lists.md), which scroll themselves.

### Keep a button pinned below scrolling content

```kotlin
col {
    h2("Edit profile")
    expanding.scrolling.col {
        repeat(30) { text("Field $it") }
    }
    important.button { text("Save") }
}
```

### Adapt to the screen width

```kotlin
rowCollapsingToColumn(50.rem) {           // a row when the window is wider than 50rem, else a column
    weight(1f).card.col { text("Sidebar") }
    weight(3f).card.col { text("Main") }
}
rowWrapping {                             // wraps onto new lines like text
    repeat(12) { card.text("Tag $it") }
}
shownForQuery(MediaQuery.MinWidth(50.rem)).text("Only on wide screens")
```

The breakpoint is the window width, not the container's.  In column mode, weights are ignored.

### Show and hide

```kotlin
val showDetails = Signal(false)
col {
    switch { checked bind showDetails }
    expanding.shownWhen { showDetails() }.card.scrolling.col {
        text("Details")
    }
}
```

`shownWhen` keeps the element alive and toggles its visibility, with an optional `transition`.  To swap between different content, use `swapping(current = { ... }) { }`, or make each alternative its own page; see [Reactivity](reactivity.md).

### Extract a helper

```kotlin
fun ElementWriter.CanAddTheme.profileCard(name: String) {
    card.col {
        h3(name)
        subtext("Engineer")
    }
}
```

Use `ElementWriter.CanAddTheme` as the receiver so the helper can apply theme modifiers; a plain `ElementWriter` receiver has none.  Callers can still prefix it, as in `expanding.profileCard("Ada")`: the modifier applies to each element the helper writes directly, so have the helper write exactly one.

## Gotchas

- Alignment, weight, `shownWhen` and sizing modifiers aren't available at the top level of `render()`, or of a helper with an `ElementWriter.CanAddTheme` receiver.  Use them inside a `col { }`, or let the caller apply them.
- `expanding` fills only the parent it is in.  To fill the screen, every container from the page root down to the element must be `expanding` (or the root).  A collapsed element with `flex-grow: 1` on the web almost always has an ancestor that isn't weighted.
- Put layout modifiers before `shownWhen`, as the type order requires: `expanding.shownWhen { }.col { }`.  The modifier then applies to the wrapper `shownWhen` inserts, which is what the parent lays out.
- To center inside an expanding region, nest: `expanding.col { centered.text("…") }`.  `expanding.centered` doesn't compile, and `centered.expanding` aligns the element itself, not its content.
- Don't use `expanding` inside `scrollingHorizontally.row`/`scrolling.col` along the scrolling axis; there is no leftover space to share.  Give children a `sizeConstraints(minWidth = …)` instead.
- `.px` is a CSS pixel on the web but a physical pixel on Android and iOS.  Use `rem` or `dp` for breakpoints and anything that should look the same everywhere.
- The dash syntax from KiteUI 6 (`expanding - card - col { }`) no longer exists.

## See also

- [Components](components.md): what goes inside the containers
- [Theming](theming.md): `card`, `important`, and the rest of the theme modifiers
- In-app examples: [docs/layout](https://kiteui.cs.lightningkite.com/docs/layout), [docs/view-modifiers](https://kiteui.cs.lightningkite.com/docs/view-modifiers)
