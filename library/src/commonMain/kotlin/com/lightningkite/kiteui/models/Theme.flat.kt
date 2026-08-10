package com.lightningkite.kiteui.models

import kotlin.math.abs
import kotlin.math.absoluteValue

public fun Theme.Companion.flat(
    id: String,
    hue: Angle,
    accentHue: Angle = hue + Angle.halfTurn,
    saturation: Float = 0.7f,
    baseBrightness: Float = 0.1f,
    brightnessStep: Float = 0.05f,
    title: FontAndStyle = FontAndStyle(),
    body: FontAndStyle = FontAndStyle(),
): Theme = Theme(
    id = id,
    font = body,
    elevation = 0.dp,
    cornerRadii = CornerRadii.RatioOfSpacing(0.8f),
    gap = 0.75.rem,
    outlineWidth = 0.px,
    foreground = if(baseBrightness > 0.6f) Color.black else Color.white,
    background = HSPColor(hue = hue, saturation = saturation, brightness = baseBrightness).toRGB(),
    outline = HSPColor(hue = hue, saturation = saturation, brightness = 0.4f).toRGB(),
    semanticOverrides = SemanticOverrides(
        HeaderSemantic.override {
            it.withoutBack(font = title)
        },
        ImportantSemantic.override {
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
        CardSemantic.override {
            it.withBack(
                background = it.background.closestColor().toHSP().let {
                    it.copy(brightness = it.brightness + brightnessStep)
                }.toRGB(),
                outline = it.outline.closestColor().toHSP().let {
                    it.copy(brightness = it.brightness + brightnessStep)
                }.toRGB()
            )
        },
//        UnselectedSemantic.override {
//            val existing = it.background.closestColor().toHSP()
//            if(abs(existing.brightness - 0.5f) > brightnessStep * 3) {
//                it.withoutBack
//            } else {
//                it.copy(
//                    id = "uns",
//                    background = it.background.closestColor().copy(alpha = 0f),
//                    foreground = it.outline.closestColor(),
//                    outlineWidth = 1.dp
//                ).withBack
//            }
//        },
//        SelectedSemantic.override {
//            it.copy(id = "sel", background = it.background.closestColor().toHSP().let {
//                it.copy(brightness = it.brightness + brightnessStep * 2)
//            }.toRGB(), outline = it.outline.closestColor().toHSP().let {
//                it.copy(brightness = it.brightness + brightnessStep * 2)
//            }.toRGB(), outlineWidth = it.outlineWidth * 2).withBack
//        },
        HoverSemantic.override {
            it.withBack(background = it.background.closestColor().toHSP().let {
                it.copy(brightness = it.brightness + brightnessStep)
            }.toRGB(), outline = it.outline.closestColor().toHSP().let {
                it.copy(brightness = it.brightness + brightnessStep)
            }.toRGB(), outlineWidth = it.outlineWidth * 2)
        },
        FocusSemantic.override {
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
        DownSemantic.override {
            it.withBack(background = it.background.closestColor().toHSP().let {
                it.copy(brightness = it.brightness + brightnessStep * 3)
            }.toRGB(), outline = it.outline.closestColor().toHSP().let {
                it.copy(brightness = it.brightness + brightnessStep * 3)
            }.toRGB(), outlineWidth = it.outlineWidth * 2)
        },

        FieldSemantic.override {
            it.withBack(
                outlineWidth = 1.px,
                background = it.background.closestColor(),
                cascading = false,
//                gap = it.gap / 2,
                cornerRadii = when(val base = it.cornerRadii) {
                    is CornerRadii.AdaptiveToSpacing -> CornerRadii.Fixed(base.value)
                    is CornerRadii.Fixed -> base
                    is CornerRadii.RatioOfSize -> base
                    is CornerRadii.RatioOfSpacing -> CornerRadii.Fixed(it.gap * base.value)
                    is CornerRadii.PerCorner -> base
                }
            )
        },
        BarSemantic.override { it.withoutBack },
        NavSemantic.override { it[CardSemantic] },
        OuterSemantic.override {
            it.withBack
        },
        MainContentSemantic.override {
            it.withBack(
                background = RadialGradient(
                    stops = listOf(
                        GradientStop(0f, HSPColor(hue = hue, saturation = saturation, brightness = baseBrightness + brightnessStep * 2).toRGB()),
                        GradientStop(0.4f, HSPColor(hue = hue, saturation = saturation, brightness = baseBrightness + brightnessStep).toRGB()),
                        GradientStop(1f, HSPColor(hue = hue, saturation = saturation, brightness = baseBrightness + brightnessStep).toRGB()),
                    ),
                )
            )
        },
        DialogSemantic.override {
            it.withBack(outlineWidth = 1.dp, gap = 2.rem, cascading = false)
        },
    ),
)