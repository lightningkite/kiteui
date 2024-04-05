package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.SizeConstraints
import com.lightningkite.kiteui.models.rem
import com.lightningkite.kiteui.views.*
import platform.UIKit.UIProgressView
import kotlin.math.roundToInt

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual typealias NProgressBar = UIProgressView

@ViewDsl
actual inline fun ViewWriter.progressBarActual(crossinline setup: ProgressBar.() -> Unit): Unit =
    element(UIProgressView()) {
        hidden = false
        handleTheme(
            this,
            foreground = {
                this.trackTintColor = it.foreground.closestColor().toUiColor()
                this.progressTintColor = it.card().background.closestColor().toUiColor()
            },
        ) {
            setup(ProgressBar(this))
        }
    }

actual var ProgressBar.ratio: Float
    get() = native.progress
    set(value) { native.progress = value }