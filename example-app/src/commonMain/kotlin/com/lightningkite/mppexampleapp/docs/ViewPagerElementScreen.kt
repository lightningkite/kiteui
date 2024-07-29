package com.lightningkite.mppexampleapp.docs

import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*

@Routable("docs/viewpager")
object ViewPagerElementScreen: DocScreen {
    override val covers: List<String> = listOf("viewPager", "ViewPager")

    override fun ViewWriter.render() {
        article {
            h1("View Pager")
            text("You can use a view pager to create an element that scrolls horizontally displaying pages.")
            text("This is frequently used for browsing photos or advertising features.")
            text("V2")
            val currentPage = Property(9)
            val items = Constant((1..30).toList())
            example("""
                val currentPage = Property(9)
                val items = Constant((1..30).toList())
                
                card - viewPager {
                    // Bind the current index of the ViewPager to `currentPage`
                    index bind currentPage
                    
                    // Define what to show here
                    children(items) {
                        stack {
                            centered - text { ::content { "Screen ${'$'}{it.await()}" } }
                        }
                    }
                }
                """.trimIndent()) {
                card - viewPager {
                    // Bind the current index of the ViewPager to `currentPage`
                    index bind currentPage

                    // Define what to show here
                    children(items) {
                        stack {
                            centered - text { ::content { "Screen ${it.await()}" } }
                        }
                    }
                }
            }
            text("You can scroll to certain pages by using 'index'.")
            example(
                """
                col {
                    important - button {
                        text("Scroll to zero (the first one)")
                        onClick {
                            currentPage.value = 0
                        }
                    }
                    important - button {
                        text("Scroll to twenty nine (the last one)")
                        onClick {
                            currentPage.value = 29
                        }
                    }
                }
                """.trimIndent()
            ) {
                col {
                    important - button {
                        text("Scroll to index zero (the first one)")
                        onClick {
                            currentPage.value = 0
                        }
                    }
                    important - button {
                        text("Scroll to index twenty nine (the last one)")
                        onClick {
                            currentPage.value = 29
                        }
                    }
                }
            }
        }
    }

}