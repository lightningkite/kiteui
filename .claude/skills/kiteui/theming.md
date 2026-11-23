# KiteUI Theming

Full chapter: [docs/theming.md](../../../docs/theming.md).  Style by meaning; never set colors in screens.

## Semantic modifiers (theme slot of the modifier chain)

Order: alignment → weight → shownWhen → sizing → **theme** → scrolling → element.  So `expanding.card.scrolling.col {}` (✅), not `expanding.scrolling.card.col {}` (❌ compile error).

```kotlin
important.button { text("Save") }     // primary action
critical.button { }                    // max attention, ≤1 per screen
danger / warning / affirmative         // destructive / caution / success
card.col { }                           // surface; nests
fieldTheme.textInput { }               // field look (field("Label") {} applies it)
buttonTheme, bar, nav, group, compact, padded, emphasized
bold, italic, underline, strikethrough, allCaps, textSize(2.rem), withSpacing(0.5)   // no background
themed(SomeSemantic).col { }           // any Semantic / ThemeDerivation
dynamicThemed { if (selected()) SelectedSemantic else null }.button { }   // reactive
```

Modifiers stack and cascade to children.  Surface semantics (card, important, danger, ...) draw background + padding: put them on a container, not on each `text` (`card.text("A"); card.text("B")` = two cards).  `dynamicTheme {}` inside an element and `Semantic.onNext` are deprecated.

## Built-in themes

```kotlin
Theme.material3("app", accent = Color.fromHexString("#2563EB"))
Theme.material3("app-dark", background = Color.fromHexString("#141218"))  // dark = dark background
Theme.material("m2"); Theme.clean("clean"); Theme.shadCnLike("shad", background = Color.white)
Theme.light("light", accent = ...)     // light only, no background param
Theme.flat2("flat2", hue = 0.6.turns)  // flat/flat2 are hue-based
```

`material3`, `material`, `clean`, `shadCnLike` take `(id, background, accent, ..., title, body)`; background brightness picks light/dark.

- Fonts: `title = FontAndStyle(font = Resources.fontsMontserrat, weight = 700), body = FontAndStyle(font = Resources.fontsOpensans)`; font files go in `resources/fonts/<family>/` named by weight/style.
- Shape/spacing: `theme.copy("roomy", gap = 1.5.rem)` (id becomes `"<old>-roomy"`).
- Restyle a semantic: `theme.copy("brand", semanticOverrides = semanticOverridesOf(ImportantSemantic to { it.withBack(background = ..., foreground = ...) }))`.
- Theme ids must be unique (equality and web CSS class are by id).
- Runtime switch: root takes a `Signal<Theme>`; set `appTheme.value = ...`.

## Custom semantic

```kotlin
data object HighlightSemantic : Semantic("hilite") {          // key must be unique
    override fun default(theme: Theme): ThemeAndBack =
        theme.withBack(background = Color.fromHexString("#FFF3BF"), foreground = Color.black)
}
themed(HighlightSemantic).col { }
val ElementWriter.CanAddTheme.highlight: ElementWriter.CanAddTheme get() = themed(HighlightSemantic)  // optional
```

`withBack(...)` = background + padding; `withoutBack(...)` = cascading changes only.  One-off: `themed(ThemeDerivation { it.copy("tinted", background = ...).withBack })`.

## Custom icons

```kotlin
val Icon.Companion.bug: Icon get() = Icon(
    width = 1.5.rem, height = 1.5.rem,
    viewBoxMinX = 0, viewBoxMinY = -960, viewBoxWidth = 960, viewBoxHeight = 960,
    pathDatas = listOf("M480-200q66 0 113-47t47-113..."),
)
```
