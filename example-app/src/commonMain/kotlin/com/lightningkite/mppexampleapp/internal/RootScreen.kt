package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.gc
import com.lightningkite.kiteui.models.Icon
import com.lightningkite.kiteui.navigation.Screen
import com.lightningkite.kiteui.reactive.invoke
import com.lightningkite.kiteui.reactive.onRemove
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.card
import com.lightningkite.kiteui.views.direct.button
import com.lightningkite.kiteui.views.direct.col
import com.lightningkite.kiteui.views.direct.h1
import com.lightningkite.kiteui.views.direct.link
import com.lightningkite.kiteui.views.direct.onClick
import com.lightningkite.kiteui.views.direct.row
import com.lightningkite.kiteui.views.direct.scrolls
import com.lightningkite.kiteui.views.direct.separator
import com.lightningkite.kiteui.views.direct.text
import com.lightningkite.kiteui.views.direct.weight
import com.lightningkite.kiteui.views.l2.icon
import com.lightningkite.mppexampleapp.docs.VideoElementScreen
import com.lightningkite.mppexampleapp.docs.ViewPagerElementScreen

@Routable("/internal")
object RootScreen : Screen {
    override fun ViewWriter.render() {
        scrolls - col {
            col {
                h1 { content = "Beautiful by default." }
                separator()
                text {
                    content =
                        "In KiteUI, styling is beautiful without effort.  No styling or manual CSS is required to get beautiful layouts.  Just how it should be."
                }
                text {
                    content = "Take a look below at some examples."
                }
                text {
                    content =
                        "Note the magnifying glass in the top right corner - clicking it will open the source of the current screen on GitHub!"
                }
            }
            col {

                fun ViewWriter.linkScreen(screen: () -> Screen) = link {
                    to = screen
                    row {
                        text {
                            ::content{ screen().title() }
//                            content  = screen.toString()
                        } in weight(1f)
                        icon(Icon.Companion.chevronRight, "Open")
                    }
                } in card

                linkScreen { ScrollIntoViewTest }
                linkScreen { FormattedInputTests() }
                linkScreen { TestingGroundScreen }
                linkScreen { ListEditScreen }
                linkScreen { LeakCheckerScreen }
                linkScreen { ExperimentScreen }
                linkScreen { AudioScreen }
                linkScreen { HorizontalRecyclerViewScreen }
                linkScreen { InfiniteImagesScreen }
                linkScreen { PlatformSpecificScreen }
                linkScreen { VideoElementScreen }
                linkScreen { ViewPagerElementScreen }
                linkScreen { ThemesScreen }
                linkScreen { ControlPerformanceTesting }
                linkScreen { ControlsScreen }
                linkScreen { FormsScreen }
                linkScreen { NavigationTestScreen }
                linkScreen { LayoutExamplesScreen }
                linkScreen { VectorsTestScreen }
                linkScreen { SampleLogInScreen }
                linkScreen { DataLoadingExampleScreen }
                linkScreen { LoadAnimationTestScreen }
                linkScreen { WebSocketScreen }
                linkScreen { CanvasSampleScreen }
                linkScreen { PongSampleScreen }
                linkScreen { ReactivityScreen }
                linkScreen { DialogSamplesScreen }
                linkScreen { ExternalServicesScreen }
                linkScreen { FullExampleScreen() }
                linkScreen { RecyclerViewScreen }
                linkScreen { PerformanceTestScreen }
                run {
                    val screen = { ArgumentsExampleScreen("test-id").also { it.toAdd.value = "Preset" } }
                    link {
                        to = screen
                        row {
                            text {
                                ::content{ screen().title() }
//                            content  = screen.toString()
                            } in weight(1f)
                            icon(Icon.Companion.chevronRight, "Open")
                        }
                        transitionId = "test-id"
                    } in card
                }

                button {
                    text { content = "GC" }
                    onClick { gc() }
                }

                calculationContext.onRemove {
                    println("Left root screen")
                }
            }
        }
    }
}