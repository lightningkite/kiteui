package com.lightningkite.kiteui.views.direct


import com.lightningkite.kiteui.models.SizeConstraints
import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.models.dp
import com.lightningkite.kiteui.views.*
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreGraphics.CGRectMake
import platform.UIKit.UIView


actual class Separator actual constructor(context: RContext): RView(context) {
    override val native = UIView(CGRectMake(0.0, 0.0, 0.0, 0.0))
    init {
        sizeConstraints = SizeConstraints(minWidth = 1.dp, minHeight = 1.dp)
    }

    override fun applyForeground(theme: Theme) {
        super.applyForeground(theme)
        native.backgroundColor = theme.foreground.closestColor().toUiColor()
    }
}
