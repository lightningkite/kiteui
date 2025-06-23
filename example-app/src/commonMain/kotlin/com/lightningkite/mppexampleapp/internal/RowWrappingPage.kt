package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.*
import com.lightningkite.kiteui.models.Icon
import com.lightningkite.kiteui.models.ListSemantic
import com.lightningkite.kiteui.models.SelectedSemantic
import com.lightningkite.kiteui.models.rem
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.readable.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.l2.Recycler2
import com.lightningkite.kiteui.views.l2.children
import com.lightningkite.kiteui.views.l2.icon
import com.lightningkite.readable.invoke
import kotlinx.coroutines.delay
import kotlinx.datetime.LocalDate
import kotlin.coroutines.CoroutineContext
import kotlin.random.Random

@Routable("row-wrapping")
object RowWrappingPage : Page {
    override val title: Readable<String>
        get() = super.title

    override fun ViewWriter.render(): ViewModifiable = scrolling - col {
        rowWrapping {
            repeat(100) {
                card - text(listOf(
                    "Longer text",
                    "Short",
                    "Much longer text",
                    "Hello world!",
                    "OK"
                ).random())
            }
        }
    }
}
