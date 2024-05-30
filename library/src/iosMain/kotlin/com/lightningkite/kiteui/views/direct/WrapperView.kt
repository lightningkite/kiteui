package com.lightningkite.kiteui.views.direct

import kotlinx.cinterop.*
import platform.CoreGraphics.CGRect
import platform.CoreGraphics.CGRectZero
import platform.CoreGraphics.CGSize
import platform.UIKit.UIView


@OptIn(ExperimentalForeignApi::class)
class WrapperView : UIView(CGRectZero.readValue()) {

    init {
        userInteractionEnabled = false
    }

    override fun sizeThatFits(size: CValue<CGSize>): CValue<CGSize> =
        (subviews.firstOrNull() as? UIView)?.sizeThatFits(size) ?: size

    override fun layoutSubviews() {
        super.layoutSubviews()
        bounds.useContents {
            (subviews.firstOrNull() as? UIView)?.setPsuedoframe(
                0.0,
                0.0,
                this@useContents.size.width,
                this@useContents.size.height,
            )
        }
    }
}