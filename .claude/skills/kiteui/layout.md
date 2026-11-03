# KiteUI Layout System

## Modern Syntax (KiteUI v7+)

**CRITICAL**: KiteUI v7 uses **dot notation** (`.`) for chaining modifiers and containers. The dash operator (`-`) is **DEPRECATED** and does not exist in modern KiteUI.

```kotlin
// ✅ CORRECT - Modern v7 syntax
expanding.scrolling.col { }
card.centered.text("Hello")
weight(1f).card.text("Content")

// ❌ WRONG - Old v6 syntax (DO NOT USE)
expanding - scrolling - col { }
card - centered - text("Hello")
```

## Modifiers vs Containers

**Modifiers** are adjectives that modify the element that comes after them:
- `expanding` - Makes element take available space (equivalent to `weight(1f)`)
- `scrolling` - Enables vertical scrolling
- `scrollingHorizontally` - Enables horizontal scrolling
- `centered` - Centers content
- `card` - Applies card background/styling
- `padded` - Adds default padding
- `weight(N.f)` - Flex-grow and flex-shrink in one
- Size modifiers: `sizeConstraints()`, positioning modifiers, etc.

**Containers** are the actual layout elements that hold children:
- `col` - Vertical layout (stacks children vertically)
- `row` - Horizontal layout (arranges children side by side)
- `frame` - Z-stack / FrameLayout (stacks children on top of each other)
- `rowCollapsingToColumn(breakpoint)` - Responsive layout that becomes column below breakpoint

**The Pattern**: `modifier.modifier.container { children }`

```kotlin
// Modifiers chain together, then end with a container
expanding.scrolling.card.col {
    // expanding = modifier (takes available space)
    // scrolling = modifier (enables vertical scroll)
    // card = modifier (applies card styling)
    // col = container (vertical layout)
}
```

## CSS Equivalents

For those familiar with CSS, here are the mappings:

**Layout Containers:**
```
col                → display: flex; flex-direction: column
row                → display: flex; flex-direction: row
frame              → position: relative (with absolute children)
gap = 1.rem        → gap: 1rem (flexbox/grid gap)
```

**Modifiers:**
```
expanding          → flex: 1 (flex-grow: 1; flex-shrink: 1)
weight(2f)         → flex: 2
scrolling          → overflow-y: auto
scrollingHorizontally → overflow-x: auto
centered           → display: flex; align-items: center; justify-content: center
padded             → padding: var(--spacing)
sizeConstraints(width = 20.rem) → width: 20rem
```

**Frame Positioning (like CSS absolute positioning):**
```
centered           → position: absolute; top: 50%; left: 50%; transform: translate(-50%, -50%)
atTop              → position: absolute; top: 0; left: 0; right: 0
atBottom           → position: absolute; bottom: 0; left: 0; right: 0
atStart            → position: absolute; left: 0; top: 0; bottom: 0
atEnd              → position: absolute; right: 0; top: 0; bottom: 0
atTopStart         → position: absolute; top: 0; left: 0
atBottomEnd        → position: absolute; bottom: 0; right: 0
```

**Chaining Pattern:**
```kotlin
expanding.scrolling.card.col { }
```
Similar to utility-first CSS (like Tailwind):
```html
<div class="flex-1 overflow-y-auto bg-card flex flex-col">
```

## Critical Scrolling Rule

⚠️ **NOTHING scrolls without explicit instruction**. You must use `scrolling` or `scrollingHorizontally` modifiers to enable scrolling.

```kotlin
// ❌ This will NOT scroll, even if content overflows
col {
    repeat(100) { text("Item $it") }
}

// ✅ This will scroll vertically
scrolling.col {
    repeat(100) { text("Item $it") }
}

// ✅ This will scroll horizontally
scrollingHorizontally.row {
    repeat(100) { card.text("$it") }
}
```

## Layout Containers

### Column (Vertical Layout)
```kotlin
col {
    gap = 1.rem  // Space between children
    text("First")
    text("Second")
    text("Third")
}

// With scrolling
scrolling.col {
    repeat(100) { text("Item $it") }
}

// Takes available space
expanding.col {
    text("Top")
    text("Bottom")
}
```

### Row (Horizontal Layout)
```kotlin
row {
    gap = 0.5.rem
    card.text("Left")
    expanding.card.text("Center (expands)")
    card.text("Right")
}

// Equal-width items using weight
row {
    weight(1f).card.text("A")
    weight(1f).card.text("B")
    weight(1f).card.text("C")
}

// Using expanding (equivalent to weight(1f))
row {
    card.text("A")
    expanding.card.text("B")  // Takes available space
    card.text("C")
}
```

### Frame (Z-Stack / Stacked Layout)
```kotlin
frame {
    // Children are stacked on top of each other (Z-axis)
    image { source = Resources.background }
    centered.text("Overlay Text")
    atTopStart.text("Top Left")
    atBottomEnd.text("Bottom Right")
}
```

**Frame positioning modifiers**:
- `centered` - Center of the frame
- `atTop`, `atBottom`, `atStart`, `atEnd` - Edge alignment
- `atTopStart`, `atTopEnd`, `atBottomStart`, `atBottomEnd` - Corner alignment

### Responsive Layout
```kotlin
// Becomes column when screen width < 70rem
rowCollapsingToColumn(70.rem) {
    weight(1f).card.text("Sidebar")
    weight(3f).card.text("Main Content")
}
```

## Sizing

```kotlin
// Set specific size
sizeConstraints(width = 20.rem, height = 10.rem).text("Fixed size")

// Set only width or height
sizeConstraints(width = 15.rem).text("Fixed width")

// Min/max constraints
sizeConstraints(minWidth = 10.rem, maxWidth = 30.rem).text("Constrained")
```

