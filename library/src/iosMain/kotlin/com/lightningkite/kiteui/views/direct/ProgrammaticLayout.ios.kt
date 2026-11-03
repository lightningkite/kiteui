package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.Log
import com.lightningkite.kiteui.WeakReference
import com.lightningkite.kiteui.debugPrint
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.models.Size
import com.lightningkite.kiteui.objc.UIViewWithSizeOverridesProtocol
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.Element
import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.NativeContainerElement
import com.lightningkite.kiteui.views.informParentOfSizeChangeDueToChild
import com.lightningkite.kiteui.views.layoutSubviewsAndLayers
import com.lightningkite.kiteui.views.theme
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

public actual class ProgrammaticLayout actual constructor(context: ElementContext) : NativeContainerElement(context), LinearLayoutElement {
    @OptIn(ExperimentalNativeApi::class)
    override val native: NProgrammaticLayout = NProgrammaticLayout().apply {
        element = WeakReference(this@ProgrammaticLayout)
        onRemove { delegate = ProgrammaticLayoutDelegate.AllFull }
    }
    public actual var delegate: ProgrammaticLayoutDelegate by native::delegate
    public actual fun invalidateLayout() {
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

    actual override var gap: Dimension? = null
        set(value) {
            field = value
            native.spacingCurrentPx = gap?.viewUnits ?: theme.gap.viewUnits
        }

    override fun nativeApplyTheme(theme: ThemeAndBack) {
        super.nativeApplyTheme(theme)
        native.spacingCurrentPx = gap?.viewUnits ?: theme.theme.gap.viewUnits
    }
}

@OptIn(ExperimentalNativeApi::class)
public class NProgrammaticLayout: UIView(CGRectMake(0.0, 0.0, 0.0, 0.0)), UIViewWithSizeOverridesProtocol {
    internal var spacingCurrentPx: Double = 0.0
    internal var paddingTopCurrentPx: Double = 0.0
    internal var paddingLeftCurrentPx: Double = 0.0
    internal var paddingRightCurrentPx: Double = 0.0
    internal var paddingBottomCurrentPx: Double = 0.0
    internal var delegate: ProgrammaticLayoutDelegate = ProgrammaticLayoutDelegate.AllFull
        set(value) {
            field = value
            setNeedsLayout()
        }
    private var currentSize: Size = Size.Zero
    internal var element: WeakReference<ProgrammaticLayout>? = null
    private val inProgress = object: ProgrammingLayoutInProgress {
        override val within: Size
            get() = currentSize
        override val gap: Double get() = spacingCurrentPx
        override val padding: Double get() = paddingLeftCurrentPx
        override val paddingTop: Double get() = paddingTopCurrentPx
        override val paddingLeft: Double get() = paddingLeftCurrentPx
        override val paddingRight: Double get() = paddingRightCurrentPx
        override val paddingBottom: Double get() = paddingBottomCurrentPx
        override fun measure(child: Element, sizeConstraint: Size): Size {
            return child.underlyingNativeElement.native.sizeThatFits2(CGSizeMake(sizeConstraint.width, sizeConstraint.height), sizeConstraints = null).useContents { Size(width, height) }.also {
                child.debugPrint { "Child ${child} measured within $sizeConstraint to be $it" }
            }
        }

        override fun place(child: Element, left: Double, top: Double, right: Double, bottom: Double) {
            child.debugPrint { "Child ${child} placed at $left, $top, $right, $bottom" }
            child.underlyingNativeElement.native.setPsuedoframe(left, top, right - left, bottom - top)
            child.underlyingNativeElement.native.layoutSubviewsAndLayers()
        }

        override fun existingPosition(child: Element): Rect = child.underlyingNativeElement.native.frame.useContents { Rect.fromSize(left = origin.x, top = origin.y, width = size.width, height = size.height) }
    }

    override fun sizeThatFits(size: CValue<CGSize>): CValue<CGSize> {
        val before = inLayout
        inLayout = true
        val r = delegate.measure(element?.get() ?: return CGSizeMake(0.0, 0.0), inProgress, size.useContents { Size(width, height) }).let { CGSizeMake(it.width, it.height) }
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

    internal var myInvalidated: Boolean = false
    internal fun invalidateLayout() {
        if(inLayout) return
        myInvalidated = true
        informParentOfSizeChangeDueToChild()
        setNeedsLayout()
    }

    internal var inLayout: Boolean = false
    private var lastLaidOutSize: Size? = null
    override fun layoutSubviews() {
        val mySize = bounds.useContents { Size(size.width, size.height) }
        if(mySize.width == 0.0 && mySize.height == 0.0) return
        if (inLayout) throw IllegalStateException()
        // Relayout not just when explicitly invalidated, but also when our own bounds changed
        // size (e.g. rotation/parent resize) since neither forceRemeasures() nor
        // subviewDidChangeSizing() fire for that case.
        if(myInvalidated || lastLaidOutSize != mySize) {
            myInvalidated = false
            lastLaidOutSize = mySize
//            Exception("layoutSubviews").printStackTrace()
            inLayout = true
            // TODO: is this weird that the result is dropped?
            delegate.measure(element?.get() ?: run {
                Log.warn("ProgrammaticLayout.layoutSubviews won't work because Element is inaccessible")
                return
            }, inProgress, mySize)
            delegate.layout(element?.get() ?: run {
                Log.warn("ProgrammaticLayout.layoutSubviews won't work because Element is inaccessible")
                return
            }, inProgress, mySize)
            inLayout = false
        }
    }

    override fun hitTest(point: CValue<CGPoint>, withEvent: UIEvent?): UIView? {
        return frameLayoutHitTest(point, withEvent)
    }
}