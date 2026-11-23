# Theming

KiteUI styles by meaning.  You mark an element `important`, `danger` or `card`; the app's `Theme` decides what that looks like.  Swap the theme and every screen follows, so code rarely sets a color.

## Minimal example

```kotlin
col {
    important.button { text("Save") }
    danger.button { text("Delete") }
    card.col {
        h3("Card")
        text("Grouped content")
    }
    warning.text("Low balance")
}
```

The theme itself is set once, at the root of the app (see [Getting started](getting-started.md)):

```kotlin
val appTheme = Signal(Theme.material3("app", accent = Color.fromHexString("#2563EB")))
```

## Common tasks

### Semantic modifiers

| Modifier | Meaning |
|---|---|
| `card` | A separate surface for a group of content.  Nests: a card in a card steps again. |
| `important` | Draw attention: the primary action on a screen. |
| `critical` | Maximum attention; at most one per screen. |
| `danger`, `warning`, `affirmative` | Destructive, cautionary, successful. |
| `emphasized` | Mild emphasis for text. |
| `fieldTheme` | Looks like an input field.  `field("Label") { }` applies it for you. |
| `buttonTheme` | Looks like a button. |
| `bar`, `nav` | App bar and navigation surfaces. |
| `group` | Same as `card`. |
| `compact` | Tighter spacing. |
| `padded` | Force padding without a background. |
| `bold`, `italic`, `underline`, `strikethrough`, `allCaps`, `textSize(2.rem)`, `withSpacing(0.5)` | Text and spacing tweaks; no background. |

Theme modifiers stack (`important.card.col`), and they cascade to children: text inside `important` takes its foreground color.  They come after alignment, weight, `shownWhen` and sizing, and before scrolling: `expanding.card.scrolling.col { }`.

### Built-in themes

```kotlin
Theme.material3("app", accent = Color.fromHexString("#2563EB"))      // Material 3
Theme.material3("app-dark", background = Color.fromHexString("#141218"))
Theme.material("m2")                                                  // Material 2
Theme.clean("clean")                                                  // Apple-style grouped cells
Theme.shadCnLike("shad", background = Color.white)                    // shadcn/ui-like, hairline borders
Theme.light("light", accent = Color.fromHexString("#0E5C6B"))         // light only
Theme.flat2("flat2", hue = 0.6.turns)                                 // older, hue-based
```

For `material3`, `material`, `clean` and `shadCnLike`, the `background` color's brightness selects light or dark; there is no separate flag.  `accent` is the brand color the rest is derived from.  `light` takes no background, and `flat`/`flat2` take a `hue` instead.

### Fonts

```kotlin
Theme.material3(
    "app",
    title = FontAndStyle(font = Resources.fontsMontserrat, weight = 700),
    body = FontAndStyle(font = Resources.fontsOpensans),
)
```

Put one family's files in a folder such as `src/commonMain/resources/fonts/opensans/`, named by weight and style (`normal.ttf`, `bold.ttf`, `light-italic.ttf`, ...); the Gradle plugin generates one `Font`, `Resources.fontsOpensans`, covering them all.  `title` is used for headers, `body` for everything else.

### Adjust a theme

```kotlin
Theme.material3("app").copy("roomy", gap = 1.5.rem)
```

`copy` changes any of the theme's properties (`gap`, `padding`, `cornerRadii`, `elevation`, `font`, colors, transitions).  The new theme's id is `"<old id>-<id>"`.

### Restyle one semantic

```kotlin
Theme.material3("app").copy(
    "brand",
    semanticOverrides = semanticOverridesOf(
        ImportantSemantic to { it.withBack(background = Color.fromHexString("#E8590C"), foreground = Color.white) },
    ),
)
```

### A semantic of your own

```kotlin
data object HighlightSemantic : Semantic("hilite") {
    override fun default(theme: Theme): ThemeAndBack = theme.withBack(
        background = Color.fromHexString("#FFF3BF"),
        foreground = Color.black,
    )
}

themed(HighlightSemantic).col { text("Highlighted") }
```

`withBack(...)` draws the background and padding; `withoutBack` changes only what cascades (like `bold`).  Themes can override your semantic through `semanticOverrides` like any built-in one.  If you use it often, a one-line extension reads like the built-ins:

```kotlin
val ElementWriter.CanAddTheme.highlight: ElementWriter.CanAddTheme get() = themed(HighlightSemantic)
```

For a one-off, `themed(ThemeDerivation { it.copy("tinted", background = Color.fromHexString("#E7F5FF")).withBack })`.

### Theme from state

```kotlin
val selected = Signal(false)
dynamicThemed { if (selected()) SelectedSemantic else null }.button {
    text("Toggle")
    onClick { selected.value = !selected.value }
}
```

The block is reactive; return `null` for no change.  `SelectedSemantic`, `UnselectedSemantic` and `DisabledSemantic` are common choices.

### Switch theme at runtime

```kotlin
button {
    text("Dark mode")
    onClick { appTheme.value = Theme.material3("app-dark", background = Color.fromHexString("#141218")) }
}
```

Works when the root was given the `Signal`, as in the minimal example.

## Gotchas

- **Surface semantics draw a background and padding on the element they are applied to.**  Put them on a container, not on each child.

  ```kotlin
  // ❌ Two separate cards
  card.text("Title")
  card.text("Content")

  // ✅ One card
  card.col {
      text("Title")
      text("Content")
  }
  ```

- **Theme ids must be unique.**  Themes compare by `id`, and on the web each id becomes a CSS class, so two different themes with the same id render alike.  Give a dark variant its own id.
- **Order is enforced by the compiler.**  `expanding.scrolling.card.col` doesn't compile because scrolling comes after theme: write `expanding.card.scrolling.col`.
- **`dynamicThemed` replaces `dynamicTheme`.**  `dynamicTheme { }` inside an element and `Semantic.onNext` are deprecated.
- **Judge a screen by switching themes.**  If it only looks right in one theme, it is probably setting colors directly.

## See also

- [Layout](layout.md): modifier order
- [Example app theme tester](https://kiteui.cs.lightningkite.com/docs/theme-tester)
