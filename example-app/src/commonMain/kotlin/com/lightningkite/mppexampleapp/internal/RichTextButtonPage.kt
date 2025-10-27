package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.*
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*

@Routable("rich-text-button")
object RichTextButtonPage : Page {
    override val title: Reactive<String>
        get() = super.title

    override fun ViewWriter.render(): ViewModifiable = run {
        col {
            val incr = Signal(0)
            text { ::content { incr().toString() }}
            button {
                text("Normal Text")
                onClick { incr.value++ }
            }
            button {
                text {
                    setBasicHtmlContent("<b>Rich</b> <i>Text</i>")
                }
                onClick { incr.value++ }
            }
        }
    }
}