package com.lightningkite.kiteui.views.direct

import ViewWriter
import com.lightningkite.kiteui.models.SizeConstraints
import com.lightningkite.kiteui.models.px
import com.lightningkite.kiteui.views.*
import platform.UIKit.UIView

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual typealias NSeparator = UIView

@ViewDsl
actual inline fun ViewWriter.separatorActual(crossinline setup: Separator.() -> Unit): Unit = element(UIView()) {
    handleTheme(
        this,
        foreground = {
            backgroundColor = it.foreground.closestColor().toUiColor()
            alpha = 0.25
        },
    ) {
        extensionSizeConstraints = SizeConstraints(minWidth = 1.px, minHeight = 1.px)
        setup(Separator(this))
    }
}