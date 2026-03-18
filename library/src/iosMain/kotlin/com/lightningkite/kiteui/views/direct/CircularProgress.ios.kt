package com.lightningkite.kiteui.views.direct


import com.lightningkite.kiteui.views.*
import platform.CoreGraphics.CGRectMake
import platform.UIKit.UIView


actual class CircularProgress actual constructor(context: ElementContext) : RView(context) {
    override val native = UIView(CGRectMake(0.0, 0.0, 0.0, 0.0))
    actual var ratio: Float = 0f
}
