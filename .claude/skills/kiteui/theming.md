# KiteUI Theming

## Using Built-in Themes

Every built-in factory takes the same first three parameters — `(id, background, accent)` — so
switching design language means swapping the function name and nothing else. `background` is the
app canvas, and **its brightness is what selects light or dark**; there is no separate flag.
`accent` is the one brand color the theme derives everything else from. Everything after those has
a default.

```kotlin
Theme.material()      // Material 2: primary app bar, elevation shadows, all-caps actions
Theme.material3()     // Material 3: tonal palette, pill controls, tone instead of shadow
Theme.clean()         // Apple: grouped white cells on gray, one tint, translucent bars
Theme.shadCnLike()    // shadcn/ui: near-neutral, hairline borders, small dense type
Theme.light()         // light-only; surfaces separate by light, outlines mean "actionable"

// Dark is the same call with a dark canvas.
Theme.material3("app-dark", background = Color.fromHexString("#141218"))

// A brand color reaches the primary action, selection, focus — and in most of these, the neutrals.
Theme.shadCnLike("app", accent = Color.fromHexString("#2563EB"))
```

`Theme.flat` and `Theme.flat2` predate this and are still parameterized by `hue` instead.

To change a theme's shape rather than its colors, derive from it with `Theme.copy` — the factories
deliberately do not take `elevation`/`cornerRadii`/`gap`/`outline` parameters:

```kotlin
Theme.material("compact").copy(id = "compact", gap = 0.5.rem)
```

## Applying Semantics

```kotlin
// These are modifiers that apply semantic meaning
important.button { text("Save") }      // Important action
danger.button { text("Delete") }       // Destructive action
warning.text("Warning message")        // Warning
critical.button { text("Override") }   // Critical action
card.col { /* ... */ }                 // Card background
fieldTheme.textInput { /* ... */ }     // Field styling
```

## Theme Rules

1. **Switching themes causes a background/card** - When you switch to a different semantic theme, it creates a visual container
2. **Switching to the *same* theme doesn't create a card** - Use `card` explicitly if needed
3. **Apply theme switches to containers, not individual elements**
4. **Typical places for themes**: `button`, `col`, `row`, `frame`

### Examples

```kotlin
// Creates background (important ≠ default)
important.button { text("Save") }

// To force a card when already in same theme context:
card.col { /* ... */ }

// ✅ Correct - theme on container
card.col {
    text("Title")
    text("Content")
}

// ❌ Avoid - themes on individual text
card.text("Title")
card.text("Content")
```

## Custom Semantics

### Simple Custom Semantic

```kotlin
data object LinkButtonSemantic : Semantic("lnkbtn") {
    override fun default(theme: Theme): ThemeAndBack = theme.copy(
        id = key,
        semanticOverrides = semanticOverridesOf(
            HoverSemantic to {
                it.copy(
                    id = "hov",
                    font = it.font.copy(underline = true)
                ).withoutBack
            }
        )
    ).withoutBack
}

// Extension for convenient access
@ViewModifierDsl3
val ViewWriter.linkButton: ViewWriter get() = LinkButtonSemantic.onNext

// Usage
linkButton.button {
    text("Click me")  // Underlines on hover
}
```

### Parametric Semantic

```kotlin
data class Background(
    val pad: Boolean = false,
    val background: (Theme) -> Paint
) : Semantic("mnbck") {
    override fun default(theme: Theme): ThemeAndBack =
        theme.copy(
            id = key,
            background = background(theme)
        ).let {
            if (pad) it.withBack else it.withBackNoPadding
        }
}

// Usage
Background(true) { it.background.closestColor() }.onNext.text("Solid bg")
```

## Dynamic Theme Switching

Apply themes dynamically based on state:

```kotlin
// Highlight current selection
link {
    dynamicTheme {
        if (project()._id == currentProjectId()) SelectedSemantic
        else if (project()._id in myProjects()) null
        else NotRelevantSemantic
    }
    text { ::content { project().name } }
    to = { ProjectPage(project()._id) }
}

// Active timer indicator
menuButton {
    dynamicTheme {
        if (timerActive()) SelectedSemantic
        else null
    }
    icon(Icon.timer, "Timers")
    opensMenu { /* ... */ }
}
```

**Common semantics for dynamic theming:**
- `SelectedSemantic` - Highlight active/selected items
- `NotRelevantSemantic` - Dim irrelevant items
- `null` - Use default theme

## Custom Icon Extensions

```kotlin
// In a centralized Icons.kt file
val Icon.Companion.bug: Icon
    get() = Icon(
        width = 1.5.rem, height = 1.5.rem,
        viewBoxMinX = 0, viewBoxMinY = -960,
        viewBoxWidth = 960, viewBoxHeight = 960,
        pathDatas = listOf("M480-200q66 0 113-47t47-113...")
    )

val Icon.Companion.feature: Icon
    get() = Icon(
        width = 1.5.rem, height = 1.5.rem,
        viewBoxMinX = 0, viewBoxMinY = -960,
        viewBoxWidth = 960, viewBoxHeight = 960,
        pathDatas = listOf("m320-240 160-122 160 122...")
    )

// Usage
icon(Icon.bug, "Debug")
icon(Icon.feature, "Feature Request")
```

## Best Practices

1. **Use semantic themes** (`important`, `danger`) over direct colors
2. **Apply themes to containers**, not individual elements
3. **Let themes cascade** from parent to children
4. **Use `card` for explicit backgrounds** when needed
5. **Keep view hierarchy shallow** to minimize theme recalculations
