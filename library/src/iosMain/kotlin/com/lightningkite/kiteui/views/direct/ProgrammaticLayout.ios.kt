package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.WeakReference
import com.lightningkite.kiteui.models.Rect
import com.lightningkite.kiteui.models.Size
import com.lightningkite.kiteui.objc.UIViewWithSizeOverridesProtocol
import com.lightningkite.kiteui.reactive.LateInitProperty
import com.lightningkite.kiteui.reactive.Readable
import com.lightningkite.kiteui.reactive.onRemove
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView
import com.lightningkite.kiteui.views.informParentOfSizeChangeDueToChild
import com.lightningkite.kiteui.views.layoutSubviewsAndLayers
import kotlinx.cinterop.CValue
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGRectZero
import platform.CoreGraphics.CGSize
import platform.CoreGraphics.CGSizeMake
import platform.UIKit.UIView
import kotlin.experimental.ExperimentalNativeApi
import kotlin.math.roundToInt

actual class ProgrammaticLayout actual constructor(context: RContext) : RView(context) {
    @OptIn(ExperimentalNativeApi::class)
    override val native: NProgrammaticLayout = NProgrammaticLayout().apply {
        rview = WeakReference(this@ProgrammaticLayout)
        onRemove { delegate = ProgrammaticLayoutDelegate.AllFull }
    }
    actual var delegate: ProgrammaticLayoutDelegate by native::delegate
    actual fun invalidateLayout() {
        native.invalidateLayout()
    }
}

@OptIn(ExperimentalNativeApi::class)
class NProgrammaticLayout: UIView(CGRectMake(0.0, 0.0, 0.0, 0.0)), UIViewWithSizeOverridesProtocol {
    var delegate: ProgrammaticLayoutDelegate = ProgrammaticLayoutDelegate.AllFull
        set(value) {
            field = value
            setNeedsLayout()
        }
    var rview: WeakReference<ProgrammaticLayout>? = null
    private val inProgress = object: ProgrammingLayoutInProgress {
        override fun measure(child: RView, sizeConstraint: Size): Size {
            return child.native.sizeThatFits2(CGSizeMake(sizeConstraint.width, sizeConstraint.height), sizeConstraints = null).useContents { Size(width, height) }
        }

        override fun place(child: RView, left: Double, top: Double, right: Double, bottom: Double) {
            child.native.setPsuedoframe(left, top, right - left, bottom - top)
            child.native.layoutSubviewsAndLayers()
        }

        override fun existingPosition(child: RView): Rect = child.native.frame.useContents { Rect.fromSize(left = origin.x, top = origin.y, width = size.width, height = size.height) }
    }

    override fun sizeThatFits(size: CValue<CGSize>): CValue<CGSize> {
        val before = inLayout
        inLayout = true
        val r = delegate.measure(rview?.get() ?: return CGSizeMake(0.0, 0.0), inProgress, size.useContents { Size(width, height) }).let { CGSizeMake(it.width, it.height) }
        inLayout = before
        return r
    }

    override fun subviewDidChangeSizing(view: UIView?) {
        if(inLayout) return
        else superview?.informParentOfSizeChangeDueToChild()
    }

    var myInvalidated = false
    fun invalidateLayout() {
//        Exception("Invalidating layout").printStackTrace()
        if(inLayout) return
        myInvalidated = true
        setNeedsLayout()
    }

    var inLayout = false
    override fun layoutSubviews() {
        if(bounds.useContents { size.width == 0.0 && size.height == 0.0 }) return
        if (inLayout) throw IllegalStateException()
        if(myInvalidated) {
            myInvalidated = false
//            Exception("layoutSubviews").printStackTrace()
            inLayout = true
            delegate.layout(rview?.get() ?: return, inProgress, bounds.useContents { Size(size.width, size.height) })
            inLayout = false
        }
    }
}