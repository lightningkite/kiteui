package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.models.ListSemantic
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.direct.col
import com.lightningkite.kiteui.views.direct.recyclerView
import com.lightningkite.kiteui.views.direct.row
import com.lightningkite.kiteui.views.direct.text
import com.lightningkite.kiteui.views.direct.textInput
import com.lightningkite.kiteui.views.expanding
import com.lightningkite.kiteui.views.fieldTheme
import com.lightningkite.kiteui.views.l2.children
import com.lightningkite.reactive.core.remember
import com.lightningkite.reactive.core.Signal

@Routable("recycler-filter-test")
object RecyclerFilterTestPage : Page {
    val searchText = Signal("")

    override fun ElementWriter.CanAddTheme.render(): Unit {
        col {

            fieldTheme.row {
                expanding.textInput {
                    hint = "Search"
                    content bind searchText
                }
            }

            expanding.col {
                expanding.themed(ListSemantic).recyclerView {
                    children(
                        items = remember {
                            listOf("asdf", "asdf1", "qwerty").filter {
                                it.contains(searchText().lowercase())
                            }
                        },
                        id = { it },
                        render = {
                            text {
                                ::content { it() }
                            }
                        }
                    )
                }
            }
        }
    }
}