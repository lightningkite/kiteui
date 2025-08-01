package com.lightningkite.mppexampleapp.docs

import com.lightningkite.kiteui.ConsoleRoot
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.models.rem
import com.lightningkite.signal.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*

@Routable("docs/viewpager")
public object ViewPagerElementPage: DocPage {
    public override val covers: List<String> = listOf("viewPager", "ViewPager")

    public override fun ViewWriter.render(): ViewModifiable = run {
        article {
            h1("View Pager")
            text("You can use a view pager to create an element that scrolling horizontally displaying pages.")
            text("This is frequently used for browsing photos or advertising features.")
            text("V2")
            val currentPage = Property(9)
            val items = Constant((1..30).toList())
            example("""
                val currentPage = Property(9)
                val items = Constant((1..30).toList())
                
                sizeConstraints(height = 10.rem) - card - viewPager {
                    // Bind the current index of the ViewPager to `currentPage`
                    index bind currentPage
                    
                    // Define what to show here
                    children(items) {
                        frame {
                            centered - text { ::content { "Page ${'$'}{it.await()}" } }
                        }
                    }
                }
                """.trimIndent()) {
                sizeConstraints(height = 10.rem) - card - viewPager {
                    new.log = ConsoleRoot.tag("Viewpager")
                    // Bind the current index of the ViewPager to `currentPage`
                    index bind currentPage

                    // Define what to show here
                    children(items) {
                        frame {
                            centered - text { ::content { "Page ${it()}" } }
                        }
                    }
                }
            }
            text("You can scroll to certain pages by using 'index'.")
            example(
                """
                col {
                    text {
                        ::content { "Current index; ${'$'}{currentPage()}" }
                    }
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
                    text {
                        ::content { "Current index; ${currentPage()}" }
                    }
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