package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.contains
import com.lightningkite.kiteui.launchManualCancel
import com.lightningkite.kiteui.views.*
import org.w3c.dom.HTMLSpanElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.get

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual typealias NDismissBackground = HTMLElement

@ViewDsl
actual inline fun ViewWriter.dismissBackgroundActual(crossinline setup: DismissBackground.() -> Unit): Unit {
    stack {
        native.classList.add("dismissBackground")
        native.onclick = { navigator.dismiss() }
        setup(DismissBackground(native))
        native.listNViews().forEach { it.onclick = { ev -> ev.stopImmediatePropagation() } }
    }
}

actual fun DismissBackground.onClick(action: suspend () -> Unit): Unit {
    native.onclick = { native.calculationContext.launchManualCancel(action) }
}