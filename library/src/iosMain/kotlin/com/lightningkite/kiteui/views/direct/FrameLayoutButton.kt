

package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.Dimension
import com.lightningkite.kiteui.objc.UIViewWithSizeOverridesProtocol
import com.lightningkite.kiteui.objc.UIViewWithSpacingRulesProtocol
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*
import kotlinx.cinterop.*
import platform.CoreGraphics.*
import platform.UIKit.*
import platform.darwin.sel_registerName

//private val UIViewLayoutParams = ExtensionProperty<UIView, LayoutParams>()
//val UIView.layoutParams: LayoutParams by UIViewLayoutParams
//
//class LayoutParams()


public class FrameLayoutButton(): UIButton(CGRectZero.readValue()), UIViewWithSizeOverridesProtocol, UIViewWithSpacingRulesProtocol {

    private val tapGestureRecognizer = UITapGestureRecognizer(this, sel_registerName("onclick"))
    private val longPressGestureRecognizer = UILongPressGestureRecognizer(this, sel_registerName("onLongPress"))

    val spacingOverride: Signal<Dimension?> = Signal<Dimension?>(null)
    override fun getSpacingOverrideProperty() = spacingOverride

    private val childSizeCache: ArrayList<HashMap<Size, Size>> = ArrayList()
    public override fun sizeThatFits(size: CValue<CGSize>): CValue<CGSize> = frameLayoutSizeThatFits(size, childSizeCache)
    public override fun layoutSubviews() = frameLayoutLayoutSubviews(childSizeCache)
    public override fun forceRemeasures() = childSizeCache.forEach { it.clear() }
    public override fun subviewDidChangeSizing(view: UIView?) = frameLayoutSubviewDidChangeSizing(view, childSizeCache)
    public override fun didAddSubview(subview: UIView) {
        super.didAddSubview(subview)
        frameLayoutDidAddSubview(subview, childSizeCache)
    }
    public override fun willRemoveSubview(subview: UIView) {
        // Fixes a really cursed crash where "this" is null due to GC interactions
        @Suppress("SENSELESS_COMPARISON", "IfThenToSafeAccess")
        if (this != null) frameLayoutWillRemoveSubview(subview, childSizeCache)
        super.willRemoveSubview(subview)
    }

    public override fun hitTest(point: CValue<CGPoint>, withEvent: UIEvent?): UIView? {
        return frameLayoutHitTest(point, withEvent)
    }

    init {
        userInteractionEnabled = true
        addGestureRecognizer(tapGestureRecognizer)
        addGestureRecognizer(longPressGestureRecognizer)
    }

    fun setOnClick(action: ()->Unit): ()->Unit {
        onClick = action
        return { onClick = null }
    }

    fun setOnLongPress(action: ()->Unit): ()->Unit {
        onLongPress = action
        return { onLongPress = null }
    }

    private var onClick: (()->Unit)? = null
    @ObjCAction
    public fun onclick() {
        if (enabled) {
            onClick?.invoke()
        }
    }

    private var onLongPress: (()->Unit)? = null
    @ObjCAction
    fun onLongPress() {
        if (enabled) {
            onLongPress?.invoke()
        }
    }
}