package com.lightningkite.kiteui.views.direct


import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.models.card
import com.lightningkite.kiteui.views.*
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreGraphics.CGRectMake
import platform.UIKit.UIProgressView

@OptIn(ExperimentalForeignApi::class)
actual class ProgressBar actual constructor(context: RContext) : RView(context) {
    override val native = UIProgressView(CGRectMake(0.0, 0.0, 0.0, 0.0))
    override fun applyForeground(theme: Theme) {
        super.applyForeground(theme)
        native.trackTintColor = theme.background.closestColor().toUiColor()
        native.progressTintColor = theme.card().foreground.closestColor().toUiColor()
    }
    actual var ratio: Float
        get() = native.progress
        set(value) { native.progress = value }
}
