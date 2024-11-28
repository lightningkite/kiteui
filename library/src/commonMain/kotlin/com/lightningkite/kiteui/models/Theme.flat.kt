package com.lightningkite.kiteui.models

import kotlin.math.abs
import kotlin.math.absoluteValue

fun Theme.Companion.flat(
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
    spacing = 0.75.rem,
    navSpacing = 1.rem,
    outlineWidth = 0.px,
    foreground = if(baseBrightness > 0.6f) Color.black else Color.white,
    background = HSPColor(hue = hue, saturation = saturation, brightness = baseBrightness).toRGB(),
    outline = HSPColor(hue = hue, saturation = saturation, brightness = 0.4f).toRGB(),
    derivations = mapOf(
        ImportantSemantic to {
            val existing = it.background.closestColor().toHSP()
            if(abs(existing.brightness - 0.5f) > brightnessStep * 3) {
                val b = existing.copy(brightness = 0.5f).toRGB()
                it.copy(
                    id = "imp",
                    foreground = b.highlight(1f),
                    background = b,
                    outline = b,
                )
            } else {
                val closerToAccent = (existing.hue angleTo hue).turns.absoluteValue > (existing.hue angleTo accentHue).turns.absoluteValue
                val b = HSPColor(hue = if(closerToAccent) hue else accentHue, saturation = saturation, brightness = 0.5f).toRGB()
                it.copy(
                    id = "imp",
                    foreground = b.highlight(1f),
                    background = b,
                    outline = b,
                )
            }.withBack
        },
        CardSemantic to {
            it.copy(
                id = "crd",
                background = it.background.closestColor().toHSP().let {
                    it.copy(brightness = it.brightness + brightnessStep)
                }.toRGB(),
                outline = it.outline.closestColor().toHSP().let {
                    it.copy(brightness = it.brightness + brightnessStep)
                }.toRGB()
            ).withBack
        },
        UnselectedSemantic to {
            val existing = it.background.closestColor().toHSP()
            if(abs(existing.brightness - 0.5f) > brightnessStep * 3) {
                it.withoutBack
            } else {
                it.copy(
                    id = "uns",
                    background = it.background.closestColor().copy(alpha = 0f),
                    foreground = it.outline.closestColor(),
                    outlineWidth = 1.dp
                ).withBack
            }
        },
        SelectedSemantic to {
            it.copy(id = "sel", background = it.background.closestColor().toHSP().let {
                it.copy(brightness = it.brightness + brightnessStep * 2)
            }.toRGB(), outline = it.outline.closestColor().toHSP().let {
                it.copy(brightness = it.brightness + brightnessStep * 2)
            }.toRGB(), outlineWidth = it.outlineWidth * 2).withBack
        },
        HoverSemantic to {
            it.copy(id = "hov", background = it.background.closestColor().toHSP().let {
                it.copy(brightness = it.brightness + brightnessStep)
            }.toRGB(), outline = it.outline.closestColor().toHSP().let {
                it.copy(brightness = it.brightness + brightnessStep)
            }.toRGB(), outlineWidth = it.outlineWidth * 2).withBack
        },
        FocusSemantic to {
            val o = it.outline.closestColor()
            val b = it.background.closestColor()
            if(b.alpha == 0f || abs(o.perceivedBrightness - b.perceivedBrightness) > 0.4) {
                it.copy(
                    id = "fcs",
                    outlineWidth = it.outlineWidth + 3.dp,
                )
            } else {
                it.copy(
                    id = "fcs",
                    outlineWidth = it.outlineWidth + 3.dp,
                    outline = Color.gray(baseBrightness).highlight(1f)
                )
            }.withBack
        },
        DownSemantic to {
            it.copy(id = "dwn", background = it.background.closestColor().toHSP().let {
                it.copy(brightness = it.brightness + brightnessStep * 3)
            }.toRGB(), outline = it.outline.closestColor().toHSP().let {
                it.copy(brightness = it.brightness + brightnessStep * 3)
            }.toRGB(), outlineWidth = it.outlineWidth * 2).withBack
        },

        FieldSemantic to {
            it.copy(
                id = "fld",
                outlineWidth = 1.px,
                background = it.background.closestColor(),
                revert = true,
//                spacing = it.spacing / 2,
                cornerRadii = when(val base = it.cornerRadii) {
                    is CornerRadii.Constant -> CornerRadii.ForceConstant(base.value)
                    is CornerRadii.ForceConstant -> base
                    is CornerRadii.RatioOfSize -> base
                    is CornerRadii.RatioOfSpacing -> CornerRadii.ForceConstant(it.spacing * base.value)
                    is CornerRadii.PerCorner -> base
                }
            ).withBack
        },
        BarSemantic to { it.withoutBack },
        NavSemantic to { it[CardSemantic] },
        MainContentSemantic to {
            it.copy(
                id = "con",
                background = RadialGradient(
                    stops = listOf(
                        GradientStop(0f, HSPColor(hue = hue, saturation = saturation, brightness = baseBrightness + brightnessStep * 2).toRGB()),
                        GradientStop(0.4f, HSPColor(hue = hue, saturation = saturation, brightness = baseBrightness + brightnessStep).toRGB()),
                        GradientStop(1f, HSPColor(hue = hue, saturation = saturation, brightness = baseBrightness + brightnessStep).toRGB()),
                    ),
                )
            ).withBack
        },
        DialogSemantic to {
            it.copy(outlineWidth = 1.dp, spacing = 2.rem, revert = true).withBack
        },
    ),
)