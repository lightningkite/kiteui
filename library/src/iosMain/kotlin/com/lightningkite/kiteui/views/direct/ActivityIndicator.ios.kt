package com.lightningkite.kiteui.views.direct

import ViewWriter
import com.lightningkite.kiteui.models.SizeConstraints
import com.lightningkite.kiteui.models.rem
import com.lightningkite.kiteui.views.*
import platform.UIKit.UIActivityIndicatorView

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual typealias NActivityIndicator = UIActivityIndicatorView

@ViewDsl
actual inline fun ViewWriter.activityIndicatorActual(crossinline setup: ActivityIndicator.() -> Unit): Unit =
    element(UIActivityIndicatorView()) {
        hidden = false
        startAnimating()
        handleTheme(
            this,
            foreground = {
                this.color = it.foreground.closestColor().toUiColor()
            },
        ) {
            extensionSizeConstraints = SizeConstraints(minWidth = 1.rem, minHeight = 1.rem)
            setup(ActivityIndicator(this))
        }
    }