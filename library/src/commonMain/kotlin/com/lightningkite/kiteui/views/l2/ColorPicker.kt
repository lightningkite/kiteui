package com.lightningkite.kiteui.views.l2

import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.dynamicTheme
import com.lightningkite.kiteui.views.expanding
import com.lightningkite.kiteui.views.fieldTheme
import com.lightningkite.reactive.context.invoke
import com.lightningkite.reactive.context.reactive
import com.lightningkite.reactive.context.reactiveSuspending
import com.lightningkite.reactive.core.Constant
import com.lightningkite.reactive.core.MutableReactive
import com.lightningkite.reactive.core.MutableRemember
import com.lightningkite.reactive.core.ReactiveWithMutableValue
import com.lightningkite.reactive.core.Signal
import com.lightningkite.reactive.extensions.debounce
import com.lightningkite.reactive.extensions.value
import kotlin.math.abs


fun ViewWriter.colorPicker(color: MutableReactive<Color>) {
    col {
        val selectedType = Signal(ColorPickerOptions.HSP)
        val debounced = color.debounce(100)

        sizeConstraints(minWidth = 12.rem).row {
            expanding.fieldTheme.select {
                this.bind(
                    edits = selectedType,
                    data = Constant(ColorPickerOptions.entries),
                    render = { it.toString() }
                )
            }
            expanding.themed(ImportantSemantic).text {
                dynamicTheme {
                    ThemeDerivation {
                        it.copy(
                            id = "color_${debounced().toInt()}",
                            background = debounced(),
                            foreground = debounced().highlight(1f),
                        ).withBack
                    }
                }
                ::content { color().toAlphalessWeb() }
            }
        }

        swapView {
            this.swapping(
                current = { selectedType() },
                views = {
                    when (it) {
                        ColorPickerOptions.RGB -> rgbControls(color)
                        ColorPickerOptions.HSP -> hspControls(color)
                        ColorPickerOptions.HSL -> hslControls(color)
                        ColorPickerOptions.HSV -> hsvControls(color)
                    }
                }
            )
        }

        listOf(
            listOf(Color.red, Color.orange, Color.yellow, Color.green, Color.teal),
            listOf(Color.blue, Color.purple, Color.white, Color.gray, Color.black),
        ).forEach {
            row {
                it.forEach { c ->
                    themed(ThemeDerivation {
                        it.copy(id = "color_${c.toInt()}", background = c).withBack
                    }).sizeConstraints(width = 1.5.rem, height = 1.5.rem).button {
                        onClick {
                            color.set(c)
                        }
                    }
                }
            }
        }
    }
}

private fun ViewWriter.hslControls(currentColor: MutableReactive<Color>) {
    val hslColor = MutableRemember { HSLColor.fromRGB(currentColor()) }

    bindColor(currentColor, hslColor) { HSLColor.fromRGB(it) }

    col {
        subtext { ::content { "Hue: ${hslColor().hue.degrees.toInt()}°" } }
        slider {
            range(0f, 360f, 1f)
            value bind hslColor.lens(
                get = { it.hue.degrees },
                modify = { a, b -> a.copy(hue = b.degrees) },
            )
        }

        subtext { ::content { "Saturation: ${(hslColor().saturation.times(100)).toInt()}%" } }
        slider {
            range(0f, 1f, 0.01f)
            value bind hslColor.lens(
                get = { it.saturation },
                modify = { a, b -> a.copy(saturation = b) },
            )
        }

        subtext { ::content { "Lightness: ${(hslColor().lightness.times(100)).toInt()}%" } }
        slider {
            range(0f, 1f, 0.01f)
            value bind hslColor.lens(
                get = { it.lightness },
                modify = { a, b -> a.copy(lightness = b) },
            )
        }

        subtext { ::content { "Alpha: ${(hslColor().alpha.times(100)).toInt()}%" } }
        slider {
            range(0f, 1f, 0.01f); value bind hslColor.lens(
            get = { it.alpha },
            modify = { a, b -> a.copy(alpha = b) },
        )
        }
    }
}

