package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.WeakReference
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.models.Size
import com.lightningkite.kiteui.objc.UIViewWithSizeOverridesProtocol
import com.lightningkite.kiteui.reactive.LateInitProperty
import com.lightningkite.kiteui.reactive.Readable
import com.lightningkite.kiteui.reactive.onRemove
import com.lightningkite.kiteui.viewDebugTarget
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
    override fun applyPadding(dimension: Dimension?) {
        super.applyPadding(dimension)
        native.padding = dimension?.value ?: 0.0
    }

    override fun applyForeground(theme: Theme) {
        native.spacing = spacing?.value ?: theme.spacing.value
    }
    override fun spacingSet(value: Dimension?) {
        native.spacing = spacing?.value ?: theme.spacing.value
    }
}

@OptIn(ExperimentalNativeApi::class)
class NProgrammaticLayout: UIView(CGRectMake(0.0, 0.0, 0.0, 0.0)), UIViewWithSizeOverridesProtocol {
    var spacing: Double = 0.0
    var padding: Double = 0.0
    var delegate: ProgrammaticLayoutDelegate = ProgrammaticLayoutDelegate.AllFull
        set(value) {
            field = value
            setNeedsLayout()
        }
    var rview: WeakReference<ProgrammaticLayout>? = null
    private val inProgress = object: ProgrammingLayoutInProgress {
        override val spacing: Double get() = this@NProgrammaticLayout.spacing
        override val padding: Double get() = this@NProgrammaticLayout.padding
        override fun measure(child: RView, sizeConstraint: Size): Size {
            return child.native.sizeThatFits2(CGSizeMake(sizeConstraint.width, sizeConstraint.height), sizeConstraints = null).useContents { Size(width, height) }.also {
                if(child == viewDebugTarget)
                    println("Child ${child} measured within $sizeConstraint to be $it")
            }
        }

        override fun place(child: RView, left: Double, top: Double, right: Double, bottom: Double) {
            if(child == viewDebugTarget)
                println("Child ${child} placed at $left, $top, $right, $bottom")
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
        else {
            myInvalidated = true
            superview?.informParentOfSizeChangeDueToChild()
        }
    }

    var myInvalidated = false
    fun invalidateLayout() {
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
            // TODO: is this weird that the result is dropped?
            delegate.measure(rview?.get() ?: run {
                println("WARNING: ProgrammaticLayout.layoutSubviews won't work because RView is inaccessible")
                return
            }, inProgress, bounds.useContents { Size(size.width, size.height) })
            delegate.layout(rview?.get() ?: run {
                println("WARNING: ProgrammaticLayout.layoutSubviews won't work because RView is inaccessible")
                return
            }, inProgress, bounds.useContents { Size(size.width, size.height) })
            inLayout = false
        }
    }
}