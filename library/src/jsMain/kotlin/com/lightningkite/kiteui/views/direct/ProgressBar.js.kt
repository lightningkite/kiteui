package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.views.ViewDsl
import ViewWriter
import org.w3c.dom.HTMLProgressElement

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual typealias NProgressBar = HTMLProgressElement

@ViewDsl
actual fun ViewWriter.progressBarActual(setup: ProgressBar.() -> Unit) {
    themedElement<NProgressBar>("progress") {
        setup(ProgressBar(this))
    }
}

actual var ProgressBar.ratio: Float
    get() = native.value.toFloat()
    set(value) {
        native.value = value.toDouble()
    }