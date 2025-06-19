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

@Routable("experiment")
object ExperimentPage : Page {
    override val title: Readable<String>
        get() = super.title

    override fun ViewWriter.render(): ViewModifiable = run {
        col {
            text("Emoji test 😊")
            text("Emoji test \uD83D\uDE0A")
            text { setBasicHtmlContent("Emoji test 😊") }
            text { setBasicHtmlContent("<strong>Emoji test 😊</strong>") }
            text { setBasicHtmlContent("Emoji test \uD83D\uDE0A") }
            text { setBasicHtmlContent("<strong>Emoji test \uD83D\uDE0A</strong>") }
        }
    }
}
