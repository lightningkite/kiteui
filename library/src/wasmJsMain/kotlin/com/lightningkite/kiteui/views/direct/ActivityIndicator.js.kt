package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.views.NView2
import com.lightningkite.kiteui.views.ViewDsl
import com.lightningkite.kiteui.views.ViewWriter
import kotlinx.dom.addClass
import org.w3c.dom.HTMLSpanElement

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual class NActivityIndicator(override val js: HTMLSpanElement): NView2<HTMLSpanElement>()

@ViewDsl
actual inline fun ViewWriter.activityIndicatorActual(crossinline setup: ActivityIndicator.() -> Unit): Unit =
    themedElement("span", ::NActivityIndicator) {
        js.addClass("spinner")
        setup(ActivityIndicator(this))
    }