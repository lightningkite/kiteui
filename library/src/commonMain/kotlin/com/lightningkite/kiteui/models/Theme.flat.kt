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
    title = title,
    body = body,
    elevation = 0.dp,
    cornerRadii = CornerRadii.RatioOfSpacing(0.8f),
    spacing = 0.75.rem,
    navSpacing = 1.rem,
    outlineWidth = 0.px,
    foreground = if(baseBrightness > 0.6f) Color.black else Color.white,
    background = HSPColor(hue = hue, saturation = saturation, brightness = baseBrightness).toRGB(),
    outline = HSPColor(hue = hue, saturation = saturation, brightness = 0.4f).toRGB(),
    important = {
        val existing = background.closestColor().toHSP()
        if(abs(existing.brightness - 0.5f) > brightnessStep * 3) {
            val b = existing.copy(brightness = 0.5f).toRGB()
            copy(
                id = "imp",
                foreground = b.highlight(1f),
                background = b,
                outline = b,
            )
        } else {
            val closerToAccent = (existing.hue angleTo hue).turns.absoluteValue > (existing.hue angleTo accentHue).turns.absoluteValue
            val b = HSPColor(hue = if(closerToAccent) hue else accentHue, saturation = saturation, brightness = 0.5f).toRGB()
            copy(
                id = "imp",
                foreground = b.highlight(1f),
                background = b,
                outline = b,
            )
        }
    },
    dialog = { card() },
    card = {
        copy(id = "crd", background = this.background.closestColor().toHSP().let {
            it.copy(brightness = it.brightness + brightnessStep)
        }.toRGB(), outline = this.outline.closestColor().toHSP().let {
            it.copy(brightness = it.brightness + brightnessStep)
        }.toRGB() )
    },
    unselected = {
        val existing = background.closestColor().toHSP()
        if(abs(existing.brightness - 0.5f) > brightnessStep * 3) {
            this
        } else {
            copy(
                id = "uns",
                background = Color.transparent,
                outlineWidth = 1.dp
            )
        }
    },
    selected = {
        copy(id = "sel", background = this.background.closestColor().toHSP().let {
            it.copy(brightness = it.brightness + brightnessStep * 2)
        }.toRGB(), outline = this.outline.closestColor().toHSP().let {
            it.copy(brightness = it.brightness + brightnessStep * 2)
        }.toRGB(), outlineWidth = outlineWidth * 2)
    },
    hover = {
        copy(id = "hov", background = this.background.closestColor().toHSP().let {
            it.copy(brightness = it.brightness + brightnessStep)
        }.toRGB(), outline = this.outline.closestColor().toHSP().let {
            it.copy(brightness = it.brightness + brightnessStep)
        }.toRGB(), outlineWidth = outlineWidth * 2)
    },
    focus = {
        copy(
            id = "fcs",
            outlineWidth = outlineWidth + 2.dp
        )
    },
    down = {
        copy(id = "dwn", background = this.background.closestColor().toHSP().let {
            it.copy(brightness = it.brightness + brightnessStep * 3)
        }.toRGB(), outline = this.outline.closestColor().toHSP().let {
            it.copy(brightness = it.brightness + brightnessStep * 3)
        }.toRGB(), outlineWidth = outlineWidth * 2)
    },

    field = {
        copy(
            id = "fld",
            outlineWidth = 1.px,
            background = background.closestColor(),
        )
    },
    bar = { null },
    nav = { card() },
    mainContent = {
        copy(
            id = "con",
            background = RadialGradient(
                stops = listOf(
                    GradientStop(0f, HSPColor(hue = hue, saturation = saturation, brightness = baseBrightness + brightnessStep * 2).toRGB()),
                    GradientStop(0.4f, HSPColor(hue = hue, saturation = saturation, brightness = baseBrightness + brightnessStep).toRGB()),
                    GradientStop(1f, HSPColor(hue = hue, saturation = saturation, brightness = baseBrightness + brightnessStep).toRGB()),
                ),
            )
        )
    }
)