package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.Log
import com.lightningkite.kiteui.WeakReference
import com.lightningkite.kiteui.debugPrint
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.models.Size
import com.lightningkite.kiteui.objc.UIViewWithSizeOverridesProtocol
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.viewDebugTarget
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView
import com.lightningkite.kiteui.views.informParentOfSizeChangeDueToChild
import com.lightningkite.kiteui.views.layoutSubviewsAndLayers
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*
import kotlin.experimental.ExperimentalNativeApi
import kotlinx.cinterop.CValue
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGPoint
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSize
import platform.CoreGraphics.CGSizeMake
import platform.UIKit.UIEvent
import platform.UIKit.UIView

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

    override fun refreshPadding() {
        super.refreshPadding()
        val value = appliedPadding
        native.paddingTopCurrentPx = value.top.viewUnits
        native.paddingLeftCurrentPx = value.left.viewUnits
        native.paddingRightCurrentPx = value.right.viewUnits
        native.paddingBottomCurrentPx = value.bottom.viewUnits
        native.setNeedsLayout()
    }

    override var gap: Dimension?
        get() = super.gap
        set(value) {
            super.gap = value
            native.spacingCurrentPx = gap?.viewUnits ?: theme.gap.viewUnits
        }

    override fun internalAddChild(index: Int, view: RView) {
        super.internalAddChild(index, view)
    }

    override fun applyTheme(theme: ThemeAndBack) { super.applyTheme(theme); val theme = theme.theme
        native.spacingCurrentPx = gap?.viewUnits ?: theme.gap.viewUnits
    }
}

@OptIn(ExperimentalNativeApi::class)
class NProgrammaticLayout: UIView(CGRectMake(0.0, 0.0, 0.0, 0.0)), UIViewWithSizeOverridesProtocol {
    var spacingCurrentPx: Double = 0.0
    var paddingTopCurrentPx: Double = 0.0
    var paddingLeftCurrentPx: Double = 0.0
    var paddingRightCurrentPx: Double = 0.0
    var paddingBottomCurrentPx: Double = 0.0
    var delegate: ProgrammaticLayoutDelegate = ProgrammaticLayoutDelegate.AllFull
        set(value) {
            field = value
            setNeedsLayout()
        }
    private var currentSize: Size = Size.Zero
    var rview: WeakReference<ProgrammaticLayout>? = null
    private val inProgress = object: ProgrammingLayoutInProgress {
        override val within: Size
            get() = currentSize
        override val gap: Double get() = spacingCurrentPx
        override val padding: Double get() = paddingLeftCurrentPx
        override val paddingTop: Double get() = paddingTopCurrentPx
        override val paddingLeft: Double get() = paddingLeftCurrentPx
        override val paddingRight: Double get() = paddingRightCurrentPx
        override val paddingBottom: Double get() = paddingBottomCurrentPx
        override fun measure(child: RView, sizeConstraint: Size): Size {
            return child.native.sizeThatFits2(CGSizeMake(sizeConstraint.width, sizeConstraint.height), sizeConstraints = null).useContents { Size(width, height) }.also {
                child.debugPrint { "Child ${child} measured within $sizeConstraint to be $it" }
            }
        }

        override fun place(child: RView, left: Double, top: Double, right: Double, bottom: Double) {
            child.debugPrint { "Child ${child} placed at $left, $top, $right, $bottom" }
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

    override fun forceRemeasures() {
        myInvalidated = true
        superview?.informParentOfSizeChangeDueToChild()
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
                Log.warn("ProgrammaticLayout.layoutSubviews won't work because RView is inaccessible")
                return
            }, inProgress, bounds.useContents { Size(size.width, size.height) })
            delegate.layout(rview?.get() ?: run {
                Log.warn("ProgrammaticLayout.layoutSubviews won't work because RView is inaccessible")
                return
            }, inProgress, bounds.useContents { Size(size.width, size.height) })
            inLayout = false
        }
    }

    override fun hitTest(point: CValue<CGPoint>, withEvent: UIEvent?): UIView? {
        return frameLayoutHitTest(point, withEvent)
    }
}