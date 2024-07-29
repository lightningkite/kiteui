package com.lightningkite.mppexampleapp.docs

import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.contains
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*

@Routable("docs/layout")
object LayoutScreen: DocScreen {
    override val covers: List<String> = listOf("layout", "Layout")

    override fun ViewWriter.render() {
        article {
            h1("Layout")
            h2("Column")
            text("The column layout is very simple it places the items vertically across the view.")
            example("""
                card -col {
                    text("Item 1")
                    text("Item 2")
                    text("Item 3")
                }
            """.trimIndent()) {
                card - col {
                    text("This is a column")
                    text("This is a column")
                }
            }

            h2("Row")
            text("The row layout is the same as the column layout, but it places the items horizontally across the view.")
            example("""
                card - row {
                    text("Item 1")
                    text("Item 2")
                    text("Item 3")
                }
            """.trimIndent()) {
                card - row {
                    text("Item 1")
                    text("Item 2")
                    text("Item 3")
                }
            }

            h2("Stack Layout")
            text("Stack layout allows for items to be stacked on top of each other.  This is useful for creating overlays. The last item in the stack is on top. This could hide views behind other views")
            example("""
                card - col {
                    stack {
                        text("Last Item")
                        compact - card - text("Behind ") in tweakTheme {  it.copy(foreground = Color(0.5f, 0f, 0f, 1f)) }
                        text("In front") in tweakTheme {  it.copy(foreground = Color(1f, 0f, 0.2f, 1f)) }
                    }
                }
            """.trimIndent()) {
                card - col {
                    stack {
                        text("Last Item")
                        compact - card - text("Behind ") in tweakTheme {  it.copy(foreground = Color(0.5f, 0f, 0f, 1f)) }
                        text("In front") in tweakTheme {  it.copy(foreground = Color(1f, 0f, 0.2f, 1f)) }
                    }
                }
            }
            text("Using the gravity modifier with the stack layout, you can align items within the stack.")
            example(
                """
            card - col {
                stack {
                    val aligns = listOf(Align.Start, Align.Center, Align.End)
                    for (h in aligns) {
                        for (v in aligns) {
                            text { content = "$\h $\v" } in gravity(h, v)
                        }
                    }
                } in sizedBox(SizeConstraints(minHeight = 200.px))
            }
                """.trimIndent()
            ) {
                card - col {
                    stack {
                        val aligns = listOf(Align.Start, Align.Center, Align.End)
                        for (h in aligns) {
                            for (v in aligns) {
                                text { content = "$h $v" } in gravity(h, v)
                            }
                        }
                    } in sizedBox(SizeConstraints(minHeight = 200.px))
                }
            }
        }
        }
    }
