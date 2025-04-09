package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.WeakReference
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.models.Size
import com.lightningkite.kiteui.objc.UIViewWithSizeOverridesProtocol
import com.lightningkite.readable.onRemove
import com.lightningkite.kiteui.viewDebugTarget
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView
import com.lightningkite.kiteui.views.informParentOfSizeChangeDueToChild
import com.lightningkite.kiteui.views.layoutSubviewsAndLayers
import kotlinx.cinterop.CValue
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSize
import platform.CoreGraphics.CGSizeMake
import platform.UIKit.UIView
import kotlin.experimental.ExperimentalNativeApi

actual class ProgrammaticLayout actual constructor(context: RContext) : RView(context) {
    override val cannotBeCovered: Boolean get() = false
    @OptIn(ExperimentalNativeApi::class)
    override val native: NProgrammaticLayout = NProgrammaticLayout().apply {
        rview = WeakReference(this@ProgrammaticLayout)
        onRemove { delegate = ProgrammaticLayoutDelegate.AllFull }
    }
    actual var delegate: ProgrammaticLayoutDelegate by native::delegate
    actual fun invalidateLayout() {
        native.invalidateLayout()
    }
    override var paddingByEdge: Edges?
        get() = super.paddingByEdge
        set(value) {
            super.paddingByEdge = value
            native.paddingTopCurrentPx = (value ?: theme.padding.takeIf { themeAndBack.padding })?.top?.canvasUnits ?: 0.0
            native.paddingLeftCurrentPx = (value ?: theme.padding.takeIf { themeAndBack.padding })?.left?.canvasUnits ?: 0.0
            native.paddingRightCurrentPx = (value ?: theme.padding.takeIf { themeAndBack.padding })?.right?.canvasUnits ?: 0.0
            native.paddingBottomCurrentPx = (value ?: theme.padding.takeIf { themeAndBack.padding })?.bottom?.canvasUnits ?: 0.0
        }
    override var gap: Dimension?
        get() = super.gap
        set(value) {
            super.gap = value
            native.spacingCurrentPx = spacing?.canvasUnits ?: theme.gap.canvasUnits
        }

    override fun applyTheme(theme: ThemeAndBack) { super.applyTheme(theme); val theme = theme.theme
        native.spacingCurrentPx = spacing?.canvasUnits ?: theme.gap.canvasUnits
        native.paddingTopCurrentPx = (paddingByEdge ?: theme.padding.takeIf { themeAndBack.padding })?.top?.canvasUnits ?: 0.0
        native.paddingLeftCurrentPx = (paddingByEdge ?: theme.padding.takeIf { themeAndBack.padding })?.left?.canvasUnits ?: 0.0
        native.paddingRightCurrentPx = (paddingByEdge ?: theme.padding.takeIf { themeAndBack.padding })?.right?.canvasUnits ?: 0.0
        native.paddingBottomCurrentPx = (paddingByEdge ?: theme.padding.takeIf { themeAndBack.padding })?.bottom?.canvasUnits ?: 0.0
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
    var rview: WeakReference<ProgrammaticLayout>? = null
    private val inProgress = object: ProgrammingLayoutInProgress {
        override val spacing: Double get() = spacingCurrentPx
        override val padding: Double get() = paddingLeftCurrentPx
        override val paddingTop: Double get() = paddingTopCurrentPx
        override val paddingLeft: Double get() = paddingLeftCurrentPx
        override val paddingRight: Double get() = paddingRightCurrentPx
        override val paddingBottom: Double get() = paddingBottomCurrentPx
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