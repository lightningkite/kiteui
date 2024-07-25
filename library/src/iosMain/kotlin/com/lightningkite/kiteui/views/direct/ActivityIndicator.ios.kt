package com.lightningkite.kiteui.views.direct


import com.lightningkite.kiteui.models.SizeConstraints
import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.models.rem
import com.lightningkite.kiteui.views.*
import platform.UIKit.UIActivityIndicatorView

actual class ActivityIndicator actual constructor(context: RContext): RView(context) {
    override val native = UIActivityIndicatorView().apply {
        hidden = false
        startAnimating()
        extensionSizeConstraints = SizeConstraints(minWidth = 1.rem, minHeight = 1.rem)
        userInteractionEnabled = false
    }

    override fun applyForeground(theme: Theme) {
        native.color = theme.foreground.closestColor().toUiColor()
    }
}
