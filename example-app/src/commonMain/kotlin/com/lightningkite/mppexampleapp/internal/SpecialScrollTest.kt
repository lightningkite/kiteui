package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.Icon
import com.lightningkite.kiteui.models.rem
import com.lightningkite.kiteui.navigation.Screen
import com.lightningkite.kiteui.reactive.Action
import com.lightningkite.kiteui.reactive.invoke
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.card
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.expanding

@Routable("scroll-test")
object SpecialScrollTest : Screen {
    override fun ViewWriter.render() {
        col {
            h1 { content = "Scroll Layout Test" }
            lateinit var verticalScrollElement: ScrollingBehaviors
            expanding - scrolls {
                snapToElements = null to Align.Start
                verticalScrollElement = this
            } - col {
                repeat(10) {
                    sizeConstraints(minWidth = 8.rem, minHeight = 8.rem) - card - text("Hello World")
                }
            }
            sizeConstraints(height = 3.rem) - row {
                button {
                    text("Scroll to End PLZ")
                    action = Action("Scroll to End", Icon.done) {
                        verticalScrollElement.scrollTo(
                            left = verticalScrollElement.content()
                                .also { println("Content: $it") }.right - verticalScrollElement.viewport()
                                .also { println("Viewport: $it") }.width,
                            top = verticalScrollElement.content().bottom - verticalScrollElement.viewport().height,
                            animated = true
                        )
                    }
                }
                expanding - text {
                    ::content {
                        verticalScrollElement.content().toString() + "\n" + verticalScrollElement.viewport().toString()
                    }
                }
            }
            lateinit var horizontalScrollElement: ScrollingBehaviors
            expanding - scrollsHorizontally {
                snapToElements = Align.Start to null
                horizontalScrollElement = this
            } - row {
                repeat(10) {
                    sizeConstraints(minWidth = 8.rem, minHeight = 8.rem) - card - text("Hello World")
                }
            }
            sizeConstraints(height = 3.rem) - row {
                button {
                    text("Scroll to End PLZ")
                    action = Action("Scroll to End", Icon.done) {
                        horizontalScrollElement.scrollTo(
                            left = horizontalScrollElement.content()
                                .also { println("Content: $it") }.right - horizontalScrollElement.viewport()
                                .also { println("Viewport: $it") }.width,
                            top = horizontalScrollElement.content().bottom - horizontalScrollElement.viewport().height,
                            animated = true
                        )
                    }
                }
                expanding - text {
                    ::content {
                        horizontalScrollElement.content().toString() + "\n" + horizontalScrollElement.viewport()
                            .toString()
                    }
                }
            }
        }
    }
}