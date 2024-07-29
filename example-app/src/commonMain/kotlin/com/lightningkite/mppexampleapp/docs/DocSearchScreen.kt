package com.lightningkite.mppexampleapp.docs

import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.QueryParameter
import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.navigation.Screen
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*

@Routable("docs")
object DocSearchScreen : Screen {

    @QueryParameter
    val query = Property<String>("")

    val docsPages = Property(listOf(
        { TextElementScreen },
        { DataScreen },
        { NavigationScreen },
        { VideoElementScreen },
        { ViewPagerElementScreen },
        { ImageElementScreen },
        { IconsScreen },
        { ViewModifiersScreen },
        { LayoutScreen }
    ))

    override fun ViewWriter.render() {
        stack {
            gravity(Align.Center, Align.Stretch) - sizedBox(SizeConstraints(width = 80.rem)) - col  {
                h1("Documentation")
                text("Here you can find many helpful pages for understanding KiteUI and its tools.")
                row {
                    centered - icon { source = Icon.search }
                    expanding - textField {
                        content bind query
                    }
                    centered - button {
                        spacing = 0.1.rem
                        icon { source = Icon.close }
                        onClick {
                            query set ""
                        }
                    }
                }
                expanding - recyclerView {
                    children(shared {
                        docsPages().mapNotNull {
                            val q = query.await()
                            if(q.isBlank()) return@mapNotNull it to it().covers
                            val matchingTerms = it().covers.filter { term ->
                                q.split(' ').all { part -> term.contains(part, ignoreCase = true) }
                            }
                            if(matchingTerms.isEmpty()) return@mapNotNull null
                            it to matchingTerms
                        }
                    }) {
                        card - link {
                            ::to { it.await().first }
                            col {
                                spacing = 0.25.rem
                                text { ::content { it.await().first().title.await() } }
                                subtext { ::content { it.await().second.joinToString() }}
                            }
                        }
                    }
                }
            }
        }
    }
}
