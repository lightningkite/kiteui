package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.Rect
import com.lightningkite.kiteui.models.Size
import com.lightningkite.kiteui.reactive.LateInitProperty
import com.lightningkite.kiteui.reactive.Readable
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView
import kotlinx.cinterop.CValue
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGRectZero
import platform.CoreGraphics.CGSize
import platform.CoreGraphics.CGSizeMake
import platform.UIKit.UIView
import kotlin.math.roundToInt

actual class ProgrammaticLayout actual constructor(context: RContext) : RView(context) {
    override val native: NProgrammaticLayout = NProgrammaticLayout()

    actual fun measureChild(
        child: RView,
        sizeConstraint: Size
    ): Size = child.native.sizeThatFits2(CGSizeMake(sizeConstraint.width, sizeConstraint.height), sizeConstraints = null).useContents {
        Size(width, height)
    }

    actual fun setChildBounds(child: RView, rect: Rect) {
        child.native.setPsuedoframe(rect.left, rect.top, rect.width, rect.height)
        child.native.setNeedsLayout()
    }

    actual val externalSizeLimit: Readable<Size> = native.externalSizeLimit
}

class NProgrammaticLayout: UIView(CGRectMake(0.0, 0.0, 0.0, 0.0)) {
    val externalSizeLimit = LateInitProperty<Size>()
    private var lastWidth: Int = 0
    private var lastHeight: Int = 0
    override fun sizeThatFits(size: CValue<CGSize>): CValue<CGSize> {
        val newWidth = size.useContents { width }.roundToInt()
        val newHeight = size.useContents { height }.roundToInt()
//        if(lastWidth == 0.0 || lastHeight == 0.0) {
        if(lastWidth != newWidth || lastHeight != newHeight) {
            lastWidth = newWidth
            lastHeight = newHeight
            externalSizeLimit.value = Size(newWidth.toDouble(), newHeight.toDouble())
        }
        var w = 0.0
        var h = 0.0
        subviews.forEach {
            it as UIView
            w = maxOf(w, it.frame.useContents { this.size.width })
            h = maxOf(h, it.frame.useContents { this.size.height })
        }
        return CGSizeMake(w, h)
    }
//    override fun layoutSubviews() {
//        println("NProgrammaticLayout.layoutSubviews")
//        subviews.forEach {
//            it as UIView
//            println("NProgrammaticLayout.layoutSubviews laying out subview ${it}")
//            it.setFrame(CGRectMake())
//        }
//    }
}