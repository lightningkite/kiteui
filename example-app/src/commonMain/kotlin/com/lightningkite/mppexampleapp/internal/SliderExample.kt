package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.*
import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*

@Routable("slider-example")
object SliderExamplePage : Page {
    override fun ViewWriter.render(): ViewModifiable {
        val sliderValue = Signal(50f)
        val sliderValueText = Signal("50")

        return scrolling.col {
            h1 { content = "Slider Example" }

            card.col {
                h2 { content = "Basic Slider (0-100)" }

                text { ::content { "Value: ${sliderValue().toInt()}" } }

                slider {
                    range(0f, 100f)
                    value bind sliderValue
                }
            }

            card.col {
                h2 { content = "Slider with Step (0-10, step 0.5)" }

                val stepSliderValue = Signal(5f)
                text { ::content { "Value: ${stepSliderValue()}" } }

                slider {
                    range(0f, 10f, 0.5f)
                    value bind stepSliderValue
                }
            }

            card.col {
                h2 { content = "Themed Sliders" }

                val themedSliderValue = Signal(50f)
                text { ::content { "Value: ${themedSliderValue().toInt()}" } }

                slider {
                    range(0f, 100f)
                    value bind themedSliderValue
                }

                card.slider {
                    range(0f, 100f)
                    value bind themedSliderValue
                }

                important.slider {
                    range(0f, 100f)
                    value bind themedSliderValue
                }

                critical.slider {
                    range(0f, 100f)
                    value bind themedSliderValue
                }
            }

            card.col {
                h2 { content = "Disabled Slider" }

                slider {
                    range(0f, 100f)
                    enabled = false
                }
            }
        }
    }
}
