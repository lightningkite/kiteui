package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.*
import com.lightningkite.kiteui.exceptions.PlainTextException
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.navigation.Screen
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.l2.*
import com.lightningkite.mppexampleapp.Resources
import kotlin.random.Random

@Routable("experiment")
object ExperimentScreen : Screen {
    override val title: Readable<String>
        get() = super.title

    override fun ViewWriter.render(){
        scrolls - col {
            gravity(Align.Center, Align.Stretch) - sizeConstraints(width = 80.rem) - card - col {
                rowCollapsingToColumn(35.rem) {
                    col {
                        gravity(Align.Center, Align.Start) - sizeConstraints(width = 9.rem, height = 9.rem) - image {
                            scaleType = ImageScaleType.Fit
                            ::source { Resources.imagesSolera }
                        }
                    }
                    row {
                        col {
                            spacing = 0.1.rem
                            h4 { ::content { "product().title" } }
                            subtext { ::content { "product().erpId" } }
                            subtext { ::content { "product().manufacturer" } }
                            atStart - SubtextSemantic.onNext - row {
                                ::exists { true }
                                spacing = 0.5.rem
                                centered - text {
                                    ::content {
                                        "In stock at"
                                    }
                                }
                                sizeConstraints(width = 10.rem) - fieldTheme - select {
                                    spacing = 0.2.rem
                                    bind(
                                        edits = Property(""),
                                        data = Constant(listOf("A", "B", "C")),
                                        render = { it }
                                    )
                                }
                            }

                        }
                        atTop - toggleButton {
                            ::exists { true }
                            ::enabled { true }
                            icon {
                                ::source { Icon.copy }
                            }
                            checked bind Property(false)
                        }
                    }
                }
                text {
                    content = """Lorem Ipsum is simply dummy text of the printing and typesetting industry. Lorem Ipsum has been the industry's standard dummy text ever since the 1500s, when an unknown printer took a galley of type and scrambled it to make a type specimen book. It has survived not only five centuries, but also the leap into electronic typesetting, remaining essentially unchanged. It was popularised in the 1960s with the release of Letraset sheets containing Lorem Ipsum passages, and more recently with desktop publishing software like Aldus PageMaker including versions of Lorem Ipsum."""
                }

                warning - col {
                    exists = false

                    bold - rowCollapsingToColumn(60.rem) {
                        text("Upcoming Price Increase")
                        text { ::content { "NOW" } }
                    }

                    text { ::content { "Message" } }
                }

                rowCollapsingToColumn(60.rem) {
                    gravity(Align.Start, Align.Center) - onlyWhen { true } - row {
                        text("List Price")
                        text("\$XXX")
//                        detail("List Price") {
//                            pricing()()?.previousListPrice?.takeUnless { it == 0.cents }?.toString() ?: "-"
//                        }
                        separator()
                        text("Your Price")
                        text("\$XXX")
//                        detail("Your Price") {
//                            pricing()()?.previousPrice?.takeUnless { it == 0.cents }?.toString() ?: "-"
//                        }
                    }
                    gravity(Align.Start, Align.Center) - onlyWhen {
                        false
                    } - text("Pricing established after ordering")

                    expanding - space()

                    col {
                        reactiveScope {
                            clearChildren()
//                            expanding
                            field("Sample") {
                                numberInput()
                            }
                        }
                    }
                }
            }
        }
    }
}