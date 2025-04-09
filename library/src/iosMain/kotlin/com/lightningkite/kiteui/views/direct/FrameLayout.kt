

package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.Dimension
import com.lightningkite.kiteui.models.SizeConstraints
import com.lightningkite.kiteui.objc.UIViewWithSizeOverridesProtocol
import com.lightningkite.kiteui.objc.UIViewWithSpacingRulesProtocol
import com.lightningkite.readable.Property
import com.lightningkite.kiteui.views.*
import kotlinx.cinterop.*
import platform.CoreGraphics.*
import platform.QuartzCore.CALayer
import platform.QuartzCore.CATextLayer
import platform.UIKit.*
import kotlin.math.max

//private val UIViewLayoutParams = ExtensionProperty<UIView, LayoutParams>()
//val UIView.layoutParams: LayoutParams by UIViewLayoutParams
//
//class LayoutParams()


class FrameLayout : UIView(CGRectZero.readValue()), UIViewWithSizeOverridesProtocol, UIViewWithSpacingRulesProtocol {

    val spacingOverride: Property<Dimension?> = Property<Dimension?>(null)
    override fun getSpacingOverrideProperty() = spacingOverride

    private val childSizeCache: ArrayList<HashMap<Size, Size>> = ArrayList()
    override fun sizeThatFits(size: CValue<CGSize>): CValue<CGSize> = frameLayoutSizeThatFits(size, childSizeCache)
    override fun layoutSubviews() = frameLayoutLayoutSubviews(childSizeCache)
    override fun subviewDidChangeSizing(view: UIView?) = frameLayoutSubviewDidChangeSizing(view, childSizeCache)
    override fun forceRemeasures() = childSizeCache.forEach { it.clear() }
    override fun didAddSubview(subview: UIView) {
        super.didAddSubview(subview)
        frameLayoutDidAddSubview(subview, childSizeCache)
    }
    override fun willRemoveSubview(subview: UIView) {
        // Fixes a really cursed crash where "this" is null due to GC interactions
        @Suppress("SENSELESS_COMPARISON", "IfThenToSafeAccess")
        if (this != null) frameLayoutWillRemoveSubview(subview, childSizeCache)
        super.willRemoveSubview(subview)
    }
    init { userInteractionEnabled = false }
    override fun hitTest(point: CValue<CGPoint>, withEvent: UIEvent?): UIView? {
        return frameLayoutHitTest(point, withEvent)
    }
}
