package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.Platform
import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.current
import com.lightningkite.kiteui.models.rem
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.reactive.Action
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.l2.toast
import com.lightningkite.reactive.context.reactive
import com.lightningkite.reactive.core.Signal

@Routable("/internal/test/repeat-modifiers")
object TestRepeatableModifiers : Page {
    val bool = Signal(true)

    private fun ElementWriter.repeat(
        n: Int = 8,
        element: ViewWriter.(Int) -> Unit = { centered.text("Item ${it + 1}") }
    ) {
        kotlin.repeat(n) {
            frame {
                element(it)
            }
        }
    }

    private inline fun ElementWriter.section(name: String, stuff: RowOrCol.() -> Unit) {
        col {
            h3(name)
            stuff()
        }
    }

    override fun ElementWriter.CanAddTheme.render() {
        scrolling.col {
            text("These are tests for ensuring that modifiers that inject elements are reusable without causing issues (varies by platform)")

            subtext("Current Platform: ${Platform.current}")

            when (Platform.current) {
                Platform.Android -> {
                    col {
                        gap = 2.rem
                        section("sizeConstraints") {
                            scrollingHorizontally.row {
                                sizeConstraints(width = 10.rem).card.repeat()
                            }
                        }

                        section("scrollingWithRefresh") {
                            sizeConstraints(height = 10.rem).row {
                                expanding.card.scrollingWithRefresh(Action("refresh") { context.toast("Refreshing!") }).repeat(5) { list ->
                                    col {
                                        repeat(10) { text("List ${list + 1} Item ${it + 1}") }
                                    }
                                }
                            }
                        }

                        section("scrolling") {
                            sizeConstraints(height = 5.rem).row {
                                expanding.card.scrolling.repeat(5) { list ->
                                    col {
                                        repeat(10) { text("List ${list + 1} Item ${it + 1}") }
                                    }
                                }
                            }
                        }

                        section("scrollingHorizontally") {
                            sizeConstraints(width = 20.rem).col {
                                card.scrollingHorizontally.repeat(4) {
                                    text("${it + 1}. This is a very long message that is meant to scroll off of the screen so I can test scrolling so it needs to be very long")
                                }
                            }
                        }
                    }
                }

                Platform.Web -> {
                    col {
                        row {
                            centered.expanding.h3("shownWhen")
                            toggleButton {
                                text("Toggle")
                                checked bind bool
                            }
                        }
                        row {
                            expanding.shownWhen { bool() }.card.repeat()
                        }
                        row {
                            val writer = expanding.shownWhen { bool() }.card
                            reactive {
                                rerunOn(bool)
                                clearChildren()
                                writer.repeat()
                            }
                        }
                    }
                }

                else -> {}
            }
        }
    }
}