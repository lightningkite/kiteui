package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.times
import com.lightningkite.kiteui.views.ViewDsl
import ViewWriter
import com.lightningkite.kiteui.views.element
import com.lightningkite.kiteui.views.reactiveScope
import org.w3c.dom.HTMLElement

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual typealias NSpace = HTMLElement

@ViewDsl
actual inline fun ViewWriter.spaceActual(crossinline setup: Space.() -> Unit): Unit = element<NSpace>("span") {
    val getter = currentTheme
    val s = Space(this)
    s.reactiveScope {
        style.width = (getter().spacing).value
        style.height = (getter().spacing).value
    }
    setup(s)
}

actual fun ViewWriter.space(multiplier: Double, setup: Space.() -> Unit): Unit = element<NSpace>("span") {
    val getter = currentTheme
    val s = Space(this)
    s.reactiveScope {
        style.width = (getter().spacing * multiplier).value
        style.height = (getter().spacing * multiplier).value
    }
    setup(s)
}