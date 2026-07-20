package com.lightningkite.kiteui.views.direct


import com.lightningkite.kiteui.models.SizeConstraints
import com.lightningkite.kiteui.models.ThemeAndBack
import com.lightningkite.kiteui.models.px
import com.lightningkite.kiteui.views.*
import platform.CoreGraphics.CGRectMake
import platform.UIKit.UIView


public actual class Separator actual constructor(context: ElementContext): NativeElement(context) {
    override val native = UIView(CGRectMake(0.0, 0.0, 0.0, 0.0))

    init {
        sizeConstraints = SizeConstraints(minWidth = 1.px, minHeight = 1.px)
    }

    override fun nativeApplyTheme(theme: ThemeAndBack) {
        super.nativeApplyTheme(theme)
        native.backgroundColor = theme.theme.separator.closestColor().toUiColor()
    }
}
