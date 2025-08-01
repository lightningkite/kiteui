package com.lightningkite.mppexampleapp.docs

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.emphasized
import com.lightningkite.kiteui.views.l2.icon
import com.lightningkite.kiteui.views.l2.titledSection
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*

@Routable("docs/layout")
public object LayoutPage : DocPage {
    public override val covers: List<String> = listOf("layout", "row", "column", "rowCollapsingToColumn", "frame", "sizeConstraints", "size", "gap", "padding")

    public override fun ViewWriter.render(): ViewModifiable = run {
        article {
            titledSection("Layout") {
                titledSection("Column and Row") {
                    text("Column and row simply place the elements in order, from top to bottom or left to right respectively.")
                    example(
                        """
                        col {
                            card - row {
                                card - text("First (first row)")
                                card - text("Second")
                                card - text("Third")
                            }
                            card - row {
                                card - text("First (second row)")
                                card - text("Second")
                                card - text("Third")
                            }
                            card - row {
                                card - text("First (third row)")
                                card - text("Second")
                                card - text("Third")
                            }
                        }
                    """.trimIndent()
                    ) {
                        col {
                            card - row {
                                card - text("First (first row)")
                                card - text("Second")
                                card - text("Third")
                            }
                            card - row {
                                card - text("First (second row)")
                                card - text("Second")
                                card - text("Third")
                            }
                            card - row {
                                card - text("First (third row)")
                                card - text("Second")
                                card - text("Third")
                            }
                        }
                    }

                    titledSection("Expanding and Weight") {
                        text("Many rows and columns have certain elements that need to take the remainder of the space available.")
                        text("This can be done by setting the weight of the element, like so:")
                        example(
                            """
                            row {
                                // By default, elements take the space they need in their container's primary direction.
                                card - text("A")
                                
                                // This element will take all the remaining space.
                                weight(1f) - card - text("B")
                                
                                // This element will again only take what it needs in the primary direction.
                                card - text("C")
                            }
                            """.trimIndent()
                        ) {
                            row {
                                // By default, elements take the space they need in their container's primary direction.
                                card - text("A")

                                // This element will take all the remaining space.
                                weight(1f) - card - text("B")

                                // This element will again only take what it needs in the primary direction.
                                card - text("C")
                            }
                        }
                        text("Usually, there's only one element that takes the remaining space.  The shortcut 'expanding' makes this more readable:")
                        example(
                            """
                            row {
                                card - text("A")
                                expanding - card - text("B")
                                card - text("C")
                            }
                            """.trimIndent()
                        ) {
                            row {
                                card - text("A")
                                expanding - card - text("B")
                                card - text("C")
                            }
                        }
                        text("When you need multiple elements to take the space, they get a ratio of the remaining space based on their weight.")
                        example(
                            """
                            row {
                                weight(1f) - card - text("weight(1f)")
                                weight(2f) - card - text("weight(2f)")
                            }
                            """.trimIndent()
                        ) {
                            row {
                                // This will take 1/3 of the space
                                weight(1f) - card - text("weight(1f)")

                                // This will take 2/3 of the space
                                weight(2f) - card - text("weight(2f)")
                            }
                        }
                        emphasized - text("Weighted elements take remaining space, and their natural size is ignored.")
                        text("This can creates these situations:")
                        example("""
                            row {
                                text("This text will never wrap.")
                                expanding - text("This text will wrap if there's insufficient space, because its size is fully determined by how much space we have left.")
                            }
                        """.trimIndent()) {
                            row {
                                text("This text will never wrap.")
                                expanding - text("This text will wrap if there's insufficient space, because its size is fully determined by how much space we have left.")
                            }
                        }
                    }

                    titledSection("Centering and Placing Items at the End") {
                        text("We can use the weight system mentioned above to move our elements to the center or end:")
                        example(
                            """
                            row {
                                expanding - space()
                                card - text("Stay")
                                card - text("centered")
                                expanding - space()
                            }
                            """.trimIndent()
                        ) {
                            row {
                                expanding - space()
                                card - text("Stay")
                                card - text("centered")
                                expanding - space()
                            }
                        }
                        example(
                            """
                            row {
                                expanding - space()
                                card - text("Move these")
                                card - text("to the end")
                            }
                            """.trimIndent()
                        ) {
                            row {
                                expanding - space()
                                card - text("Move these")
                                card - text("to the end")
                            }
                        }
                    }

                    titledSection("Alignment") {
                        text("By default, elements in a row or column take the entire space in the secondary direction (width in column, height in row).")
                        example("""
                            row {
                                card - text("We're\nobnoxiously\ntall\nbecause\nwe\nhave\nnewlines.")
                                card - text("Fine then, guess we'll be that tall.")
                            }
                        """.trimIndent()) {
                            row {
                                card - text("We're\nobnoxiously\ntall\nbecause\nwe\nhave\nnewlines.")
                                card - text("Fine then, guess we'll be that tall.")
                            }
                        }
                        text("However, there are times where we'd rather the child elements not take the whole space.  You can do this by setting an alignment with the atX modifiers:")
                        example("""
                            row {
                                card - text("default")
                                atTop - card - text("atTop")
                                centered - card - text("centered")
                                atBottom - card - text("atBottom")
                            }
                        """.trimIndent()) {
                            row {
                                card - text("default")
                                atTop - card - text("atTop")
                                centered - card - text("centered")
                                atBottom - card - text("atBottom")
                            }
                        }
                        example("""
                            col {
                                card - text("default")
                                atStart - card - text("atStart")
                                centered - card - text("centered")
                                atEnd - card - text("atEnd")
                            }
                        """.trimIndent()) {
                            col {
                                card - text("default")
                                atStart - card - text("atStart")
                                centered - card - text("centered")
                                atEnd - card - text("atEnd")
                            }
                        }
                    }

                    titledSection("Spacing") {
                        text("Spacing refers to the distance between elements in a row or column.")
                        text("By default, gap is controlled by the current theme, but this can be overridden per element.")
                        emphasized - text("Spacing set via 'gap = ' is per element and does not cascade to children.")

                        example(
                            """
                            row {
                                col {
                                    card - text("Default Spacing")
                                    card - text("A")
                                    card - text("B")
                                    card - text("C")
                                }
                                col {
                                    gap = 1.px
                                    card - text("gap = 1.px")
                                    card - text("A")
                                    card - text("B")
                                    card - text("C")
                                }
                                col {
                                    gap = 2.rem
                                    card - text("gap = 2.rem")
                                    card - text("A")
                                    card - text("B")
                                    card - text("C")
                                }
                            }
                        """.trimIndent()
                        ) {
                            row {
                                col {
                                    card - text("Default Spacing")
                                    card - text("A")
                                    card - text("B")
                                    card - text("C")
                                }
                                col {
                                    gap = 1.px
                                    card - text("gap = 1.px")
                                    card - text("A")
                                    card - text("B")
                                    card - text("C")
                                }
                                col {
                                    gap = 2.rem
                                    card - text("gap = 2.rem")
                                    card - text("A")
                                    card - text("B")
                                    card - text("C")
                                }
                            }
                        }

                        titledSection("Per-Element Spacing") {
                            emphasized - text("Please avoid using per-element gap.  You can frequently achieve the same layouts in simpler ways.")
                            text("For example, let's say you want some text elements closer together in a column than the other elements.")
                            text("The clearest way to represent this is by grouping elements.")
                            example("""
                                frame {
                                    centered - card - col {
                                        centered - icon(Icon.externalLink, "Link")

                                        // Clearly indicates the elements are associated with each other
                                        col {
                                            gap = 0.px
                                            h2("Some Important Link")
                                            subtext("Some subtext explaining it")
                                        }
                                    }
                                }
                            """.trimIndent()) {
                                frame {
                                    centered - card - col {
                                        centered - icon(Icon.externalLink, "Link")

                                        // Clearly indicates the elements are associated with each other
                                        col {
                                            gap = 0.px
                                            h2("Some Important Link")
                                            subtext("Some subtext explaining it")
                                        }
                                    }
                                }
                            }
                            emphasized - text("However, per-element gap useful in some situations.  Here's how to do it:")
                            example("""
                                col {
                                    card - text("Start")
                                    card - text("Normal gap above me")
                                    spacingOverrideBeforeNext(2.px)
                                    card - text("2.px above me")
                                    spacingOverrideBeforeNext(5.rem)
                                    card - text("5.rem above me")
                                }
                            """.trimIndent()) {
                                col {
                                    card - text("Start")
                                    card - text("Normal gap above me")
                                    spacingOverrideBeforeNext(2.px)
                                    card - text("2.px above me")
                                    spacingOverrideBeforeNext(5.rem)
                                    card - text("5.rem above me")
                                }
                            }
                        }
                    }
                }
                titledSection("Row Collapsing to Column") {
                    text("Sometimes you need a row for wide screens and a column for smaller screens.")
                    text("A highly effective way to do this is using 'rowCollapsingToColumn'.")
                    example("""
                        rowCollapsingToColumn(breakpoint = 60.rem) {
                            expanding - card - text("First Element")
                            expanding - card - text("Second Element")
                        }
                    """.trimIndent()) {
                        rowCollapsingToColumn(breakpoint = 60.rem) {
                            expanding - card - text("First Element")
                            expanding - card - text("Second Element")
                        }
                    }
                    text("This approach is optimized for CSS and involves no JS code execution when the breakpoint is hit.")
                    text("If the window's width is greater than the breakpoint, the layout will be horizontal and will use weights.")
                    text("If the window's width is less than the breakpoint, the layout will be vertical will ignore weights.")
                    emphasized - text("Again, rowCollapsingToColumn ignores weights/expanding when vertical.")
                    text("This is critical to the above layout: in vertical mode, if weights weren't ignored, the elements would typically be calculated to be height zero.")
                }
                titledSection("Simple List of Elements") {
                    text("Sometimes you have a fairly small list of data that you want to display.")
                    example("""
                        val strings = Signal(listOf("First", "Second", "Third", "Fourth"))
                        col {
                            forEach(strings) {
                                text { content = it }
                            }
                        }
                    """.trimIndent()) {
                        val strings = Signal(listOf("First", "Second", "Third", "Fourth"))
                        col {
                            forEach(strings) {
                                text { content = it }
                            }
                        }
                    }
                    text("Have a small dataset that needs to scroll add the scrolling\nor scrollingHorizontally view modifiers.")
                    example("""
                        sizeConstraints(height = 10.rem) - scrolling - col {
                            val strings = Signal((0..20).toList().map { "String ${'$'}it" })
                            forEach(strings) { string ->
                                text { content = string }
                            }
                        }""".trimIndent()) {
                        sizeConstraints(height = 10.rem) - scrolling - col {
                            val strings = Signal((0..20).toList().map { "String $it" })
                            forEach(strings) { string ->
                                text { content = string }
                            }
                        }
                    }
                }
                titledSection("Frame and Other Layouts") {
                    text("Frames are layouts that allow you to pull an element to a particular edge or corner and let them overlap.")
                    emphasized - text("All elements that aren't row or column are frames.")
                    text("By demonstration:")
                    example("""
                        frame {
                            atTopStart - card - text("atTopStart")
                            atBottom - card - text("atBottom")
                            centered - sizeConstraints(width = 10.rem, height = 10.rem) - important - text("Centered and on top")
                        }
                    """.trimIndent()) {
                        frame {
                            atTopStart - card - text("atTopStart")
                            atBottom - card - text("atBottom")
                            centered - sizeConstraints(width = 10.rem, height = 10.rem) - important - text("Centered and on top")
                        }
                    }
                    text("Elements are listed from bottom to top.  Imagine it as painting: the first thing you paint will get covered up by the things you paint later.")

                    titledSection("Alignment") {
                        text("Alignment can be controlled with the 'align' modifier.")
                        example("""
                            sizeConstraints(minHeight = 10.rem) - frame {
                                align(
                                    horizontal = Align.Start,
                                    vertical = Align.Start,
                                ) - card - text("Start/Start")
                                align(
                                    horizontal = Align.Stretch,
                                    vertical = Align.End,
                                ) - card - text("Stretch/End")
                                align(
                                    horizontal = Align.End,
                                    vertical = Align.Center,
                                ) - card - text("End/Center")
                                align(
                                    horizontal = Align.Center,
                                    vertical = Align.Center,
                                ) - card - text("Center/Center")
                            }
                        """.trimIndent()) {
                            sizeConstraints(minHeight = 10.rem) - frame {
                                align(
                                    horizontal = Align.Start,
                                    vertical = Align.Start,
                                ) - card - text("Start/Start")
                                align(
                                    horizontal = Align.Stretch,
                                    vertical = Align.End,
                                ) - card - text("Stretch/End")
                                align(
                                    horizontal = Align.End,
                                    vertical = Align.Center,
                                ) - card - text("End/Center")
                                align(
                                    horizontal = Align.Center,
                                    vertical = Align.Center,
                                ) - card - text("Center/Center")
                            }
                        }
                        text("I'd recommend using the shortcut words for less verbose code:")
                        example("""
                            sizeConstraints(minHeight = 10.rem) - frame {
                                atTopStart - card - text("atTopStart")
                                atCenterStart - card - text("atCenterStart")
                                atBottomStart - card - text("atBottomStart")
                                atTopCenter - card - text("atTopCenter")
                                centered - card - text("centered")
                                atBottomCenter - card - text("atBottomCenter")
                                atTopEnd - card - text("atTopEnd")
                                atCenterEnd - card - text("atCenterEnd")
                                atBottomEnd - card - text("atBottomEnd")
                            }
                        """.trimIndent()) {
                            sizeConstraints(minHeight = 10.rem) - frame {
                                atTopStart - card - text("atTopStart")
                                atCenterStart - card - text("atCenterStart")
                                atBottomStart - card - text("atBottomStart")
                                atTopCenter - card - text("atTopCenter")
                                centered - card - text("centered")
                                atBottomCenter - card - text("atBottomCenter")
                                atTopEnd - card - text("atTopEnd")
                                atCenterEnd - card - text("atCenterEnd")
                                atBottomEnd - card - text("atBottomEnd")
                            }
                        }
                        example("""
                            sizeConstraints(minHeight = 10.rem) - frame {
                                centeredHorizontally - danger - frame { centered - text("centeredHorizontally") }
                                atStart - card - frame { centered - text("atStart") }
                                atEnd - important - frame { centered - text("atEnd") }
                                atTop - critical - frame { centered - text("atTop") }
                                atBottom - affirmative - frame { centered - text("atBottom") }
                            }
                        """.trimIndent()) {
                            sizeConstraints(minHeight = 10.rem) - frame {
                                centeredHorizontally - danger - frame { centered - text("centeredHorizontally") }
                                atStart - card - frame { centered - text("atStart") }
                                atEnd - important - frame { centered - text("atEnd") }
                                atTop - critical - frame { centered - text("atTop") }
                                atBottom - affirmative - frame { centered - text("atBottom") }
                            }
                        }
                    }
                }

                titledSection("Explicit Sizing") {
                    text("You can provide explicit sizes and size rules to views using the 'sizeConstraints' modifier.")
                    example("""
                        frame {
                            centered -
                                    sizeConstraints(width = 10.rem, height = 10.rem) -
                                    card -
                                    text("I will always be a 10x10 square.")
                        }
                    """.trimIndent()) {
                        frame {
                            centered -
                                    sizeConstraints(width = 10.rem, height = 10.rem) -
                                    card -
                                    text("I will always be a 10x10 square.")
                        }
                    }
                    // TODO: Better explanation
                    text("Elements are not permitted to overrun their parent's size (except when it's scrollable), so you can use 'width' and 'height' to create elements that work across multiple screen sizes effectively.")
                    example("""
                        frame {
                            centered - 
                                    sizeConstraints(width = 20.rem) - 
                                    card - 
                                    text("This will be 20rem or less.  Try changing the screen's size to watch its behavior.")
                        }
                    """.trimIndent()) {
                        frame {
                            centered -
                                    sizeConstraints(width = 20.rem) -
                                    card -
                                    text("This will be 20rem or less.  Try changing the screen's size to watch its behavior.")
                        }
                    }
                    text("'minWidth' and 'minHeight' can be used to enforce a minimum size, but allow for an element to get larger.")
                    text("'maxWidth' and 'maxHeight' can be used to enforce a maximum size, but allow for an element to be naturally sized smaller.")
                    text("You usually don't want to use 'maxWidth' and 'maxHeight'.")
                }

                titledSection("Padding") {
                    emphasized - text("Padding rules apply to all layouts and views.")
                    text("Padding is determined as follows:")
                    text("- If explicitly set, padding is exactly as entered.")
                    text("- If the element has a background, the default padding from the theme is applied.")
                    text("- Otherwise, the element has no padding.")

                    titledSection("Removing or Explicitly Applying Default Padding") {
                        text("The 'padded' and 'unpadded' modifiers can do just that.")
                        example("""
                            col {
                                padded - text("Padding applied despite having no background")
                                unpadded - card - text("Padding not applied despite having background")
                            }
                        """.trimIndent()) {
                            col {
                                padded - text("Padding applied despite having no background")
                                unpadded - card - text("Padding not applied despite having background")
                            }
                        }
                    }

                    titledSection("Avoid explicit padding") {
                        text("Explicit padding frequently makes for cluttered code.  If theme-based padding works for you, don't put in explicit padding.")
                    }

                    titledSection("Explicit Padding") {
                        text("Explicit padding can be set for all sides individually or at the same time.")
                        example("""
                            col {
                                card - frame {
                                    paddingByEdge = Edges(left = 1.rem, top = 2.rem, right = 0.rem, bottom = 5.px)
                                    card - text {
                                        content = "Padding set per edge (left = 1.rem, top = 2.rem, right = 0.rem, bottom = 5.px)"
                                    }
                                }
                                card - frame {
                                    padding = 5.px
                                    card - text {
                                        content = "Padding set to 5px universally"
                                    }
                                }
                                card - frame {
                                    card - text {
                                        content = "Default padding"
                                    }
                                }
                            }
                        """.trimIndent()) {
                            col {
                                card - frame {
                                    paddingByEdge = Edges(left = 1.rem, top = 2.rem, right = 0.rem, bottom = 5.px)
                                    card - text {
                                        content = "Padding set per edge (left = 1.rem, top = 2.rem, right = 0.rem, bottom = 5.px)"
                                    }
                                }
                                card - frame {
                                    padding = 5.px
                                    card - text {
                                        content = "Padding set to 5px universally"
                                    }
                                }
                                card - frame {
                                    card - text {
                                        content = "Default padding"
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
