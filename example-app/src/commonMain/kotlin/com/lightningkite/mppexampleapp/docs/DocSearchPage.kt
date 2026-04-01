package com.lightningkite.mppexampleapp.docs

import com.lightningkite.kiteui.LogRoot
import com.lightningkite.kiteui.QueryParameter
import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.l2.RecyclerViewPlacerVerticalGrid
import com.lightningkite.kiteui.views.l2.children
import com.lightningkite.reactive.core.*

@Routable("docs")
object DocSearchPage : Page {

    @QueryParameter
    val query = Signal<String>("")

    val docsPages = Signal(listOf(
        // TODO: Gradle tasks
        // TODO: Platform-specific views
        // TODO: Custom widgets
        { ThemeTesterPage },
        { ResourcesPage },
        { DataPage },
        { ReactiveToolsPage },
        { ThemingPage },
        { TextElementPage },
        { NavigationPage },
        { VideoElementPage },
        { ViewPagerElementPage },
        { ImageElementPage },
        { ImageCompressionPage },
        { ZoomableImageElementPage },
        { IconsPage },
        { ViewModifiersPage },
        { LayoutPage },
        { RecyclerViewPage },
        { CheatSheet }
    ))

    override fun ElementWriter.CanAddTheme.render(): Unit = run {
        frame {
            align(Align.Center, Align.Stretch).sizedBox(SizeConstraints(width = 80.rem)).col  {
                h1("Documentation")
                text("Here you can find many helpful pages for understanding KiteUI and its tools.")
                row {
                    centered.icon { source = Icon.search }
                    expanding.textField {
                        content bind query
                    }
                    centered.button {
                        icon { source = Icon.close }
                        onClick {
                            query set ""
                        }
                    }
                }
                expanding.onNext(ListSemantic).recyclerView {
                    paddingByEdge = Edges(
                        left = 0.rem,
                        top = 0.rem,
                        right = 0.rem,
                        bottom = 10.rem
                    )
                    placer = RecyclerViewPlacerVerticalGrid(1).apply { log = LogRoot.tag("placer") }
                    children(remember {
                        docsPages().mapNotNull {
                            val q = query()
                            if (q.isBlank()) return@mapNotNull it to it().covers
                            val matchingTerms = it().covers.filter { term ->
                                q.split(' ').all { part -> term.contains(part, ignoreCase = true) }
                            }
                            if (matchingTerms.isEmpty()) return@mapNotNull null
                            it to matchingTerms
                        }
                    }, { it }) {
                        card.link {
                            ::to { it().first }
                            col {
                                gap = 0.25.rem
                                text { ::content { it().first().title() } }
                                subtext { ::content { it().second.joinToString() }}
                            }
                        }
                    }
                }
            }
        }
    }
}
