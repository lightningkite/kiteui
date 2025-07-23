package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.gc
import com.lightningkite.kiteui.models.Icon
import com.lightningkite.kiteui.models.ListSemantic
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.views.ViewModifiable
import com.lightningkite.readable.onRemove
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.card
import com.lightningkite.kiteui.views.direct.button
import com.lightningkite.kiteui.views.direct.col
import com.lightningkite.kiteui.views.direct.frame
import com.lightningkite.kiteui.views.direct.h1
import com.lightningkite.kiteui.views.direct.link
import com.lightningkite.kiteui.views.direct.onClick
import com.lightningkite.kiteui.views.direct.row
import com.lightningkite.kiteui.views.direct.scrolling
import com.lightningkite.kiteui.views.direct.separator
import com.lightningkite.kiteui.views.direct.text
import com.lightningkite.kiteui.views.direct.weight
import com.lightningkite.kiteui.views.l2.icon
import com.lightningkite.mppexampleapp.docs.VideoElementPage
import com.lightningkite.mppexampleapp.docs.ViewPagerElementPage
import com.lightningkite.mppexampleapp.internal.SliderExamplePage
import com.lightningkite.readable.Constant
import com.lightningkite.readable.Readable

@Routable("/internal")
object RootPage : Page {
    override val title: Readable<String> = Constant("Test Pages")
    override fun ViewWriter.render(): ViewModifiable = run {
        scrolling - col {
            col {
                h1 { content = "Test Pages" }
                separator()
                text("These test pages aren't necessarily meant to be examples of what you can do with KiteUI.  They are meant to be a way to test out new features and find bugs.  They are also meant to be a way to test out different layouts and components.  However, you may find them interesting.")
            }
            ListSemantic.onNext - col {

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

                linkPage { RowWrappingPage }
                linkPage { CoveringTestPage }
                linkPage { PopoverTestingPage }
                linkPage { SwapViewPage }
                linkPage { DragPage }
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
                linkPage { SliderExamplePage }
                linkPage { FormsPage }
                linkPage { NavigationTestPage }
                linkPage { LayoutExamplesPage }
                linkPage { VectorsTestPage }
                linkPage { SampleLogInPage }
                linkPage { DataLoadingExamplePage }
                linkPage { LoadAnimationTestPage }
                linkPage { WebSocketPage }
                linkPage { CanvasSamplePage }
                linkPage { GraphExamplePage }
                linkPage { PongSamplePage }
                linkPage { ReactivityPage }
                linkPage { DialogSamplesPage }
                linkPage { ExternalServicesPage }
                linkPage { FullScreenPage() }
                linkPage { RecyclerViewTestPage }
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
