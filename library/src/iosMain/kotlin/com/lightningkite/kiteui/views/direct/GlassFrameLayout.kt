package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.InternalKiteUi
import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.Dimension
import com.lightningkite.kiteui.models.Edges
import com.lightningkite.kiteui.objc.UIViewWithSizeOverridesProtocol
import com.lightningkite.kiteui.objc.UIViewWithSpacingRulesProtocol
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*
import kotlinx.cinterop.*
import platform.CoreGraphics.*
import platform.UIKit.*

/**
 * A UIVisualEffectView that mimics the functionality of FrameLayout.
 * This allows us to have a blur effect and frame layout capabilities in a single view.
 */
@InternalKiteUi
public class GlassFrameLayout : UIVisualEffectView(UIBlurEffect.effectWithStyle(UIBlurEffectStyle.UIBlurEffectStyleLight)), UIViewWithSizeOverridesProtocol, UIViewWithSpacingRulesProtocol {

    public val spacingOverride: Signal<Dimension?> = Signal<Dimension?>(null)
    public override fun getSpacingOverrideProperty(): Signal<Dimension?> = spacingOverride

    private val childSizeCache: ArrayList<HashMap<Size, Size>> = ArrayList()
    public override fun sizeThatFits(size: CValue<CGSize>): CValue<CGSize> = frameLayoutSizeThatFits(size, childSizeCache)
    public override fun layoutSubviews(): Unit = frameLayoutLayoutSubviews(childSizeCache)
    public override fun forceRemeasures(): Unit = childSizeCache.forEach { it.clear() }
    public override fun subviewDidChangeSizing(view: UIView?): Unit = frameLayoutSubviewDidChangeSizing(view, childSizeCache)
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
}
