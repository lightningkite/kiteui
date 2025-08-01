package com.lightningkite.mppexampleapp.docs

import com.lightningkite.kiteui.ConsoleRoot
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.QueryParameter
import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.signal.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.l2.RecyclerViewPlacerVerticalGrid
import com.lightningkite.kiteui.views.l2.children

@Routable("docs")
public object DocSearchPage : Page {

    @QueryParameter
    public val query = Property<String>("")

    public val docsPages = Property(listOf(
        // TODO: Gradle tasks
        // TODO: Resources
        // TODO: Platform-specific views
        // TODO: Custom widgets
        { DataPage },
        { ReactiveToolsPage },
        { ThemingPage },
        { TextElementPage },
        { NavigationPage },
        { VideoElementPage },
        { ViewPagerElementPage },
        { ImageElementPage },
        { ZoomableImageElementPage },
        { IconsPage },
        { ViewModifiersPage },
        { LayoutPage },
        { CheatSheet }
    ))

    public override fun ViewWriter.render(): ViewModifiable = run {
        frame {
            align(Align.Center, Align.Stretch) - sizedBox(SizeConstraints(width = 80.rem)) - col  {
                h1("Documentation")
                text("Here you can find many helpful pages for understanding KiteUI and its tools.")
                row {
                    centered - icon { source = Icon.search }
                    expanding - textField {
                        content bind query
                    }
                    centered - button {
                        gap = 0.1.rem
                        icon { source = Icon.close }
                        onClick {
                            query set ""
                        }
                    }
                }
                expanding - ListSemantic.onNext - recyclerView {
                    placer = RecyclerViewPlacerVerticalGrid(1).apply { log = ConsoleRoot.tag("placer") }
                    children(shared {
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
                        card - link {
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
