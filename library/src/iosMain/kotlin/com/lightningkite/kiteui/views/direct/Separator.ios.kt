package com.lightningkite.kiteui.views.direct


import com.lightningkite.kiteui.models.SizeConstraints
import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.models.px
import com.lightningkite.kiteui.views.*
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreGraphics.CGRectMake
import platform.UIKit.UIView

@OptIn(ExperimentalForeignApi::class)
actual class Separator actual constructor(context: RContext): RView(context) {
    override val native = UIView(CGRectMake(0.0, 0.0, 0.0, 0.0))
    init {
        sizeConstraints = SizeConstraints(minWidth = 1.px, minHeight = 1.px)
    }

    override fun applyForeground(theme: Theme) {
        super.applyForeground(theme)
        native.backgroundColor = theme.foreground.closestColor().applyAlpha(SEPARATOR_ALPHA).toUiColor()
    }
}
