package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.Edges
import com.lightningkite.kiteui.views.extensionPadding
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
        val p = extensionPadding ?: Edges.ZERO
        return ((subviews.firstOrNull() as? UIView)?.sizeThatFits(size) ?: size).useContents {
            CGSizeMake(width + p.horizontalSum.value, height + p.verticalSum.value)
        }
    }

    override fun layoutSubviews() {
        super.layoutSubviews()
        val p = extensionPadding ?: Edges.ZERO
        bounds.useContents {
            (subviews.firstOrNull() as? UIView)?.setPsuedoframe(
                p.left.value,
                p.top.value,
                this@useContents.size.width - p.horizontalSum.value,
                this@useContents.size.height - p.verticalSum.value,
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