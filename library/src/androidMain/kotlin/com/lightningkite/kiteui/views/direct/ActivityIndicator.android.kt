package com.lightningkite.kiteui.views.direct

import android.content.res.ColorStateList
import android.widget.ProgressBar
import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.views.ViewDsl
import com.lightningkite.kiteui.views.ViewWriter

actual typealias NActivityIndicator = ProgressBar

@ViewDsl
actual inline fun ViewWriter.activityIndicatorActual(crossinline setup: ActivityIndicator.() -> Unit) {
    return viewElement(factory = { ProgressBar(it, null, android.R.attr.progressBarStyleSmall) }, wrapper = ::ActivityIndicator) {
        handleTheme(native, foreground = {
            theme: Theme, progressBar: ProgressBar ->
            progressBar.indeterminateTintList = ColorStateList.valueOf(theme.foreground.colorInt())
        }) {
            setup(this)
        }
    }
}