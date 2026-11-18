# KiteUI Layout

Full chapter: [docs/layout.md](../../../docs/layout.md).

## Shape

`modifier.modifier.element { children }`.  Containers: `col`, `row`, `frame` (stack), `rowCollapsingToColumn(bp)`, `rowWrapping`.  Children size to content on the main axis and stretch across the other axis.

## Modifier order (compiler-enforced)

```
alignment → weight → shownWhen → sizing → theme → scrolling → element
```

```kotlin
centered.expanding.sizeConstraints(maxWidth = 40.rem).card.scrolling.col { }  // ✅
expanding.card.scrolling.col { }                                              // ✅
expanding.scrolling.card.col { }    // ❌ theme after scrolling
expanding.centered.text("x")        // ❌ alignment after weight
shownWhen { a() }.expanding.col { } // ❌ weight after shownWhen; write expanding.shownWhen { a() }.col { }
```

Chains with alignment/weight/shownWhen/sizing only work inside a container.  `render()` and `CanAddTheme` helpers start at theme level:

```kotlin
override fun ElementWriter.CanAddTheme.render() {
    expanding.scrolling.col { }              // ❌ no weight at top of render()
    col { expanding.scrolling.col { } }      // ✅
}
```

- Alignment: `centered`, `atStart/atEnd/atTop/atBottom`, corners (`atTopEnd`, `atBottomCenter`, ...), `centeredHorizontally/Vertically`, `align(h, v)`, `maxWidthCentered(w)`
- Weight: `expanding` (= `weight(1f)`), `weight(n)`, `dynamicWeight { }`
- Visibility: `shownWhen { }`, `shownForQuery(MediaQuery.MinWidth(50.rem))`
- Sizing: `sizeConstraints(width, height, minWidth, maxWidth, minHeight, maxHeight, aspectRatio)`, `maxWidth(w)`, `maxHeight(h)`, `dynamicSizeConstraints { }`
- Theme (repeatable): `card`, `important`, `danger`, `padded`, `unpadded`, `bold`, ...
- Scrolling: `scrolling`, `scrollingHorizontally`, `scrollingBoth { }`, `scrollingWithRefresh(action)`; function forms take a `ScrollingBehaviors.() -> Unit`

## Patterns

```kotlin
row { card.text("A"); expanding.card.text("rest"); card.text("C") }   // fill remaining space
row { weight(1f).text("1/3"); weight(2f).text("2/3") }                 // ratios
row { expanding.space(); button { text("Cancel") }; important.button { text("Save") } }  // push to end
col { atStart.text("a"); centered.text("b"); atEnd.text("c") }       // cross-axis alignment
frame { image { ... }; centered.text("over"); atTopEnd.button { icon(Icon.close, "Close") } }
col { h2("Title"); expanding.scrolling.col { /* content */ }; important.button { text("Save") } }  // pinned footer
expanding.col { centered.text("centered in remaining space") }        // center inside expanding region
rowCollapsingToColumn(50.rem) { weight(1f).col { }; weight(3f).col { } }  // window-width breakpoint; column ignores weights
scrollingHorizontally.row { repeat(20) { sizeConstraints(minWidth = 4.rem).card.text("$it") } }
```

Helpers: receiver `ElementWriter.CanAddTheme` (plain `ElementWriter` has no theme modifiers).  Callers can prefix (`expanding.myCard()`); the prefix applies to every element the helper writes directly, so write one.

## Rules

- Nothing scrolls unless marked `scrolling`; overflow is clipped.  Lists: `recyclerView`/`windowedList` scroll themselves.
- `expanding` only fills its parent.  For full height, every ancestor must be `expanding`.  If an `expanding` view collapses on the web, walk up to the first ancestor without weight.
- Gap and padding come from the theme.  Elements with a background get padding; others don't.  Avoid `gap = ` / `padding = ` unless needed; `gap` doesn't cascade.
- Units: `Int.rem`, `Double.rem`, `Int.dp`, `Double.dp`, `Int.px`.  No `percent`, no `Double.px`.  `.px` is physical pixels on Android/iOS but CSS pixels on web: use `rem`/`dp` for sizes and breakpoints.
- Don't build tabs from parallel `shownWhen` branches: every branch stays alive.  Use separate `@Routable` pages or `swapping(current = { tab() }) { }`.
- KiteUI 6 dash syntax (`expanding - col { }`) is gone.
