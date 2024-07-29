package com.lightningkite.kiteui.views.direct


import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.models.card
import com.lightningkite.kiteui.views.*
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreGraphics.CGRectMake
import platform.UIKit.UIProgressView
import platform.UIKit.UIView

@OptIn(ExperimentalForeignApi::class)
actual class ProgressBar actual constructor(context: RContext) : RView(context) {
    override val native = UIProgressView(CGRectMake(0.0, 0.0, 0.0, 0.0))
    override fun applyForeground(theme: Theme) {
        super.applyForeground(theme)
        native.trackTintColor = theme.foreground.closestColor().toUiColor()
        native.progressTintColor = theme.card().background.closestColor().toUiColor()
    }
    actual var ratio: Float
        get() = native.progress
        set(value) { native.progress = value }
}
