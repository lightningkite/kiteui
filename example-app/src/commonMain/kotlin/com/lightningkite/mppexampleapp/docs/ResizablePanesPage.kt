package com.lightningkite.mppexampleapp.docs

import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.emphasized
import com.lightningkite.kiteui.views.l2.icon
import com.lightningkite.kiteui.views.l2.titledSection
import com.lightningkite.readable.Property

@Routable("docs/resizable-panes")
object ResizablePanesPage : DocPage {
    override val covers: List<String> = listOf("resizable", "panes", "resizablePanes", "split", "divider", "resize")

    override fun ViewWriter.render(): ViewModifiable = run {
        article {
            titledSection("Resizable Panes") {
                text("Resizable panes allow you to create layouts where users can adjust the size of elements by dragging dividers between them.")

                titledSection("Basic Usage") {
                    text("Similar to row and column, resizable panes arrange elements either horizontally or vertically, but with resizable dividers between them.")
                    example(
                        """
                        resizablePanes {
                            vertical = true // Arrange vertically (default)
                            card - text("First pane")
                            card - text("Second pane")
                            card - text("Third pane")
                        }
                        """.trimIndent()
                    ) {
                        resizablePanes {
                            vertical = true
                            card - text("First pane")
                            card - text("Second pane")
                            card - text("Third pane")
                        }
                    }

                    text("You can also arrange elements horizontally:")
                    example(
                        """
                        resizablePanes {
                            vertical = false // Arrange horizontally
                            card - text("Left pane")
                            card - text("Middle pane")
                            card - text("Right pane")
                        }
                        """.trimIndent()
                    ) {
                        resizablePanes {
                            vertical = false
                            card - text("Left pane")
                            card - text("Middle pane")
                            card - text("Right pane")
                        }
                    }
                }

                titledSection("Customizing Dividers") {
                    text("You can customize the size of the dividers between panes:")
                    example(
                        """
                        resizablePanes {
                            vertical = false
                            dividerSize = 8.px // Make dividers thicker
                            card - text("Left pane")
                            card - text("Right pane")
                        }
                        """.trimIndent()
                    ) {
                        resizablePanes {
                            vertical = false
                            dividerSize = 8.px
                            card - text("Left pane")
                            card - text("Right pane")
                        }
                    }

                    text("You can also set a minimum size for panes to prevent them from becoming too small:")
                    example(
                        """
                        resizablePanes {
                            vertical = true
                            minPaneSize = 100.px // Minimum pane size
                            card - text("Top pane")
                            card - text("Bottom pane")
                        }
                        """.trimIndent()
                    ) {
                        resizablePanes {
                            vertical = true
                            minPaneSize = 100.px
                            card - text("Top pane")
                            card - text("Bottom pane")
                        }
                    }
                }

                titledSection("Practical Examples") {
                    text("Resizable panes are useful for creating split views, code editors, and other interfaces where users need to adjust the layout.")

                    text("Here's an example of a simple code editor layout:")
                    example(
                        """
                        sizeConstraints(height = 20.rem) - resizablePanes {
                            vertical = false

                            // File explorer panel
                            weight(1f) - card - col {
                                h3("Files")
                                text("file1.txt")
                                text("file2.txt")
                                text("file3.txt")
                            }

                            // Editor panel
                            weight(3f) - card - col {
                                h3("Editor")
                                text("function example() {")
                                text("    console.log('Hello, world!');")
                                text("}")
                            }
                        }
                        """.trimIndent()
                    ) {
                        sizeConstraints(height = 20.rem) - resizablePanes {
                            vertical = false

                            // File explorer panel
                            weight(1f) - card - col {
                                h3("Files")
                                text("file1.txt")
                                text("file2.txt")
                                text("file3.txt")
                            }

                            // Editor panel
                            weight(3f) - card - col {
                                h3("Editor")
                                text("function example() {")
                                text("    console.log('Hello, world!');")
                                text("}")
                            }
                        }
                    }

                    text("And here's a more complex layout with both horizontal and vertical splits:")
                    example(
                        """
                        sizeConstraints(height = 30.rem) - resizablePanes {
                            vertical = false

                            // Left panel
                            weight(1f) - card - col {
                                h3("Navigation")
                                text("Home")
                                text("About")
                                text("Contact")
                            }

                            // Right panel with vertical split
                            weight(3f) - resizablePanes {
                                vertical = true

                                // Top content
                                weight(2f) - card - col {
                                    h3("Content")
                                    text("This is the main content area that can be resized.")
                                    text("Try dragging the dividers to adjust the layout.")
                                }

                                // Bottom panel
                                weight(1f) - card - col {
                                    h3("Console")
                                    text("> Loading complete")
                                    text("> Ready")
                                }
                            }
                        }
                        """.trimIndent()
                    ) {
                        sizeConstraints(height = 30.rem) - resizablePanes {
                            vertical = false

                            // Left panel
                            weight(1f) - card - col {
                                h3("Navigation")
                                text("Home")
                                text("About")
                                text("Contact")
                            }

                            // Right panel with vertical split
                            weight(3f) - resizablePanes {
                                vertical = true

                                // Top content
                                weight(2f) - card - col {
                                    h3("Content")
                                    text("This is the main content area that can be resized.")
                                    text("Try dragging the dividers to adjust the layout.")
                                }

                                // Bottom panel
                                weight(1f) - card - col {
                                    h3("Console")
                                    text("> Loading complete")
                                    text("> Ready")
                                }
                            }
                        }
                    }
                }

                titledSection("Spacing") {
                    text("Like other container elements, you can control the spacing between elements:")
                    example(
                        """
                        resizablePanes {
                            vertical = true
                            card - text("First pane")
                            spacingOverrideBeforeNext(2.rem)
                            card - text("Second pane (with extra space above)")
                            card - text("Third pane")
                        }
                        """.trimIndent()
                    ) {
                        resizablePanes {
                            vertical = true
                            card - text("First pane")
                            spacingOverrideBeforeNext(2.rem)
                            card - text("Second pane (with extra space above)")
                            card - text("Third pane")
                        }
                    }
                }

                titledSection("Dynamic Content") {
                    text("Resizable panes handle dynamic content changes gracefully:")
                    example(
                        """
                        val showExtra = Property(false)
                        col {
                            row {
                                checkbox {
                                    checked bind showExtra
                                }
                                text("Show extra content")
                            }

                            resizablePanes {
                                vertical = false
                                card - text("Left pane")

                                // Middle pane only shows when checkbox is checked
                                shownWhen { showExtra() } - card - text("Middle pane (dynamic)")

                                card - text("Right pane")
                            }
                        }
                        """.trimIndent()
                    ) {
                        val showExtra = Property(false)
                        col {
                            row {
                                checkbox {
                                    checked bind showExtra
                                }
                                text("Show extra content")
                            }

                            resizablePanes {
                                vertical = false
                                card - text("Left pane")

                                // Middle pane only shows when checkbox is checked
                                shownWhen { showExtra() } - card - text("Middle pane (dynamic)")

                                card - text("Right pane")
                            }
                        }
                    }
                }
            }
        }
    }
}
