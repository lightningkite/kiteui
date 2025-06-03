package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.*
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.readable.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*

@Routable("rich-text-button")
object RichTextButtonPage : Page {
    override val title: Readable<String>
        get() = super.title

    override fun ViewWriter.render(): ViewModifiable = run {
        col {
            val incr = Property(0)
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