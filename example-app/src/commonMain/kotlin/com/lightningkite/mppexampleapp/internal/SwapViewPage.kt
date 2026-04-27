package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.Icon
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.models.ScreenTransition
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.card
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.expanding
import com.lightningkite.kiteui.views.l2.RecyclerViewPlacerVerticalGrid
import com.lightningkite.kiteui.views.l2.children
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable


@Serializable
enum class ViewMode {
    Grid,
    List
}

data class Test(
    val id: String,
    val title: String
)

@Routable("swapview")
object SwapViewPage : Page {
    override fun ViewWriter.render() {
        val clock = reactiveProcess {
            var tick = 0
            while (true) {
                emit(tick++)
                delay(2000)  // 2 second intervals for better visibility
            }
        }


        val testItems = remember {
            listOf(Test("1", "test"), Test("2", "test"), Test("3", "test"), Test("4", "test"))
        }

        col {

            val viewMode = PersistentProperty("viewMode", ViewMode.Grid)

            card.button {
                icon {
                    ::source { if (viewMode() == ViewMode.Grid) Icon.menu else Icon.list }
                    description = "Toggle view"
                }
                onClick {
                    viewMode.value = if (viewMode.value == ViewMode.Grid) ViewMode.List else ViewMode.Grid
                }
            }


            expanding.swapView {
                swapping(
                    current = { viewMode() },
                    views = { mode ->


                        when (mode) {
                            ViewMode.Grid -> {
                                expanding.recyclerView {
                                    ::placer { RecyclerViewPlacerVerticalGrid(2) }
                                    children(testItems, { it.id }) { item ->
                                        card.col {
                                            text {
                                                ::content{
                                                    item().title
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            ViewMode.List -> {
                                expanding.recyclerView {
                                    children(testItems, { it }) { item ->
                                        card.col {
                                            text {
                                                ::content{
                                                    item().title
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                )
            }




            // Fade transition
//            row {
//                text("Fade:")
//                swapView {
//                    swapping(
//                        transition = { ScreenTransition.Fade },
//                        current = { clock() },
//                        views = { tick -> h1("${(tick % 3) + 1}") }
//                    )
//                }
//            }
//
//            // Push transition (slide left)
//            row {
//                text("Push:")
//                swapView {
//                    swapping(
//                        transition = { ScreenTransition.Push },
//                        current = { clock() },
//                        views = { tick -> h1("${(tick % 3) + 1}") }
//                    )
//                }
//            }
//
//            // Pop transition (slide right)
//            row {
//                text("Pop:")
//                swapView {
//                    swapping(
//                        transition = { ScreenTransition.Pop },
//                        current = { clock() },
//                        views = { tick -> h1("${(tick % 3) + 1}") }
//                    )
//                }
//            }
//
//            // PullUp transition
//            row {
//                text("PullUp:")
//                swapView {
//                    swapping(
//                        transition = { ScreenTransition.PullUp },
//                        current = { clock() },
//                        views = { tick -> h1("${(tick % 3) + 1}") }
//                    )
//                }
//            }
//
//            // PullDown transition
//            row {
//                text("PullDown:")
//                swapView {
//                    swapping(
//                        transition = { ScreenTransition.PullDown },
//                        current = { clock() },
//                        views = { tick -> h1("${(tick % 3) + 1}") }
//                    )
//                }
//            }
//
//            // GrowFade transition
//            row {
//                text("GrowFade:")
//                swapView {
//                    swapping(
//                        transition = { ScreenTransition.GrowFade },
//                        current = { clock() },
//                        views = { tick -> h1("${(tick % 3) + 1}") }
//                    )
//                }
//            }
//
//            // ShrinkFade transition
//            row {
//                text("ShrinkFade:")
//                swapView {
//                    swapping(
//                        transition = { ScreenTransition.ShrinkFade },
//                        current = { clock() },
//                        views = { tick -> h1("${(tick % 3) + 1}") }
//                    )
//                }
//            }
        }
    }
}