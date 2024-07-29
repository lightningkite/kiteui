@file:OptIn(ExperimentalForeignApi::class)

package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.launchManualCancel
import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.SizeConstraints
import com.lightningkite.kiteui.objc.UIViewWithSizeOverridesProtocol
import com.lightningkite.kiteui.objc.UIViewWithSpacingRulesProtocol
import com.lightningkite.kiteui.models.Dimension
import com.lightningkite.kiteui.reactive.CalculationContext
import com.lightningkite.kiteui.reactive.Property
import com.lightningkite.kiteui.views.*
import kotlinx.cinterop.*
import platform.CoreGraphics.*
import platform.QuartzCore.CATextLayer
import platform.UIKit.*
import platform.darwin.sel_registerName
import kotlin.experimental.ExperimentalNativeApi
import kotlin.math.max
import kotlin.native.ref.WeakReference

//private val UIViewLayoutParams = ExtensionProperty<UIView, LayoutParams>()
//val UIView.layoutParams: LayoutParams by UIViewLayoutParams
//
//class LayoutParams()

@OptIn(ExperimentalForeignApi::class)
class FrameLayoutButton(val calculationContext: CalculationContext): UIButton(CGRectZero.readValue()), UIViewWithSizeOverridesProtocol, UIViewWithSpacingRulesProtocol {
    var padding: Double
        get() = extensionPadding ?: 0.0
        set(value) { extensionPadding = value }

    var onClick: suspend ()->Unit = {}
    val spacingOverride: Property<Dimension?> = Property<Dimension?>(null)
    override fun getSpacingOverrideProperty() = spacingOverride
    private val childSizeCache: ArrayList<HashMap<Size, Size>> = ArrayList()
    override fun sizeThatFits(size: CValue<CGSize>): CValue<CGSize> = frameLayoutSizeThatFits(size, childSizeCache)
    override fun layoutSubviews() = frameLayoutLayoutSubviews(childSizeCache)
    override fun subviewDidChangeSizing(view: UIView?) = frameLayoutSubviewDidChangeSizing(view, childSizeCache)
    override fun didAddSubview(subview: UIView) {
        super.didAddSubview(subview)
        frameLayoutDidAddSubview(subview, childSizeCache)
    }
    override fun willRemoveSubview(subview: UIView) {
        if(this != null) frameLayoutWillRemoveSubview(subview, childSizeCache)
        super.willRemoveSubview(subview)
    }

    override fun hitTest(point: CValue<CGPoint>, withEvent: UIEvent?): UIView? {
        return frameLayoutHitTest(point, withEvent)
    }
    init {
        addTarget(this, sel_registerName("onclick"), UIControlEventTouchUpInside)
    }

    var virtualEnable = true
    @ObjCAction
    fun onclick() {
        if (enabled && virtualEnable) {
            calculationContext?.launchManualCancel {
                virtualEnable = false
                try { onClick() } finally { virtualEnable = true }
            }
        }
    }
}