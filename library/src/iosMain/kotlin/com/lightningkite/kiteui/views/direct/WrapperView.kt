package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.views.extensionPadding
import kotlinx.cinterop.*
import platform.CoreGraphics.*
import platform.UIKit.UICoordinateSpaceProtocol
import platform.UIKit.UIEvent
import platform.UIKit.UIView


@OptIn(ExperimentalForeignApi::class)
class WrapperView : UIView(CGRectZero.readValue()) {

    init {
        userInteractionEnabled = false
    }

    override fun sizeThatFits(size: CValue<CGSize>): CValue<CGSize> {
        val p = extensionPadding ?: 0.0
        return ((subviews.firstOrNull() as? UIView)?.sizeThatFits(size) ?: size).useContents {
            CGSizeMake(width + p * 2, height + p * 2)
        }
    }

    override fun layoutSubviews() {
        super.layoutSubviews()
        val p = extensionPadding ?: 0.0
        bounds.useContents {
            (subviews.firstOrNull() as? UIView)?.setPsuedoframe(
                p,
                p,
                this@useContents.size.width - p * 2,
                this@useContents.size.height - p * 2,
            )
        }
    }
    override fun hitTest(point: CValue<CGPoint>, withEvent: UIEvent?): UIView? {
        if (hidden) return null
        if (bounds.useContents {
                val rect = this
                point.useContents {
                    val point = this
                    point.x >= rect.origin.x &&
                            point.y >= rect.origin.y &&
                            point.x <= rect.origin.x + rect.size.width &&
                            point.y <= rect.origin.y + rect.size.height
                }
            }) {
            return (subviews.firstOrNull() as? UIView)?.let {
                if (it.hidden) return@let null
                it.hitTest(
                    it.convertPoint(point = point, fromCoordinateSpace = this as UICoordinateSpaceProtocol),
                    withEvent
                )
            }
        }
        return null
    }
}