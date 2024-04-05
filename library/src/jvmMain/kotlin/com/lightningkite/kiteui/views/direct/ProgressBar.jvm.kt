package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.dom.HTMLProgressElement
import com.lightningkite.kiteui.views.NView
import com.lightningkite.kiteui.views.ViewDsl
import com.lightningkite.kiteui.views.ViewWriter

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual typealias NProgressBar = HTMLProgressElement

@ViewDsl
actual fun ViewWriter.progressBarActual(setup: ProgressBar.() -> Unit) {
}

actual var ProgressBar.ratio: Float
    get() = TODO("Not yet implemented")
    set(value) {}