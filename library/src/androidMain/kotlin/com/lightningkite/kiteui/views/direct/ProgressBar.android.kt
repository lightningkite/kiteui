package com.lightningkite.kiteui.views.direct

import android.content.res.ColorStateList
import android.widget.ProgressBar as AProgressBar
import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.views.ViewDsl
import com.lightningkite.kiteui.views.ViewWriter
import kotlin.math.roundToInt

actual typealias NProgressBar = AProgressBar

@ViewDsl
actual inline fun ViewWriter.progressBarActual(crossinline setup: ProgressBar.() -> Unit) {
    return viewElement(factory = { AProgressBar(it, null, android.R.attr.progressBarStyleHorizontal) }, wrapper = ::ProgressBar) {
        handleTheme(native, foreground = {
            theme: Theme, progressBar: AProgressBar ->
            progressBar.progressTintList = ColorStateList.valueOf(theme.foreground.colorInt())
            progressBar.progressBackgroundTintList = ColorStateList.valueOf(theme.background.colorInt())
            progressBar.setPaddingAll(0)
        }) {
            native.min = 0
            native.max = 10000
            setup(this)
        }
    }
}

actual var ProgressBar.ratio: Float
    get() = native.progress / 10000f
    set(value) { native.progress = (value * 10000).roundToInt() }