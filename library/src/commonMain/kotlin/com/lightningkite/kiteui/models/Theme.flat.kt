package com.lightningkite.kiteui.models

fun Theme.Companion.flat(
    hue: Angle,
    accentHue: Angle = hue + Angle.halfTurn,
    saturation: Float = 0.7f,
    baseBrightness: Float = 0.1f,
    brightnessStep: Float = 0.05f,
    title: FontAndStyle = FontAndStyle(),
    body: FontAndStyle = FontAndStyle(),
) = Theme(
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
        val b = HSPColor(hue = hue, saturation = saturation, brightness = 0.5f).toRGB()
        copy(
            foreground = b.highlight(1f),
            background = b
        )
    },
    critical = {
        val b = HSPColor(hue = accentHue, saturation = saturation, brightness = 0.5f).toRGB()
        copy(
            foreground = b.highlight(1f),
            background = b
        )
    },
    dialog = { card() },
    card = {
        copy(background = this.background.closestColor().toHSP().let {
            it.copy(brightness = it.brightness + brightnessStep)
        }.toRGB())
    },
    selected = {
        copy(background = this.background.closestColor().toHSP().let {
            it.copy(brightness = it.brightness + brightnessStep * 2, saturation = saturation + 0.3f)
        }.toRGB())
    },
    hover = {
        copy(background = this.background.closestColor().toHSP().let {
            it.copy(brightness = it.brightness + brightnessStep)
        }.toRGB())
    },
    focus = {
        copy(
            outlineWidth = outlineWidth + 2.dp
        )
    },
    down = {
        copy(background = this.background.closestColor().toHSP().let {
            it.copy(brightness = it.brightness + brightnessStep * 3)
        }.toRGB())
    },

    field = {
        copy(
            outlineWidth = 1.px,
        )
    },
    bar = { null },
    nav = { card() },
    mainContent = {
        copy(
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