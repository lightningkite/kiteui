package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.Icon
import com.lightningkite.kiteui.models.rem
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.reactive.Action
import com.lightningkite.kiteui.viewDebugTarget
import com.lightningkite.kiteui.views.ViewModifiable
import com.lightningkite.readable.invoke
import com.lightningkite.readable.reactive
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.card
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.expanding

@Routable("scroll-test")
object SpecialScrollTest : Page {
    override fun ViewWriter.render(): ViewModifiable = run {
        col {
            h1 { content = "Scroll Layout Test" }
            lateinit var verticalScrollElement: ScrollingBehaviors
            expanding - scrolling {
                verticalScrollElement = this
            } - col {
                repeat(10) {
                    sizeConstraints(minWidth = 8.rem, minHeight = 8.rem) - card - text("Hello World: $it")
                }
            }
            sizeConstraints(height = 7.rem) - row {
                col {
                    button {
                        text("Scroll to End Smooth")
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
                    button {
                        text("Offset 100")
                        action = Action("Offset 100", Icon.done) {
                            verticalScrollElement.scrollToKeepAnimations(0.0, 100.0)
                        }
                    }
                }
                expanding - text {
                    ::content {
                        verticalScrollElement.content().toString() + "\n" + verticalScrollElement.viewport().toString()
                    }
                }
                reactive {
                    println("viewport: ${verticalScrollElement.viewport()}")
                }
                checkbox { verticalScrollElement::snapToElements { if(checked()) null to Align.Start else null to null } }
                checkbox { verticalScrollElement::scrollSnapStop { checked() } }
            }
            lateinit var horizontalScrollElement: ScrollingBehaviors
            expanding - scrollsHorizontally {
                horizontalScrollElement = this
            } - row {
                repeat(10) {
                    sizeConstraints(minWidth = 8.rem, minHeight = 8.rem) - card - text("Hello World: $it")
                }
            }
            sizeConstraints(height = 7.rem) - row {
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
                checkbox { horizontalScrollElement::snapToElements { if(checked()) Align.Start to null else null to null } }
                checkbox { horizontalScrollElement::scrollSnapStop { checked() } }
            }
        }
    }
}