package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.gc
import com.lightningkite.kiteui.models.Icon
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.views.ViewModifiable
import com.lightningkite.readable.invoke
import com.lightningkite.readable.onRemove
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.card
import com.lightningkite.kiteui.views.direct.Frame
import com.lightningkite.kiteui.views.direct.button
import com.lightningkite.kiteui.views.direct.col
import com.lightningkite.kiteui.views.direct.frame
import com.lightningkite.kiteui.views.direct.h1
import com.lightningkite.kiteui.views.direct.link
import com.lightningkite.kiteui.views.direct.onClick
import com.lightningkite.kiteui.views.direct.row
import com.lightningkite.kiteui.views.direct.scrolling
import com.lightningkite.kiteui.views.direct.separator
import com.lightningkite.kiteui.views.direct.switch
import com.lightningkite.kiteui.views.direct.text
import com.lightningkite.kiteui.views.direct.weight
import com.lightningkite.kiteui.views.l2.icon
import com.lightningkite.mppexampleapp.docs.VideoElementPage
import com.lightningkite.mppexampleapp.docs.ViewPagerElementPage

@Routable("/internal")
object RootPage : Page {
    override fun ViewWriter.render(): ViewModifiable = run {
        scrolling - col {
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

                fun ViewWriter.linkPage(screen: () -> Page) = link {
                    to = screen
                    row {
                        text {
                            ::content{ screen().title() }
//                            content  = screen.toString()
                        } in weight(1f)
                        icon(Icon.Companion.chevronRight, "Open")
                    }
                } in card

                linkPage { RichTextButtonPage }
                linkPage { R2VPPage }
                linkPage { ProgrammaticLayoutTestPage }
                linkPage { SpecialScrollTest }
                linkPage { ScrollIntoViewTest }
                linkPage { FormattedInputTests() }
                linkPage { TestingGroundPage }
                linkPage { ListEditPage }
                linkPage { LeakCheckerPage }
                linkPage { ExperimentPage }
                linkPage { Recycler2TestPage }
                linkPage { AudioPage }
                linkPage { HorizontalRecyclerViewPage }
                linkPage { InfiniteImagesPage }
                linkPage { PlatformSpecificPage }
                linkPage { VideoElementPage }
                linkPage { ViewPagerElementPage }
                linkPage { ThemesPage }
                linkPage { ControlsPage }
                linkPage { FormsPage }
                linkPage { NavigationTestPage }
                linkPage { LayoutExamplesPage }
                linkPage { VectorsTestPage }
                linkPage { SampleLogInPage }
                linkPage { DataLoadingExamplePage }
                linkPage { LoadAnimationTestPage }
                linkPage { WebSocketPage }
                linkPage { CanvasSamplePage }
                linkPage { PongSamplePage }
                linkPage { ReactivityPage }
                linkPage { DialogSamplesPage }
                linkPage { ExternalServicesPage }
                linkPage { FullExampleScreen() }
                linkPage { RecyclerViewPage }
                linkPage { PerformanceTestPage }
                run {
                    val screen = { ArgumentsExamplePage("test-id").also { it.toAdd.value = "Preset" } }
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
                var retained: Any? = null
                button {
                    text { content = "Cause Leak" }
                    onClick {
                        val f = frame {}
                        retained = f
                        removeChild(f)
                    }
                }

                calculationContext.onRemove {
                    println("Left root screen")
                }
            }
        }
    }
}