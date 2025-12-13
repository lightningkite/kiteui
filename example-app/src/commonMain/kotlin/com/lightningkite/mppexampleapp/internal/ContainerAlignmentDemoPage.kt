package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*

/**
 * Demonstrates the newChildHorizontalAlign and newChildVerticalAlign properties
 * that allow containers to set default alignment for their children.
 */
@Routable("container-alignment-demo")
object ContainerAlignmentDemoPage : Page {
    override fun ViewWriter.render(): Unit = run {
        scrolling.col {
            h1 { content = "Container Child Alignment Demo" }

            text {
                content = "Containers can now set default alignment for their children using " +
                    "newChildHorizontalAlign and newChildVerticalAlign properties. " +
                    "Children without explicit alignment will use the container's defaults."
            }

            // Column with horizontal alignment defaults
            card.col {
                h2 { content = "Column with Horizontal Alignment Defaults" }

                subtext { content = "newChildHorizontalAlign = Align.Start" }
                sizedBox(SizeConstraints(minHeight = 8.rem)).important.col {
                    newChildHorizontalAlign = Align.Start

                    card.text { content = "Left-aligned (Start)" }
                    card.text { content = "Also left-aligned" }
                    card.text { content = "All use default" }
                }

                space()

                subtext { content = "newChildHorizontalAlign = Align.Center" }
                sizedBox(SizeConstraints(minHeight = 8.rem)).important.col {
                    newChildHorizontalAlign = Align.Center

                    card.text { content = "Center-aligned" }
                    card.text { content = "Also centered" }
                    card.text { content = "All use default" }
                }

                space()

                subtext { content = "newChildHorizontalAlign = Align.End" }
                sizedBox(SizeConstraints(minHeight = 8.rem)).important.col {
                    newChildHorizontalAlign = Align.End

                    card.text { content = "Right-aligned (End)" }
                    card.text { content = "Also right-aligned" }
                    card.text { content = "All use default" }
                }
            }

            // Row with vertical alignment defaults
            card.col {
                h2 { content = "Row with Vertical Alignment Defaults" }

                subtext { content = "newChildVerticalAlign = Align.Start (Top)" }
                sizedBox(SizeConstraints(minHeight = 10.rem)).important.row {
                    newChildVerticalAlign = Align.Start
                    gap = 1.rem

                    card.col {
                        text { content = "Top" }
                        subtext { content = "(Start)" }
                    }
                    card.sizedBox(SizeConstraints(height = 4.rem)).frame {
                        centered.text { content = "Tall item" }
                    }
                    card.text { content = "Top-aligned" }
                }

                space()

                subtext { content = "newChildVerticalAlign = Align.Center" }
                sizedBox(SizeConstraints(minHeight = 10.rem)).important.row {
                    newChildVerticalAlign = Align.Center
                    gap = 1.rem

                    card.text { content = "Middle" }
                    card.sizedBox(SizeConstraints(height = 4.rem)).frame {
                        centered.text { content = "Tall item" }
                    }
                    card.text { content = "Center-aligned" }
                }

                space()

                subtext { content = "newChildVerticalAlign = Align.End (Bottom)" }
                sizedBox(SizeConstraints(minHeight = 10.rem)).important.row {
                    newChildVerticalAlign = Align.End
                    gap = 1.rem

                    card.col {
                        text { content = "Bottom" }
                        subtext { content = "(End)" }
                    }
                    card.sizedBox(SizeConstraints(height = 4.rem)).frame {
                        centered.text { content = "Tall item" }
                    }
                    card.text { content = "Bottom-aligned" }
                }
            }

            // Frame with both horizontal and vertical defaults
            card.col {
                h2 { content = "Frame with Both Alignment Defaults" }

                subtext { content = "newChildHorizontalAlign = End, newChildVerticalAlign = Start" }
                sizedBox(SizeConstraints(minHeight = 12.rem)).important.frame {
                    newChildHorizontalAlign = Align.End
                    newChildVerticalAlign = Align.Start

                    card.text { content = "Top-Right" }
                }

                space()

                subtext { content = "newChildHorizontalAlign = Center, newChildVerticalAlign = Center" }
                sizedBox(SizeConstraints(minHeight = 12.rem)).important.frame {
                    newChildHorizontalAlign = Align.Center
                    newChildVerticalAlign = Align.Center

                    card.text { content = "Centered" }
                }

                space()

                subtext { content = "newChildHorizontalAlign = Start, newChildVerticalAlign = End" }
                sizedBox(SizeConstraints(minHeight = 12.rem)).important.frame {
                    newChildHorizontalAlign = Align.Start
                    newChildVerticalAlign = Align.End

                    card.text { content = "Bottom-Left" }
                }
            }

            // Explicit alignment overrides default
            card.col {
                h2 { content = "Explicit Alignment Overrides Default" }
                subtext { content = "Container sets Center default, but children can override" }

                sizedBox(SizeConstraints(minHeight = 10.rem)).important.col {
                    newChildHorizontalAlign = Align.Center

                    card.text { content = "Uses default (Center)" }
                    align(Align.Start, Align.Stretch).card.text { content = "Explicitly left (Start)" }
                    align(Align.End, Align.Stretch).card.text { content = "Explicitly right (End)" }
                    card.text { content = "Back to default (Center)" }
                }
            }

            // Nested containers with different defaults
            card.col {
                h2 { content = "Nested Containers with Different Defaults" }

                sizedBox(SizeConstraints(minHeight = 15.rem)).important.col {
                    newChildHorizontalAlign = Align.Start

                    card.text { content = "Outer: Left-aligned" }

                    card.col {
                        newChildHorizontalAlign = Align.End
                        text { content = "Inner container" }
                        critical.text { content = "Right-aligned in inner" }
                        critical.text { content = "Also right-aligned" }
                    }

                    card.text { content = "Outer: Back to left-aligned" }
                }
            }

            // Practical example: Form layout
            card.col {
                h2 { content = "Practical Example: Form Layout" }
                subtext { content = "Using default alignment to simplify form creation" }

                card.col {
                    newChildHorizontalAlign = Align.Stretch
                    gap = 0.5.rem

                    text { content = "Login Form" }

                    textField { hint = "Email" }
                    textField { hint = "Password" }

                    space()

                    row {
                        newChildHorizontalAlign = Align.Center
                        gap = 1.rem

                        button { text("Cancel") }
                        important.button { text("Login") }
                    }
                }
            }

            // Comparison: Before and After
            card.col {
                h2 { content = "Comparison: Before vs After" }

                h3 { content = "Before (Explicit alignment for each child)" }
                card.col {
                    align(Align.End, Align.Stretch).text { content = "Button 1" }
                    align(Align.End, Align.Stretch).text { content = "Button 2" }
                    align(Align.End, Align.Stretch).text { content = "Button 3" }
                }

                space()

                h3 { content = "After (Container default alignment)" }
                card.col {
                    newChildHorizontalAlign = Align.End
                    text { content = "Button 1" }
                    text { content = "Button 2" }
                    text { content = "Button 3" }
                }

                subtext {
                    content = "Much cleaner! The container sets the default, and all children automatically use it."
                }
            }
        }
    }
}
