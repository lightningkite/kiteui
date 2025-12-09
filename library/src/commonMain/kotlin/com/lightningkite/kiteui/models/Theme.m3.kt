package com.lightningkite.kiteui.models

import kotlin.random.Random

fun Theme.Companion.material3(
    id: String,
    primary: Color = Color.fromHex(0xFF6200EE.toInt()),
    secondary: Color = Color.fromHex(0xFF03DAC6.toInt()),
    primaryForeground: Color = if (primary.perceivedBrightness < 0.6f) Color.white else Color.black,
    secondaryForeground: Color = if (secondary.perceivedBrightness < 0.6f) Color.white else Color.black,
    foreground: Paint = Color.black,
    backgroundAdjust: Float = 0.1f,
    background: Paint = Color.interpolate(foreground.closestColor().invert(), primary, backgroundAdjust),
    title: FontAndStyle = FontAndStyle(systemDefaultFont),
    body: FontAndStyle = FontAndStyle(systemDefaultFont),
    elevation: Dimension = 1.dp,
    cornerRadii: CornerRadii = CornerRadii.RatioOfSpacing(1f),
    gap: Dimension = 1.rem,
    outline: Paint = background.closestColor().highlight(0.1f),
    outlineWidth: Dimension = 0.px,
) = Theme(
    id = id,
    font = body,
    elevation = elevation,
    cornerRadii = cornerRadii,
    gap = gap,
    outline = outline,
    outlineWidth = outlineWidth,
    foreground = foreground,
    background = background,
    derivations = mapOf(
        OuterSemantic to {
            it.alter(
                cascading = false,
                gap = 0.px,
                cornerRadii = CornerRadii.ForceConstant(0.px),
                outlineWidth = 0.px,
            ).withBackNoPadding
        },
        MainContentSemantic to {
            it.withBack(
                cascading = false,
                cornerRadii = CornerRadii.ForceConstant(0.px),
                outlineWidth = 0.px,
            )
        },
        BarSemantic to {
            it
                .withBack(
                    cascading = false,
                    cornerRadii = CornerRadii.ForceConstant(0.px),
                    outlineWidth = 0.px,
                )

        },
        HeaderSemantic to {
            it.withoutBack(font = title)
        },
        ImportantSemantic to {
            it.withBack(
                foreground = primaryForeground,
                background = primary,
                outline = primary.highlight(0.1f),
                derivations = mapOf(ImportantSemantic to {
                    it.withBack(
                        foreground = secondaryForeground,
                        background = secondary,
                        outline = secondary.highlight(0.1f),
                    )
                }
                )
            )
        },
        CriticalSemantic to {
            it.withBack(
                foreground = secondaryForeground,
                background = secondary,
                outline = secondary.highlight(0.1f),
            )
        },
    ),
)

object M3Theme {
    operator fun invoke(
        id: String,
        primary: Color = Color.fromHex(0xFF6200EE.toInt()),
        secondary: Color = Color.fromHex(0xFF03DAC6.toInt()),
        primaryForeground: Color = if (primary.perceivedBrightness < 0.6f) Color.white else Color.black,
        secondaryForeground: Color = if (secondary.perceivedBrightness < 0.6f) Color.white else Color.black,
        foreground: Paint = Color.black,
        backgroundAdjust: Float = 0.1f,
        background: Paint = Color.interpolate(foreground.closestColor().invert(), primary, backgroundAdjust),
        title: FontAndStyle = FontAndStyle(systemDefaultFont),
        body: FontAndStyle = FontAndStyle(systemDefaultFont),
        elevation: Dimension = 1.dp,
        cornerRadii: CornerRadii = CornerRadii.RatioOfSpacing(1f),
        gap: Dimension = 1.rem,
        outline: Paint = background.closestColor().highlight(0.1f),
        outlineWidth: Dimension = 0.px,
    ) = Theme.material3(
        id = id,
        primary = primary,
        secondary = secondary,
        primaryForeground = primaryForeground,
        secondaryForeground = secondaryForeground,
        foreground = foreground,
        backgroundAdjust = backgroundAdjust,
        background = background,
        title = title,
        body = body,
        elevation = elevation,
        cornerRadii = cornerRadii,
        gap = gap,
        outline = outline,
        outlineWidth = outlineWidth,
    )

    fun randomLight(): Theme {
        val hue = Random.nextFloat().turns
        val saturation = Random.nextFloat() * 0.5f + 0.25f
        val value = Random.nextFloat() * 0.5f + 0.25f
        return this(
            id = "m3RandomLight-${Random.nextInt()}",
            primary = HSVColor(hue = hue, saturation = saturation, value = value).toRGB(),
            secondary = HSVColor(hue = hue + Angle.halfTurn, saturation = 1f - saturation, value = 1f - value).toRGB(),
            backgroundAdjust = Random.nextFloat() * 0.15f,
        )
    }

    fun randomDark(): Theme {
        val hue = Random.nextFloat().turns
        val saturation = Random.nextFloat() * 0.5f + 0.25f
        val value = Random.nextFloat() * 0.5f + 0.25f
        return this(
            id = "m3RandomDark-${Random.nextInt()}",
            foreground = Color.white,
            backgroundAdjust = Random.nextFloat() * 0.5f,
            primary = HSVColor(hue = hue, saturation = saturation, value = value).toRGB(),
            secondary = HSVColor(hue = hue + Angle.halfTurn, saturation = 1f - saturation, value = 1f - value).toRGB(),
        )
    }

    fun random(): Theme = if (Random.nextBoolean()) randomLight() else randomDark()
}

