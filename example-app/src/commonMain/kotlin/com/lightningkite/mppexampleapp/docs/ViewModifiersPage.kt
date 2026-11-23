package com.lightningkite.mppexampleapp.docs

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*

@Routable("docs/view-modifiers")
object ViewModifiersPage : DocPage {
    override val covers: List<String> =
        listOf("view modifiers", "View modifier", "ViewModifiers", "viewModifiers", "viewmodifiers")

    override fun ElementWriter.CanAddTheme.render(): Unit = run {
        article {
            h1("View Modifiers")
            text("KiteUI has a number of view modifiers that can be used to modify the look, behavior and position of views.")
            h2("Syntax")
            text("Modifiers go before the element, joined with '.'. The compiler enforces their order: alignment → weight → shownWhen → sizing → theme → scrolling → element.")
            example(
                """
                col {
                    important.button { text("Hello World") }
                    centered.sizeConstraints(width = 20.rem).card.text("Centered card")
                }
            """.trimIndent()
            ) {
                col {
                    important.button { text("Hello World") }
                    centered.sizeConstraints(width = 20.rem).card.text("Centered card")
                }
            }
            h2("Alignment")
            text("align takes a horizontal and a vertical alignment.  It positions items within a column, row or frame. " +
                    "You can use the following values for the two parameters: ${
                        Align.entries.joinToString(", ") { it.name }
                    } ")
            example(
                """
               sizeConstraints(minHeight = 200.px).frame {
                    val aligns = listOf(Align.Start, Align.Center, Align.End)
                    for (h in aligns) {
                        for (v in aligns) {
                            align(h, v).text { content = "${'$'}h ${'$'}v" }
                        }
                    }
                }
            """.trimIndent()
            ) {
                sizeConstraints(minHeight = 200.px).frame {
                    val aligns = listOf(Align.Start, Align.Center, Align.End)
                    for (h in aligns) {
                        for (v in aligns) {
                            align(h, v).text { content = "$h $v" }
                        }
                    }
                }
            }
            text("There's also a shortcut available for simply centering an item.")
            example(
                """
                sizeConstraints(minHeight = 100.px).frame {
                    centered.text("Centered")
                }
            """.trimIndent()
            ) {
                sizeConstraints(minHeight = 100.px).frame {
                    centered.text("Centered")
                }
            }
            h2("Weight")
            text("Weight is used to set the size of a view in a row or column.  It is used to set the relative size of the view compared to other views in the row or column.")
            h3("Elements with Weight in a Row")
            example(
                """
                row {
                    weight(0.5f).card.text { content = "Card 1" }
                    weight(5f).card.text { content = "Card 2" }
                    weight(2f).card.text { content = "Card 3" }
                }
            """.trimIndent()
            ) {
                row {
                    weight(0.5f).card.text { content = "Card 1" }
                    weight(5f).card.text { content = "Card 2" }
                    weight(2f).card.text { content = "Card 3" }
                }
            }

            h3("Elements with Weight in a Column")
            example(
                """
                sizeConstraints(minHeight = 200.px).col {
                    weight(0.5f).card.text { content = "Card 1" }
                    weight(5f).card.text { content = "Card 2" }
                    weight(2f).card.text { content = "Card 3" }
                }
            """.trimIndent()
            ) {
                sizeConstraints(minHeight = 200.px).col {
                    weight(0.5f).card.text { content = "Card 1" }
                    weight(5f).card.text { content = "Card 2" }
                    weight(2f).card.text { content = "Card 3" }
                }
            }

            h2("Text Popover")
            text("The textPopover modifier is used to add a popover to a text view.")
            example(
                """
                textPopover("This is a popover").text("Text Popover")
            """.trimIndent()
            ) {
                textPopover("This is a popover").text("Text Popover")
            }

            h2("Has Popover")
            text("The hintPopover modifier is used to add a popover to a view.")
            example(
                """
                hintPopover {
                    card.col {
                        card.button {
                            text("Popover")
                        }
                        card.button {
                            text("Second Popover button")
                        }
                    }
                }.button {
                    text("Has Popover")
                }
            """.trimIndent()
            ) {
                hintPopover {
                    card.col {
                        card.button {
                            text("Popover")
                        }
                        card.button {
                            text("Second Popover button")
                        }
                    }
                }.button {
                    text("Has Popover")
                }
            }
            h2("Scrolls")
            text("The scrolling modifier is used to add a scroll bar to a view.")
            example(
                """
                sizeConstraints(maxHeight = 100.px).scrolling.col {
                    text("Scrolls Vertically Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed do eiusmod tempor incididunt ut " +
                            "labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip " +
                            "ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla " +
                            "pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum." +
                            "Scrolls Vertically Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed do eiusmod tempor incididunt ut ")
                }
            """.trimIndent()
            ) {
                sizeConstraints(maxHeight = 100.px).scrolling.col {
                    text(
                        "Scrolls Vertically Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed do eiusmod tempor incididunt ut " +
                                "labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip " +
                                "ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla " +
                                "pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum." +
                                "Scrolls Vertically Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed do eiusmod tempor incididunt ut "
                    )
                }
            }

            h2("Scrolling Horizontally")
            text("The scrollingHorizontally modifier is used to add a horizontal scroll bar to a view.")
            example(
                """
                sizeConstraints(minHeight = 10.px).scrollingHorizontally.row {
                    text(
                        "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed do eiusmod tempor incididunt ut " +
                                "Scrolls Vertically Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed do eiusmod tempor incididunt ut"
                    )
                }
              """.trimIndent()
            ) {
                sizeConstraints(minHeight = 10.px).scrollingHorizontally.row {
                    text(
                        "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed do eiusmod tempor incididunt ut " +
                                "Scrolls Vertically Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed do eiusmod tempor incididunt ut"
                    )
                }
            }

            h2("Size Constraints")
            text("sizeConstraints sets the width and height of a view in a column, row or frame: a fixed width and height, or minimums and maximums.")
            example(
                """
                col {
                    sizeConstraints(maxWidth = 200.px, maxHeight = 200.px).card.text(
                        "Size Constraints with maxWidth, and maxHeight  Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed do eiusmod tempor incididunt ut"
                    )
                }
                col {
                    sizeConstraints(width = 100.px, height = 100.px).card.text(
                        "Size Constraints with width and height"
                    )
                }
                col {
                    sizeConstraints(minWidth = 200.px, minHeight = 200.px).card.text(
                        "Size Constraints with min width and minHeight"
                    )
                }
            """.trimIndent()
            ) {
                col {
                    sizeConstraints(maxWidth = 200.px, maxHeight = 200.px).card.text(
                        "Size Constraints with maxWidth, and maxHeight  Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed do eiusmod tempor incididunt ut"
                    )
                }
                col {
                    sizeConstraints(width = 100.px, height = 100.px).card.text(
                        "Size Constraints with width and height"
                    )
                }
                col {
                    sizeConstraints(minWidth = 200.px, minHeight = 200.px).card.text(
                        "Size Constraints with min width and minHeight"
                    )
                }
            }

            h2("Padded")
            text("The padded modifier is used to add padding to a box.")
            text("You can override the default padding by setting the theme gap value.")
            example(
                """
                col {
                    card.row {
                        padded.text("Padded")
                    }
                    card.row {
                        text("Not Padded")
                    }
                }
            """.trimIndent()
            ) {
                col {
                    card.row {
                        padded.text("Padded")
                    }
                    card.row {
                        text("Not Padded")
                    }
                }

            }

            h2("Shown When")
            text("The shownWhen modifier is used to show a view only when a condition is met.")
            val condition: Signal<Boolean> = Signal(true)

            example(
                """
                val condition = Signal(true)
                col {
                    important.toggleButton {
                        text { ::content { if (condition()) "Hide" else "Show" } }
                        checked bind condition
                    }
                    shownWhen { condition() }.text("Show Text Only When Toggled")
                }
            """.trimIndent()
            ) {
                col {
                    important.toggleButton {
                        text { ::content { if (condition()) "Hide" else "Show" } }
                        checked bind condition
                    }
                    shownWhen { condition() }.text("Show Text Only When Toggled")
                }
            }
        }
    }
}