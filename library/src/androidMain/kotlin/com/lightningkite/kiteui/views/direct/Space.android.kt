package com.lightningkite.kiteui.views.direct

import android.view.View
import com.lightningkite.kiteui.views.ViewDsl
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.lparams

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual typealias NSpace = View

@ViewDsl
actual inline fun ViewWriter.spaceActual(crossinline setup: Space.() -> Unit) {
    return viewElement(factory = ::NSpace, wrapper = ::Space) {
        handleTheme(native, foreground = { it, native ->
            native.lparams.width = it.spacing.value.toInt()
            native.lparams.height = it.spacing.value.toInt()
        }) {
            setup(this)
        }
    }
}

actual fun ViewWriter.space(
    multiplier: Double,
    setup: Space.() -> Unit,
) {
    return viewElement(factory = ::View, wrapper = ::Space) {
        handleTheme(native, foreground = { it, native ->
            native.lparams.width = it.spacing.value.times(multiplier).toInt()
            native.lparams.height = it.spacing.value.times(multiplier).toInt()
        }) {
            setup(this)
        }
    }
}