package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.card
import com.lightningkite.kiteui.views.centered
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.dynamicTheme
import com.lightningkite.reactive.context.reactive
import com.lightningkite.reactive.core.Reactive
import com.lightningkite.reactive.core.Signal
import com.lightningkite.reactive.extensions.debounce
import com.lightningkite.reactive.lensing.lens

@Routable("color-test")
object ColorTestPage : Page {
    override fun ViewWriter.render() {
        scrolling.col {
            centered.sizeConstraints(width = 48.rem).col {
                h1 { content = "Color Test" }

                card.col {
                    val color = Signal(Color.red)
                    h2 { content = "RGB Controls" }
                    row {
                        sizeConstraints(width = 12.rem).col {
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

                            text { ::content { "Alpha: ${color().alpha}" } }
                            slider {
                                range(0f, 1f);
                                value bind color.lens(
                                    get = { it.alpha },
                                    modify = { a, b -> a.copy(alpha = b) })
                            }
                        }
                        space()

                        renderColorSection(color)
                    }
                }
                card.col {
                    val hslColor = Signal(
                        HSLColor(
                            alpha = 1.0f,
                            hue = 36.degrees,
                            saturation = 0.64f,
                            lightness = 0.8f
                        )
                    )

                    h2 { content = "HSL Controls" }
                    row {
                        sizeConstraints(width = 12.rem).col {
                            text { ::content { "Hue: ${hslColor().hue.degrees.toInt()}°" } }
                            slider {
                                range(0f, 360f)
                                value bind hslColor.lens(
                                    get = { it.hue.degrees },
                                    modify = { a, b -> a.copy(hue = b.degrees) },
                                )
                            }

                            text { ::content { "Saturation: ${(hslColor().saturation.times(100)).toInt()}%" } }
                            slider {
                                range(0f, 1f)
                                value bind hslColor.lens(
                                    get = { it.saturation },
                                    modify = { a, b -> a.copy(saturation = b) },
                                )
                            }

                            text { ::content { "Lightness: ${(hslColor().lightness.times(100)).toInt()}%" } }
                            slider {
                                range(0f, 1f)
                                value bind hslColor.lens(
                                    get = { it.lightness },
                                    modify = { a, b -> a.copy(lightness = b) },
                                )
                            }

                            text { ::content { "Alpha: ${(hslColor().alpha.times(100)).toInt()}%" } }
                            slider {
                                range(0f, 1f); value bind hslColor.lens(
                                get = { it.alpha },
                                modify = { a, b -> a.copy(alpha = b) },
                            )
                            }
                        }
                        space()

                        renderColorSection(hslColor.lens { it.toRGB() })
                    }
                }
                card.col {
                    val hsvColor = Signal(
                        HSVColor(
                            alpha = 1.0f,
                            hue = 36.degrees,
                            saturation = 0.64f,
                            value = 1f
                        )
                    )

                    h2 { content = "HSL Controls" }
                    row {
                        sizeConstraints(width = 12.rem).col {
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
                        space()

                        renderColorSection(hsvColor.lens { it.toRGB() })
                    }
                }


                card.col {
                    h2 { content = "Presets" }
                    row {
                        listOf(
                            Color.red, Color.orange, Color.yellow, Color.green,
                            Color.teal, Color.blue, Color.purple, Color.white, Color.black
                        ).forEach { c ->
                            themed(ThemeDerivation {
                                it.copy(id = "color_${c.toInt()}", background = c).withBack
                            }).sizeConstraints(width = 3.rem, height = 3.rem).frame {

                            }
                            space()
                        }
                    }
                }
            }
        }
    }

    private fun ViewWriter.renderColorSection(color: Reactive<Color>) {
        col {
            val debounced = color.debounce(200)
            sizeConstraints(height = 6.rem, width = 6.rem).frame {
                dynamicTheme {
                    ThemeDerivation {
                        it.copy(
                            id = "color_${debounced().toInt()}",
                            background = debounced()
                        ).withBack
                    }
                }
            }
            space()
            col {
                text { ::content { "RGBA: ${debounced().redInt}, ${debounced().greenInt}, ${debounced().blueInt}, ${(debounced().alpha * 100).toInt()}%" } }
                text { ::content { "Hex: ${debounced().toAlphalessWeb()}" } }
            }
        }
    }
}
