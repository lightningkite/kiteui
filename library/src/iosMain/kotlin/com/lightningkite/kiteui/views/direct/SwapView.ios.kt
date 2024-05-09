package com.lightningkite.kiteui.views.direct

import ViewWriter
import com.lightningkite.kiteui.afterTimeout
import com.lightningkite.kiteui.models.Dimension
import com.lightningkite.kiteui.models.ScreenTransition
import com.lightningkite.kiteui.objc.UIViewWithSizeOverridesProtocol
import com.lightningkite.kiteui.objc.UIViewWithSpacingRulesProtocol
import com.lightningkite.kiteui.reactive.Property
import com.lightningkite.kiteui.views.*
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readValue
import platform.CoreGraphics.*
import platform.UIKit.UIEvent
import platform.UIKit.UIView

@OptIn(ExperimentalForeignApi::class)
actual class NSwapView: UIView(CGRectZero.readValue()), UIViewWithSizeOverridesProtocol,
    UIViewWithSpacingRulesProtocol {
    var padding: Double
        get() = extensionPadding ?: 0.0
        set(value) { extensionPadding = value }
    val spacingOverride: Property<Dimension?> = Property<Dimension?>(null)
    override fun getSpacingOverrideProperty() = spacingOverride

    init {
        clipsToBounds = true
    }

    private val childSizeCache: ArrayList<HashMap<Size, Size>> = ArrayList()
    override fun sizeThatFits(size: CValue<CGSize>): CValue<CGSize> = frameLayoutSizeThatFits(size, childSizeCache)
    override fun layoutSubviews() = frameLayoutLayoutSubviews(childSizeCache)
    override fun subviewDidChangeSizing(view: UIView?) = frameLayoutSubviewDidChangeSizing(view, childSizeCache)
    override fun didAddSubview(subview: UIView) {
        super.didAddSubview(subview)
        frameLayoutDidAddSubview(subview, childSizeCache)
    }
    override fun willRemoveSubview(subview: UIView) {
        // Fixes a really cursed crash where "this" is null
        if(this != null) frameLayoutWillRemoveSubview(subview, childSizeCache)
        super.willRemoveSubview(subview)
    }

    override fun hitTest(point: CValue<CGPoint>, withEvent: UIEvent?): UIView? {
        return frameLayoutHitTest(point, withEvent).takeUnless { it == this }
    }

    internal var swapViewKeepLast = true
}


@ViewDsl
actual inline fun ViewWriter.swapViewActual(crossinline setup: SwapView.() -> Unit) = element(NSwapView()) {
    extensionViewWriter = this@swapViewActual.newViews()
    handleTheme(this, viewDraws = false) {
        setup(SwapView(this))
    }
}

@ViewDsl
actual inline fun ViewWriter.swapViewDialogActual(crossinline setup: SwapView.() -> Unit): Unit =
    element(NSwapView()) {
        extensionViewWriter = this@swapViewDialogActual.newViews()
        handleTheme(this, viewDraws = false) {
            hidden = true
            setup(SwapView(this))
        }
    }

//actual fun SwapView.swap(transition: ScreenTransition, createNewView: ViewWriter.() -> Unit): Unit {
//    native.extensionViewWriter!!.rootCreated = null
//    native.withoutAnimation {
//        native.clearNViews()
//        createNewView(native.extensionViewWriter!!)
//        native.extensionViewWriter!!.rootCreated?.let {
//            native.addNView(it)
//        }
//        native.hidden = native.subviews.isEmpty()
//        native.informParentOfSizeChange()
//    }
//}

@OptIn(ExperimentalForeignApi::class)
actual fun SwapView.swap(transition: ScreenTransition, createNewView: ViewWriter.() -> Unit): Unit {
    native.hidden = false
    val oldView = native.subviews.lastOrNull() as? UIView
    oldView?.let { oldView ->
        native.animateIfAllowed {
            transition.exit(oldView)
        }
    }
    afterTimeout((native.extensionAnimationDuration ?: 0.15).times(1000).toLong()) {
        oldView?.let { native.removeNView(it) }
        native.hidden = native.subviews.isEmpty()
        native.informParentOfSizeChange()
    }

    native.extensionViewWriter!!.rootCreated = null
    native.withoutAnimation {
        native.swapViewKeepLast = false
        createNewView(native.extensionViewWriter!!)
        native.extensionViewWriter!!.rootCreated?.let {
            native.addNView(it)
            transition.enter(it)
            native.swapViewKeepLast = true
        }
    }
    val created = native.extensionViewWriter!!.rootCreated
    created?.let { created ->
        native.animateIfAllowed {
            created.transform = CGAffineTransformMake(1.0, 0.0, 0.0, 1.0, 0.0, 0.0)
            created.opacity = 1.0
        }
    }
}
