package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.Edges
import com.lightningkite.kiteui.views.extensionPadding
import com.lightningkite.kiteui.views.extensionSafeInsetPadding
import kotlinx.cinterop.*
import platform.CoreGraphics.*
import platform.UIKit.UICoordinateSpaceProtocol
import platform.UIKit.UIEvent
import platform.UIKit.UIView



class WrapperView : UIView(CGRectZero.readValue()) {

    init {
        userInteractionEnabled = false
    }

    override fun sizeThatFits(size: CValue<CGSize>): CValue<CGSize> {
        val padding = extensionPadding?.plus(extensionSafeInsetPadding) ?: Edges.ZERO
        return ((subviews.firstOrNull() as? UIView)?.sizeThatFits(size) ?: size).useContents {
            CGSizeMake(width + padding.horizontalSum.value, height + padding.verticalSum.value)
        }
    }

    override fun layoutSubviews() {
        super.layoutSubviews()
        val padding = extensionPadding?.plus(extensionSafeInsetPadding) ?: Edges.ZERO
        bounds.useContents {
            (subviews.firstOrNull() as? UIView)?.setPsuedoframe(
                padding.left.value,
                padding.top.value,
                this@useContents.size.width - padding.horizontalSum.value,
                this@useContents.size.height - padding.verticalSum.value,
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