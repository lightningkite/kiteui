package com.lightningkite.mppexampleapp.docs

import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*

object TemplatePage: DocPage {
    override val title: Reactive<String>
        get() = Constant("Name of Topic")
    override val covers: List<String> = listOf("topic")

    override fun ViewWriter.render(): Unit = run {
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