package com.lightningkite.kiteui.views.direct


import com.lightningkite.kiteui.InternalKiteUi
import com.lightningkite.kiteui.models.SizeConstraints
import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.models.ThemeAndBack
import com.lightningkite.kiteui.models.dp
import com.lightningkite.kiteui.models.px
import com.lightningkite.kiteui.views.*
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreGraphics.CGRectMake
import platform.UIKit.UIView


@InternalKiteUi
public actual class Separator public actual constructor(context: RContext): RView(context) {
    override val native: UIView = UIView(CGRectMake(0.0, 0.0, 0.0, 0.0))
    init {
        sizeConstraints = SizeConstraints(minWidth = 1.px, minHeight = 1.px)
    }

    override fun applyTheme(theme: ThemeAndBack) {
        super.applyTheme(theme)
        native.backgroundColor = theme.theme.separator.closestColor().toUiColor()
    }
}
