package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.models.Icon
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.l2.icon
import com.lightningkite.reactive.core.Signal

/**
 * Test page for identifying click targeting issues.
 * Each clickable element reports its identity when clicked.
 */
@Routable("click-test")
object ClickTestPage : Page {

    override val title get() = com.lightningkite.reactive.core.Constant("Click Test")

    private val lastClicked = Signal("(none)")
    private val clickLog = Signal(mutableListOf<String>())

    private fun logClick(name: String) {
        lastClicked.value = name
        val log = clickLog.value.toMutableList()
        log.add(0, name)
        if (log.size > 10) log.removeLast()
        clickLog.value = log
    }

    override fun ViewWriter.render(): Unit = run {
        scrolling.col {
            h1 { content = "Click Test Page" }

            // Status display
            card.col {
                h3 { content = "Last Clicked:" }
                text { ::content { lastClicked() } }
            }

            // Grid of numbered buttons
            h2 { content = "Button Grid (3x3)" }
            text { content = "Click each button - it should report its number" }

            card.col {
                row {
                    for (i in 1..3) {
                        weight(1f).button {
                            text { content = "Button $i" }
                            onClick { logClick("Button $i") }
                        }
                    }
                }
                row {
                    for (i in 4..6) {
                        weight(1f).button {
                            text { content = "Button $i" }
                            onClick { logClick("Button $i") }
                        }
                    }
                }
                row {
                    for (i in 7..9) {
                        weight(1f).button {
                            text { content = "Button $i" }
                            onClick { logClick("Button $i") }
                        }
                    }
                }
            }

            // Links test
            h2 { content = "Link Row" }
            text { content = "Click each link - should report A, B, C, D" }

            card.row {
                for (letter in listOf("A", "B", "C", "D")) {
                    weight(1f).link {
                        centered.col {
                            icon(Icon.home, "")
                            text { content = "Link $letter" }
                        }
                        onClick { logClick("Link $letter") }
                    }
                }
            }

            // Tall buttons (like bottom tabs)
            h2 { content = "Bottom Tab Style (side by side)" }
            text { content = "These simulate bottom navigation tabs" }

            card.row {
                for (tab in listOf("Home", "Search", "Profile", "Settings")) {
                    weight(1f).button {
                        col {
                            centered.icon(Icon.home, "")
                            centered.text { content = tab }
                        }
                        onClick { logClick("Tab: $tab") }
                    }
                }
            }

            // Click log
            h2 { content = "Click Log (last 10)" }
            card.col {
                text {
                    ::content {
                        clickLog().joinToString("\n").ifEmpty { "(no clicks yet)" }
                    }
                }
            }

            // Clear button
            button {
                text { content = "Clear Log" }
                onClick {
                    lastClicked.value = "(none)"
                    clickLog.value = mutableListOf()
                }
            }
        }
    }
}
