package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.card
import com.lightningkite.kiteui.views.centered
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.dynamicTheme
import com.lightningkite.kiteui.views.fieldTheme
import com.lightningkite.kiteui.views.important
import com.lightningkite.kiteui.views.l2.colorPicker
import com.lightningkite.mppexampleapp.appTheme
import com.lightningkite.reactive.context.invoke
import com.lightningkite.reactive.context.reactive
import com.lightningkite.reactive.context.reactiveSuspending
import com.lightningkite.reactive.core.Constant
import com.lightningkite.reactive.core.MutableRemember
import com.lightningkite.reactive.core.Reactive
import com.lightningkite.reactive.core.ReactiveWithMutableValue
import com.lightningkite.reactive.core.Signal
import com.lightningkite.reactive.extensions.debounce
import com.lightningkite.reactive.extensions.interceptWrite
import com.lightningkite.reactive.extensions.modify
import com.lightningkite.reactive.extensions.value
import kotlin.math.abs


@Routable("color-test")
object ColorTestPage : Page {

    override fun ViewWriter.render() {
        scrolling.col {
            val color = Signal(Color.red)

            centered.menuButton {
                requireClick = true

                val debounced = color.debounce(100)

                padding = 0.dp
                frame {
                    text {
                        content = "Change Me!"
                        dynamicTheme {
                            ThemeDerivation {
                                it.copy(
                                    id = "color_${debounced().toInt()}",
                                    background = debounced(),
                                    foreground = debounced().highlight(1f),
                                ).withBack
                            }
                        }
                    }
                }

                opensMenu {
                    card.colorPicker(color)
                }
            }

            space(8.0)

            changeAppBackgroundColor()

        }
    }

    fun ViewWriter.changeAppBackgroundColor() {
        important.centered.menuButton {
            text { content = "Background Color" }
            requireClick = true

            val color = MutableRemember { appTheme().background.closestColor() }
            val debounced = color.debounce(1000)

            reactiveSuspending {
                debounced().let { newColor ->
                    appTheme.modify {
                        it.copy(
                            id = "bg_color_${newColor.toInt()}",
                            background = newColor.closestColor(),
                        )
                    }
                }
            }
            opensMenu {
                colorPicker(color)
            }
        }
    }

}

