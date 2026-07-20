package com.lightningkite.kiteui.views.direct


import com.lightningkite.kiteui.views.*
import platform.CoreGraphics.CGRectMake
import platform.UIKit.UIView


public actual class CircularProgress actual constructor(context: ElementContext) : NativeElement(context) {
    override val native: UIView = UIView(CGRectMake(0.0, 0.0, 0.0, 0.0))
    public actual var ratio: Float = 0f
}
