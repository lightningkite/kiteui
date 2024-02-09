package com.lightningkite.mppexampleapp.com.lightningkite.mppexampleapp.docs

import com.lightningkite.rock.Routable
import com.lightningkite.rock.contains
import com.lightningkite.rock.models.Align
import com.lightningkite.rock.models.dp
import com.lightningkite.rock.reactive.*
import com.lightningkite.rock.views.*
import com.lightningkite.rock.views.direct.*

@Routable("docs/viewpager")
object ViewPagerScreen: DocScreen {
    override val covers: List<String> = listOf("viewPager", "ViewPager")

    override fun ViewWriter.render() {
        scrolls - col {
            h1("View Pager")
            text("You can use a view pager to create an element that scrolls horizontally displaying pages.")
            text("This is frequently used for browsing photos or advertising features.")
            text("V2")
            val currentPage = Property(0)
            val items = Constant((1..3).toList())
            example("""
                val currentPage = Property(0)
                val items = Constant((1..3).toList())
                
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
                        text("Scroll to two (the last one)")
                        onClick {
                            currentPage.value = 2
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
                        text("Scroll to index two (the last one)")
                        onClick {
                            currentPage.value = 2
                        }
                    }
                }
            }
        }
    }

}