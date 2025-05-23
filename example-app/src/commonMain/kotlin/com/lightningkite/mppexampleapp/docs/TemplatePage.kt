package com.lightningkite.mppexampleapp.docs

import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.ExternalServices
import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.models.*
import com.lightningkite.signal.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import kotlin.random.Random

object TemplatePage: DocPage {
    override val title: Readable<String>
        get() = Constant("Name of Topic")
    override val covers: List<String> = listOf("topic")

    override fun ViewWriter.render(): ViewModifiable = run {
        article {
            h1("Name of Topic")
            text("Some instruction goes here.")
            example("""
                text("HI")
                """.trimIndent()) {
                text("HI")
            }
        }
    }

}