package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.gc
import com.lightningkite.kiteui.models.Icon
import com.lightningkite.kiteui.models.ListSemantic
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.card
import com.lightningkite.kiteui.views.direct.button
import com.lightningkite.kiteui.views.direct.col
import com.lightningkite.kiteui.views.direct.frame
import com.lightningkite.kiteui.views.direct.h1
import com.lightningkite.kiteui.views.direct.h2
import com.lightningkite.kiteui.views.direct.link
import com.lightningkite.kiteui.views.direct.onClick
import com.lightningkite.kiteui.views.direct.row
import com.lightningkite.kiteui.views.direct.scrolling
import com.lightningkite.kiteui.views.direct.separator
import com.lightningkite.kiteui.views.direct.space
import com.lightningkite.kiteui.views.direct.subtext
import com.lightningkite.kiteui.views.direct.text
import com.lightningkite.kiteui.views.direct.weight
import com.lightningkite.kiteui.views.expanding
import com.lightningkite.kiteui.views.l2.icon
import com.lightningkite.mppexampleapp.docs.VideoElementPage
import com.lightningkite.mppexampleapp.docs.ViewPagerElementPage
import com.lightningkite.mppexampleapp.internal.SliderExamplePage
import com.lightningkite.mppexampleapp.internal.ColorTestPage
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*

@Routable("/internal")
object RootPage : Page {
    override val title: Reactive<String> = Constant("Test Pages")
    override fun ViewWriter.render(): Unit = run {
        scrolling.col {
            col {
                h1 { content = "Test Pages" }
                separator()
                text("These test pages aren't necessarily meant to be examples of what you can do with KiteUI.  They are meant to be a way to test out new features and find bugs.  They are also meant to be a way to test out different layouts and components.  However, you may find them interesting.")
            }
            space()
            ListSemantic.onNext.col {

                fun ViewWriter.linkPage(screen: () -> Page) = card.link {
                    to = screen
                    row {
                        expanding.text {
                            ::content{ screen().title() }
                        }
                        icon(Icon.Companion.chevronRight, "Open")
                    }
                }


                fun ViewWriter.sectionLabel(label: String) {
                    space()
                    subtext { content = label }
                }

                sectionLabel("Core UI & Controls")
                linkPage { ControlsPage }
                linkPage { ThemesPage }
                linkPage { ColorTestPage }
                linkPage { SliderExamplePage }
                linkPage { DialogSamplesPage }
                linkPage { DatePickerExamplePage }



                sectionLabel("Layout & Containers")
                linkPage { LayoutExamplesPage }
                linkPage { ContainerAlignmentDemoPage }
                linkPage { RowWrappingPage }
                linkPage { ProgrammaticLayoutTestPage }
                linkPage { FullScreenPage() }
                linkPage { SwapViewPage }
                linkPage { ViewPagerElementPage }

                sectionLabel("Navigation")
                linkPage { NavigationTestPage }
                linkPage { TestingGroundPage }

                sectionLabel("Forms & Input")
                linkPage { FormsPage }
                linkPage { FormattedInputTests() }
                linkPage { RichTextButtonPage }
                linkPage { SampleLogInPage }

                sectionLabel("Scrolling & Visibility")
                linkPage { SpecialScrollTest }
                linkPage { ScrollIntoViewTest }
                linkPage { CoveringTestPage }
                linkPage { PopoverTestingPage }
                linkPage { NestedPopoverTestPage }

                sectionLabel("Recycler / Lists")
                linkPage { RecyclerViewTestPage }
                linkPage { Recycler2TestPage }
                linkPage { RecyclerFilterTestPage }
                linkPage { HorizontalRecyclerViewPage }
                linkPage { InfiniteImagesPage }
                linkPage { R2VPPage }
                linkPage { Recycler2PullToRefreshTest }


                sectionLabel("Media")
                linkPage { ImageTestPage }
                linkPage { VideoElementPage }
                linkPage { AudioPage }
                linkPage { AudioTestPage }
                linkPage { CameraScannerTestPage }

                sectionLabel("Animation")
                linkPage { LoadAnimationTestPage }
                linkPage { LottieExamplePage }
                linkPage { LottieRendererTestPage }

                sectionLabel("Graphics & Canvas")
                linkPage { CanvasSamplePage }
                linkPage { CanvasApiTestPage }
                linkPage { VectorsTestPage }
                linkPage { GraphExamplePage }
                linkPage { PongSamplePage }

                sectionLabel("Data & Networking")
                linkPage { DataLoadingExamplePage }
                linkPage { WebSocketPage }
                linkPage { ExternalServicesPage }
                linkPage { SsrResourceExamplePage() }

                sectionLabel("Reactivity & State")
                linkPage { ReactivityPage }

                sectionLabel("Platform & Device")
                linkPage { PlatformSpecificPage }

                sectionLabel("Performance & Diagnostics")
                linkPage { PerformanceTestPage }
                linkPage { LeakCheckerPage }

                sectionLabel("Interaction & Gestures")
                linkPage { ClickTestPage }
                linkPage { DragPage }

                sectionLabel("Misc / Experimental")
                linkPage { ExperimentPage }

                run {
                    val screen = { ArgumentsExamplePage("test-id").also { it.toAdd.value = "Preset" } }
                    card.link {
                        to = screen
                        row {
                            weight(1f).text {
                                ::content{ screen().title() }
//                            content  = screen.toString()
                            }
                            icon(Icon.Companion.chevronRight, "Open")
                        }
                        transitionId = "test-id"
                    }
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