private fun ViewWriter.hspControls(color: MutableReactive<Color>) {
    val hspColor = MutableRemember { HSPColor.fromRGB(color()) }

    bindColor(color, hspColor) { HSPColor.fromRGB(it) }

    col {
        subtext { ::content { "Hue: ${hspColor().hue.degrees.toInt()}°" } }
        slider {
            range(0f, 360f, 1f)
            value bind hspColor.lens(
                get = { it.hue.degrees },
                modify = { a, b -> a.copy(hue = b.degrees) },
            )
        }

        subtext { ::content { "Saturation: ${(hspColor().saturation.times(100)).toInt()}%" } }
        slider {
            range(0f, 1f, 0.01f)
            value bind hspColor.lens(
                get = { it.saturation },
                modify = { a, b -> a.copy(saturation = b) },
            )
        }

        subtext { ::content { "Brightness: ${(hspColor().brightness.times(100)).toInt()}%" } }
        slider {
            range(0f, 1f, 0.01f)
            value bind hspColor.lens(
                get = { it.brightness },
                modify = { a, b -> a.copy(brightness = b) },
            )
        }

        subtext { ::content { "Alpha: ${(hspColor().alpha.times(100)).toInt()}%" } }
        slider {
            range(0f, 1f, 0.01f); value bind hspColor.lens(
            get = { it.alpha },
            modify = { a, b -> a.copy(alpha = b) },
        )
        }
    }
}

private fun ViewWriter.rgbControls(color: MutableReactive<Color>) {
    col {
        text { ::content { "Red: ${color().redInt}" } }
        slider {
            range(0f, 1f); value bind color.lens(
            get = { it.red },
            modify = { a, b -> a.copy(red = b) })
        }

        text { ::content { "Green: ${color().greenInt}" } }
        slider {
            range(0f, 1f);
            value bind color.lens(
                get = { it.green },
                modify = { a, b -> a.copy(green = b) }
            )
        }

        text { ::content { "Blue: ${color().blueInt}" } }
        slider {
            range(0f, 1f);
            value bind color.lens(
                get = { it.blue },
                modify = { a, b -> a.copy(blue = b) })
        }

        text { ::content { "Alpha: ${color().alpha.times(100).toInt()}" } }
        slider {
            range(0f, 1f);
            value bind color.lens(
                get = { it.alpha },
                modify = { a, b -> a.copy(alpha = b) }
            )
        }
    }
}

private fun ViewWriter.hsvControls(currentColor: MutableReactive<Color>) {
    val hsvColor = MutableRemember { HSVColor.fromRGB(currentColor()) }
    bindColor(currentColor, hsvColor) { HSVColor.fromRGB(it) }

    col {
        text { ::content { "Hue: ${hsvColor().hue.degrees.toInt()}°" } }
        slider {
            range(0f, 360f)
            value bind hsvColor.lens(
                get = { it.hue.degrees },
                modify = { a, b -> a.copy(hue = b.degrees) },
            )
        }

        text { ::content { "Saturation: ${(hsvColor().saturation.times(100)).toInt()}%" } }
        slider {
            range(0f, 1f)
            value bind hsvColor.lens(
                get = { it.saturation },
                modify = { a, b -> a.copy(saturation = b) },
            )
        }

        text { ::content { "Value: ${(hsvColor().value.times(100)).toInt()}%" } }
        slider {
            range(0f, 1f)
            value bind hsvColor.lens(
                get = { it.value },
                modify = { a, b -> a.copy(value = b) },
            )
        }

        text { ::content { "Alpha: ${(hsvColor().alpha.times(100)).toInt()}%" } }
        slider {
            range(0f, 1f); value bind hsvColor.lens(
            get = { it.alpha },
            modify = { a, b -> a.copy(alpha = b) },
        )
        }
    }
}

/**
 * Since there's so much rounding with converting rgb to other types, only change
 * if there is sufficient difference between color values.
 */
private fun <V : ColorSpace, T : ReactiveWithMutableValue<V>> ViewWriter.bindColor(
    currentColor: MutableReactive<Color>,
    other: T,
    fromColor: (Color) -> V
) {
    reactiveSuspending {
        currentColor.set(other().toRGB())
    }
    reactive {
        if (other.state.ready) {
            val parentColor = currentColor()
            val local = other.state.raw.toRGB()
            val tolerance = 0.1f

            val isDifferent =
                abs(parentColor.green - local.green) > tolerance ||
                        abs(parentColor.red - local.red) > tolerance ||
                        abs(parentColor.blue - local.blue) > tolerance ||
                        abs(parentColor.green - local.green) > tolerance

            if (isDifferent) {
                other.value = fromColor(parentColor)
            }
        }
    }
}


private enum class ColorPickerOptions {
    RGB,
    HSP,
    HSL,
    HSV;
}
