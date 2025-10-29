package com.lightningkite.kiteui.models

import kotlin.math.abs
import kotlin.math.absoluteValue

fun Theme.Companion.flat2(
    id: String,
    hue: Angle,
    accentHue: Angle = hue + Angle.halfTurn,
    saturation: Float = 0.7f,
    baseBrightness: Float = 0.1f,
    brightnessStep: Float = 0.05f,
    title: FontAndStyle = FontAndStyle(),
    body: FontAndStyle = FontAndStyle(),
) = Theme(
    id = id,
    font = body,
    elevation = 0.dp,
    cornerRadii = CornerRadii.RatioOfSpacing(0.8f),
    gap = 0.75.rem,
    outlineWidth = 0.px,
    foreground = if(baseBrightness > 0.6f) Color.black else Color.white,
    background = HSPColor(hue = hue, saturation = saturation, brightness = baseBrightness).toRGB(),
    outline = HSPColor(hue = hue, saturation = saturation, brightness = 0.4f).toRGB(),
    derivations = mapOf(
        HeaderSemantic to {
            it.withoutBack(font = title)
        },
        ImportantSemantic to {
            val existing = it.background.closestColor().toHSP()
            if(abs(existing.brightness - 0.5f) > brightnessStep * 3) {
                val b = existing.copy(brightness = 0.5f).toRGB()
                it.withBack(
                    foreground = b.highlight(1f),
                    background = b,
                    outline = b,
                )
            } else {
                val closerToAccent = (existing.hue angleTo hue).turns.absoluteValue > (existing.hue angleTo accentHue).turns.absoluteValue
                val b = HSPColor(hue = if(closerToAccent) hue else accentHue, saturation = saturation, brightness = 0.5f).toRGB()
                it.withBack(
                    foreground = b.highlight(1f),
                    background = b,
                    outline = b,
                )
            }
        },
        CardSemantic to {
            it.withBack(
                background = it.background.closestColor().toHSP().let {
                    it.copy(brightness = it.brightness + brightnessStep)
                }.toRGB(),
                outline = it.outline.closestColor().toHSP().let {
                    it.copy(brightness = it.brightness + brightnessStep)
                }.toRGB()
            )
        },
        HoverSemantic to {
            it.withBack(background = it.background.closestColor().toHSP().let {
                it.copy(brightness = it.brightness + brightnessStep)
            }.toRGB(), outline = it.outline.closestColor().toHSP().let {
                it.copy(brightness = it.brightness + brightnessStep)
            }.toRGB(), outlineWidth = it.outlineWidth * 2)
        },
        FocusSemantic to {
            val o = it.outline.closestColor()
            val b = it.background.closestColor()
            if(b.alpha == 0f || abs(o.perceivedBrightness - b.perceivedBrightness) > 0.4) {
                it.withBack(
                    outlineWidth = it.outlineWidth + 3.dp,
                )
            } else {
                it.withBack(
                    outlineWidth = it.outlineWidth + 3.dp,
                    outline = Color.gray(baseBrightness).highlight(1f)
                )
            }
        },
        DownSemantic to {
            it.withBack(background = it.background.closestColor().toHSP().let {
                it.copy(brightness = it.brightness + brightnessStep * 3)
            }.toRGB(), outline = it.outline.closestColor().toHSP().let {
                it.copy(brightness = it.brightness + brightnessStep * 3)
            }.toRGB(), outlineWidth = it.outlineWidth * 2)
        },

        FieldSemantic to {
            it.withBack(
                outlineWidth = 1.px,
                background = it.background.closestColor(),
                cascading = false,
//                gap = it.gap / 2,
                cornerRadii = when(val base = it.cornerRadii) {
                    is CornerRadii.Constant -> CornerRadii.ForceConstant(base.value)
                    is CornerRadii.ForceConstant -> base
                    is CornerRadii.RatioOfSize -> base
                    is CornerRadii.RatioOfSpacing -> CornerRadii.ForceConstant(it.gap * base.value)
                    is CornerRadii.PerCorner -> base
                }
            )
        },

        ListSemantic to {
            it.withoutBack(gap = 2.dp, cascading = false)
        },

        BarSemantic to { it[MainContentSemantic] },
        NavSemantic to {
            it.withBack(
                cascading = false,
                cornerRadii = CornerRadii.Constant(0.px),
                padding = Edges(0.px)
            )
        },
        OuterSemantic to { it.withBack(cascading = false, gap = 1.px, padding = Edges.ZERO, background = Color.gray(0.3f)) },
        MainContentSemantic to { it.withBack(cascading = false, cornerRadii = CornerRadii.Constant(0.px)) },

        DialogSemantic to {
            it.withBack(outlineWidth = 1.dp, padding = Edges(2.rem), cascading = false)
        },
    ),
)