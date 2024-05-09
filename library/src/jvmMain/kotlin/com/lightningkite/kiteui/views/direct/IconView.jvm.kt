package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.dom.HTMLElement
import com.lightningkite.kiteui.models.Icon
import com.lightningkite.kiteui.views.ViewDsl
import ViewWriter

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual typealias NIconView = HTMLElement

@ViewDsl
actual inline fun ViewWriter.iconActual(crossinline setup: IconView.() -> Unit) {
}

actual inline var IconView.source: Icon?
    get() = TODO()
    set(value) {
    }

actual inline var IconView.description: String?
    get() = TODO()
    set(value) {
    }
