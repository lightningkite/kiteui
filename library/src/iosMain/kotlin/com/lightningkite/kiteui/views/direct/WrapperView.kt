package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.views.extensionPadding
import kotlinx.cinterop.*
import platform.CoreGraphics.CGRectZero
import platform.CoreGraphics.CGSize
import platform.UIKit.UIView


@OptIn(ExperimentalForeignApi::class)
class WrapperView : UIView(CGRectZero.readValue()) {

    override fun sizeThatFits(size: CValue<CGSize>): CValue<CGSize> =
        (subviews.firstOrNull() as? UIView)?.sizeThatFits(size) ?: size

    override fun layoutSubviews() {
        super.layoutSubviews()
        val padding = extensionPadding ?: 0.0
        bounds.useContents {
            (subviews.firstOrNull() as? UIView)?.setPsuedoframe(
                padding,
                0.0,
                this@useContents.size.width - 2 * padding,
                this@useContents.size.height,
            )
        }
    }
}