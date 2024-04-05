package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.views.NView
import com.lightningkite.kiteui.views.NView2
import com.lightningkite.kiteui.views.ViewDsl
import com.lightningkite.kiteui.views.ViewWriter
import kotlinx.dom.addClass
import org.w3c.dom.HTMLProgressElement
import org.w3c.dom.HTMLSpanElement

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual class NProgressBar(override val js: HTMLProgressElement): NView2<HTMLProgressElement>()

@ViewDsl
actual inline fun ViewWriter.progressBarActual(crossinline setup: ProgressBar.() -> Unit): Unit =
    themedElement("progress", ::NProgressBar) {
        setup(ProgressBar(this))
    }

actual var ProgressBar.ratio: Float
    get() = native.js.value.toFloat()
    set(value) { native.js.value = value.toDouble() }