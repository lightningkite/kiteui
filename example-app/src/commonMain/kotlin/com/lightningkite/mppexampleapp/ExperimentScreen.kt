package com.lightningkite.mppexampleapp

import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.*
import com.lightningkite.kiteui.exceptions.PlainTextException
import com.lightningkite.kiteui.models.Icon
import com.lightningkite.kiteui.models.px
import com.lightningkite.kiteui.models.rem
import com.lightningkite.kiteui.navigation.Screen
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.l2.field
import com.lightningkite.kiteui.views.l2.icon

@Routable("experiment")
object ExperimentScreen : Screen {
    override val title: Readable<String>
        get() = super.title

    override fun ViewWriter.render() {
        stack {
            centered - sizeConstraints(width = 40.rem) - col {
                numberInput {
                    content bind Property(5.0)
                }
//                val value = Property(false)
//                card - toggleButton {
//                    checked bind value
//                    text("Show")
//                }
//                onlyWhen { value() } - text("hidden item")
//                text("Lower item")
//
//                field("Email") {
//                    row {
//                        val text = Property("").also { it.addListener { println("text: ${it.value}") } }.lens(
//                            get = { it },
//                            set = {
//                                if(it.isBlank()) throw PlainTextException("Cannot be blank")
//                                it
//                            }
//                        )
//                        expanding
//                        val tf = textInput { content bind text }
//                        button {
//                            spacing = 0.px
//                            icon(Icon.close.copy(1.5.rem, 1.5.rem), "Clear")
//                            onClick { tf.content.value = "" }
//                        }
//                    }
//                }
            }
        }
    }
}