## Layout Positioning

```kotlin
// In a row
expanding.text("Takes available space")
weight(2f).text("Takes 2x space relative to weight(1f)")

// In a frame
centered.text("Centered")
atTop.text("Top")
atBottom.text("Bottom")
atStart.text("Start (left in LTR)")
atEnd.text("End (right in LTR)")
atTopStart.text("Top left")
atBottomEnd.text("Bottom right")
```

## Equal Width Cards in a Row

Use `expanding` on each item to make them equal width:

```kotlin
row {
    listOf("Option A", "Option B", "Option C").forEach { label ->
        expanding.card.button {
            centered.text { content = label }
            onClick { /* ... */ }
        }
    }
}
```

**Important**: You cannot use `expanding.` with conditional expressions directly. Apply `expanding.` to each branch:

```kotlin
// ❌ Won't work - conditionals can't be chained with dot notation
expanding.if (isSelected) { selected.card.button { } } else { card.button { } }

// ✅ Correct pattern - apply expanding inside each branch
if (isSelected) {
    expanding.selected.card.button { /* ... */ }
} else {
    expanding.card.button { /* ... */ }
}
```

## Fixed Bottom Button Pattern

Put action buttons outside the scrolling container so they're always visible:

```kotlin
col {
    // Header
    row { /* ... */ }

    // Scrollable content
    expanding.padded.scrolling.col {
        // Content that may overflow
        card.col { /* ... */ }
        card.col { /* ... */ }
    }

    // Fixed bottom button (always visible)
    padded.important.button {
        centered.text { content = "Submit" }
        onClick { /* ... */ }
    }
}
```

**Key Points**:
- Use `expanding` on the scrolling container to fill available space
- Put the action button AFTER the scrolling container
- The button stays fixed at the bottom regardless of scroll position

## Responsive Horizontal Scrolling

For rows with many options that don't fit on small screens, use `scrollingHorizontally`:

```kotlin
// Option cards that scroll horizontally on mobile
subtext { content = "Select Time" }
scrollingHorizontally.row {
    listOf("6 AM", "7 AM", "8 AM", "9 AM", "10 AM").forEach { time ->
        sizeConstraints(minWidth = 4.rem).card.button {
            centered.text { content = time }
            onClick { /* ... */ }
        }
    }
}
```

**Key Points**:
- Use `scrollingHorizontally.row { }` to make row contents scrollable
- Add `sizeConstraints(minWidth = X.rem)` to prevent cards from getting too small
- Don't use `expanding` with `scrollingHorizontally` - cards should have fixed/minimum widths
- On desktop, all items show; on mobile, users can scroll horizontally

**When to use each approach**:
- `expanding` - When you want items to fill available width (good for 2-3 items)
- `scrollingHorizontally` + `sizeConstraints(minWidth)` - When you have 4+ items that may not fit on mobile
- `rowCollapsingToColumn(breakpoint)` - When you want items to stack vertically on small screens

## Modifier Order

**Position > Visibility > Scroll > Theme**

Example:
```kotlin
expanding.scrolling.card.col {
    // Position first (expanding)
    // Then scroll (scrolling)
    // Then theme (card)
}
```

### Pitfall: `expanding` + `shownWhen` Order

<!-- by Claude -->
`shownWhen` creates a wrapper element. Modifiers **before** it affect the wrapper; modifiers **after** it affect the child inside.

```kotlin
// ❌ BROKEN: expanding is applied to the child inside the wrapper.
// The shownWhen wrapper has flex-grow: 0 → collapses to height: 0px!
shownWhen { !isLoading() }.expanding.recyclerView { ... }

// ✅ CORRECT: expanding is applied to the shownWhen wrapper itself.
// The wrapper gets flex-grow: 1 and takes available space.
expanding.shownWhen { !isLoading() }.recyclerView { ... }
```

This applies to any layout modifier (`expanding`, `weight()`, `sizeConstraints()`, etc.) — always put them **before** `shownWhen` so they affect the wrapper, not the hidden child.

### Pitfall: a column only becomes flex when a direct child has weight

<!-- by Claude -->
KiteUI emits a column as `display: block` until one of its **direct** children carries weight, at
which point it gains `childHasWeight` and becomes `display: flex`. A block container lets children
size to content, so an `expanding` grandchild will not stretch — even though the grandchild really
does have `flex-grow: 1`. Inspecting the collapsed element shows correct CSS and hides the cause;
walk *up* the chain looking for the first `display: block` column.

This bites hardest with `shownWhen`, which inserts its own column:

```kotlin
// ❌ Output pane collapses: the shownWhen wrapper's column has no weighted direct child,
//    so it stays display:block and the expanding output never stretches.
expanding.shownWhen { tab() == Tab.Logs }.col {
    card.row { /* controls */ }
    expanding.outputView(...)          // reports flex-grow:1, still 24px tall
}

// ✅ Weight the content column so the wrapper above it becomes a flex container.
expanding.shownWhen { tab() == Tab.Logs }.col {
    expanding.col {
        card.row { /* controls */ }
        expanding.outputView(...)
    }
}
```

Better still, avoid the nesting entirely — make each branch its own `@Routable` page, or use
`swapView`. See "Don't build tabs from parallel `shownWhen` branches" in SKILL.md.

Debugging recipe: dump `clientHeight` / `flexGrow` / `display` for each ancestor. The culprit is
the highest node that has the right height but `display: block`.

## Units and Dimensions

```kotlin
// Rem units (relative to root font size) - preferred for responsive design
1.rem
2.5.rem

// Pixels (absolute)
100.px
50.px

// DP (density-independent pixels, Android concept)
16.dp

// Percentages
50.percent
```
