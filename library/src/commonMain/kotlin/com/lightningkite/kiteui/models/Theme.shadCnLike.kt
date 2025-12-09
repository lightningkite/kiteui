package com.lightningkite.kiteui.models

import kotlin.time.Duration.Companion.seconds

fun Theme.Companion.shadCnLike(
    id: String,
    background: Color = Color.gray(0.05f),
    accent: Color = HSPColor(hue = 0.6.turns, saturation = 0.8f, brightness = 0.3f).toRGB(),
    title: FontAndStyle = FontAndStyle(),
    body: FontAndStyle = FontAndStyle(),
): Theme {
    val baseBrightness = background.perceivedBrightness
    val outlineHighlight = 0.1f
    val cardHighlight = 0.05f
    val superAccent = accent.toHSP().let { it.copy(hue = it.hue + 0.5.turns) }.toRGB()
    val backHue = accent.toHSP().hue
    val backSaturation = 0.2f
    fun Paint.brighten2(amount: Float): Color {
        return this.closestColor().toHSP().let {
            it.copy(
                hue = backHue,
                saturation = backSaturation,
                brightness = it.brightness + amount
            )
        }.toRGB()
    }
    fun Paint.highlight2(amount: Float): Color {
        return this.closestColor().toHSP().let {
            it.copy(
                hue = backHue,
                saturation = backSaturation,
                brightness = it.brightness + amount * (if(it.brightness > 0.5f) -1 else 1)
            )
        }.toRGB()
    }
    fun Paint.foreground(): Color {
        return if(closestColor().perceivedBrightness > 0.7f) Color.black else Color.white
    }
    return Theme(
        id = id,
        font = body,
        elevation = 0.dp,
        cornerRadii = CornerRadii.Constant(0.75.rem),
        gap = 0.75.rem,
        padding = Edges(0.75.rem),
        outlineWidth = 0.px,
        transitionDuration = 0.1.seconds,
        foreground = if (baseBrightness > 0.6f) Color.black else Color.white,
        background = background,
        outline = background.highlight(outlineHighlight),
        derivations = mapOf(
            HeaderSemantic to {
                it.withoutBack(font = title)
            },
            ImportantSemantic to {
                when(it.background) {
                    accent -> it.withBack(
                        background = superAccent,
                        foreground = superAccent.foreground(),
                    )
                    else -> it.withBack(
                        background = accent,
                        foreground = accent.foreground(),
                    )
                }
            },
            CardSemantic to {
                it.alter(
                    background = it.background.brighten2(cardHighlight),
                    outline = it.outline.brighten2(cardHighlight),
                ).withBack(
                    cascading = false,
                    outlineWidth = 1.px,
                )
            },
            GroupSemantic to {
                it.withBack(
                    cascading = false,
                    outlineWidth = 1.px,
                )
            },
            HoverSemantic to {
                it.withBack(
                    background = it.background.brighten2(cardHighlight),
                    outline = it.outline.brighten2(cardHighlight),
                )
            },
            FocusSemantic to {
                it.withBack(
                    cascading = false,
                    outlineWidth = 3.px,
                )
            },
            DownSemantic to {
                it.withBack(
                    background = it.background.brighten2(cardHighlight),
                    outline = it.outline.brighten2(cardHighlight),
                )
            },

            FieldSemantic to { it[CardSemantic] },

            ListSemantic to {
                it.withoutBack(gap = 1.px, cascading = false)
            },

            BarSemantic to {
                it.withBack(
                    background = background.highlight2(cardHighlight),
                    cascading = false,
                    outlineWidth = 1.px,
                    cornerRadii = CornerRadii.Constant(0.px),
                    padding = Edges(0.px)
                )
            },
            NavSemantic to {
                it.withBack(
                    background = background.highlight2(cardHighlight),
                    cascading = false,
                    outlineWidth = 1.px,
                    cornerRadii = CornerRadii.Constant(0.px),
                    padding = Edges(0.px)
                )
            },
            OuterSemantic to {
                it.withBack(
                    cascading = false,
                    outlineWidth = 1.px,
                    cornerRadii = CornerRadii.Constant(0.px),
                    padding = Edges(0.px)
                )
            },
            MainContentSemantic to { it.withBack(cascading = false, cornerRadii = CornerRadii.Constant(0.px)) },

            DialogSemantic to {
                it.withBack(outlineWidth = 1.dp, padding = Edges(2.rem), cascading = false)
            },

            // TODO: Selected / Unselected semantics
        ),
    )
}