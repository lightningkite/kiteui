package com.lightningkite.kiteui.models

import kotlin.random.Random

fun Theme.Companion.material(
    id: String,
    foreground: Paint = Color.black,
    background: Paint = Color.white,
    primary: Color = Color.fromHex(0xFF6200EE.toInt()),
    secondary: Color = Color.fromHex(0xFF03DAC6.toInt()),
    primaryForeground: Color = if (primary.perceivedBrightness < 0.6f) Color.white else Color.black,
    secondaryForeground: Color = if (secondary.perceivedBrightness < 0.6f) Color.white else Color.black,
    title: FontAndStyle = FontAndStyle(systemDefaultFont),
    body: FontAndStyle = FontAndStyle(systemDefaultFont),
    elevation: Dimension = 1.dp,
    cornerRadii: CornerRadii = CornerRadii.RatioOfSpacing(1f),
    spacing: Dimension = 1.rem,
    outline: Paint = background.closestColor().highlight(0.1f),
    outlineWidth: Dimension = 0.dp,
) = Theme(
    id = id,
    title = title,
    body = body,
    elevation = elevation,
    cornerRadii = cornerRadii,
    spacing = spacing,
    outline = outline,
    outlineWidth = outlineWidth,
    foreground = foreground,
    background = background,
    dialog = themeDeriver {
        copy(
            id = "dlg",
            background = this.background.closestColor().darken(0.1f),
            outline = this.outline.closestColor().darken(0.1f),
            elevation = this.elevation * 2f,
        )
    },
    important = themeDeriver {
        copy(
            id = "imp",
            foreground = primaryForeground,
            background = primary,
            outline = primary.highlight(0.1f),
            important = themeDeriver {
                copy(
                    id = "imp2",
                    foreground = secondaryForeground,
                    background = secondary,
                    outline = secondary.highlight(0.1f),
                )
            }
        )
    },
    bar = themeDeriver {
        copy(
            id = "bar",
            foreground = primaryForeground,
            background = primary,
            outline = primary.highlight(0.1f),
            important = themeDeriver {
                copy(
                    id = "barimp",
                    foreground = secondaryForeground,
                    background = secondary,
                    outline = secondary.highlight(0.1f),
                )
            }
        )
    },
    critical = themeDeriver {
        copy(
            id = "crt",
            foreground = secondaryForeground,
            background = secondary,
            outline = secondary.highlight(0.1f),
        )
    },
)

@Deprecated("Use Theme.material instead")
object MaterialLikeTheme {
    operator fun invoke(
        id: String,
        foreground: Paint = Color.black,
        background: Paint = Color.white,
        primary: Color = Color.fromHex(0xFF6200EE.toInt()),
        secondary: Color = Color.fromHex(0xFF03DAC6.toInt()),
        primaryForeground: Color = if (primary.perceivedBrightness < 0.6f) Color.white else Color.black,
        secondaryForeground: Color = if (secondary.perceivedBrightness < 0.6f) Color.white else Color.black,
        title: FontAndStyle = FontAndStyle(systemDefaultFont),
        body: FontAndStyle = FontAndStyle(systemDefaultFont),
        elevation: Dimension = 1.dp,
        cornerRadii: CornerRadii = CornerRadii.RatioOfSpacing(1f),
        spacing: Dimension = 1.rem,
        outline: Paint = background.closestColor().highlight(0.1f),
        outlineWidth: Dimension = 0.dp,
    ) = Theme.material(
        id = id,
        foreground = foreground,
        background = background,
        primary = primary,
        secondary = secondary,
        primaryForeground = primaryForeground,
        secondaryForeground = secondaryForeground,
        title = title,
        body = body,
        elevation = elevation,
        cornerRadii = cornerRadii,
        spacing = spacing,
        outline = outline,
        outlineWidth = outlineWidth
    )

    fun randomLight(): Theme {
        val hue = Random.nextFloat().turns
        val saturation = Random.nextFloat() * 0.5f + 0.25f
        val value = Random.nextFloat() * 0.5f + 0.25f
        return Theme.material(
            id = "materialRandomLight-${Random.nextInt()}",
            primary = HSVColor(hue = hue, saturation = saturation, value = value).toRGB(),
            secondary = HSVColor(hue = hue + Angle.halfTurn, saturation = 1f - saturation, value = 1f - value).toRGB(),
        )
    }

    fun randomDark(): Theme {
        val hue = Random.nextFloat().turns
        val saturation = Random.nextFloat() * 0.5f + 0.25f
        val value = Random.nextFloat() * 0.5f + 0.25f
        return Theme.material(
            id = "materialRandomDark-${Random.nextInt()}",
            foreground = Color.white,
            background = Color.gray(0.2f),
            primary = HSVColor(hue = hue, saturation = saturation, value = value).toRGB(),
            secondary = HSVColor(hue = hue + Angle.halfTurn, saturation = 1f - saturation, value = 1f - value).toRGB(),
        )
    }

    fun random(): Theme = if (Random.nextBoolean()) randomLight() else randomDark()
}

fun Theme.randomTitleFontSettings() = copy(
    id = "${Random.nextInt()}",
    title = title.copy(
        font = systemDefaultFont,
        weight = if (Random.nextBoolean()) 700 else 500,
        allCaps = Random.nextBoolean()
    )
)

fun Theme.randomElevationAndCorners() = when (Random.nextInt(0, 3)) {
    0 -> copy(
        id = "${Random.nextInt()}",
        elevation = Random.nextInt(2, 4).dp,
        cornerRadii = CornerRadii.RatioOfSpacing(Random.nextFloat())
    )

    1 -> copy(
        id = "${Random.nextInt()}",
        outlineWidth = Random.nextInt(1, 4).dp,
        cornerRadii = CornerRadii.RatioOfSpacing(Random.nextFloat())
    )

    else -> copy(
        id = "${Random.nextInt()}",
        outlineWidth = Random.nextInt(1, 4).dp,
        elevation = Random.nextInt(2, 4).dp,
        cornerRadii = CornerRadii.RatioOfSpacing(Random.nextFloat())
    )
}