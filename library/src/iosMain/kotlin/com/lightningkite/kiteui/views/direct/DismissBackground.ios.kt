package com.lightningkite.kiteui.views.direct


import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.navigation.dialogScreenNavigator
import com.lightningkite.kiteui.objc.UIViewWithSizeOverridesProtocol
import com.lightningkite.kiteui.objc.UIViewWithSpacingRulesProtocol
import com.lightningkite.kiteui.reactive.CalculationContext
import com.lightningkite.kiteui.reactive.Property
import com.lightningkite.kiteui.views.*
import kotlinx.cinterop.*
import kotlinx.coroutines.launch
import platform.CoreGraphics.CGPoint
import platform.CoreGraphics.CGRectZero
import platform.CoreGraphics.CGSize
import platform.UIKit.*
import platform.darwin.sel_registerName


actual class DismissBackground actual constructor(context: RContext) : RView(context) {
    override val native = NDismissBackground(this)

    actual fun onClick(action: suspend () -> Unit): Unit {
        native.onClick = action
    }

    override fun postSetup() {
        super.postSetup()
        children.forEach { it.native.userInteractionEnabled = true }
    }

    init {
        onClick { dialogScreenNavigator.clear() }
    }

    override fun applyState(theme: ThemeAndBack): ThemeAndBack {
        return super.applyState(theme[DismissSemantic])
    }
}


@Suppress("ACTUAL_WITHOUT_EXPECT")

actual class NDismissBackground(val calculationContext: CalculationContext) : UIButton(CGRectZero.readValue()),
    UIViewWithSizeOverridesProtocol,
    UIViewWithSpacingRulesProtocol {
    var padding: Double
        get() = extensionPadding ?: 0.0
        set(value) {
            extensionPadding = value
        }

    var onClick: suspend () -> Unit = {}
    val spacingOverride: Property<Dimension?> = Property<Dimension?>(null)
    var anchor: Pair<PopoverPreferredDirection, UIView>? = null
    override fun getSpacingOverrideProperty() = spacingOverride
    private val childSizeCache: ArrayList<HashMap<Size, Size>> = ArrayList()
    override fun sizeThatFits(size: CValue<CGSize>): CValue<CGSize> = frameLayoutSizeThatFits(size, childSizeCache)
    override fun layoutSubviews() {
        val anchor = anchor
        if (anchor == null) {
            frameLayoutLayoutSubviews(childSizeCache)
        } else {
            frameLayoutLayoutAnchoredSubviews(childSizeCache, anchor)
        }
    }
    override fun subviewDidChangeSizing(view: UIView?) = frameLayoutSubviewDidChangeSizing(view, childSizeCache)
    override fun didAddSubview(subview: UIView) {
        super.didAddSubview(subview)
        frameLayoutDidAddSubview(subview, childSizeCache)
    }

    override fun willRemoveSubview(subview: UIView) {
        // Fixes a really cursed crash where "this" is null due to GC interactions
        @Suppress("SENSELESS_COMPARISON")
        if (this != null) frameLayoutWillRemoveSubview(subview, childSizeCache)
        super.willRemoveSubview(subview)
    }

    override fun hitTest(point: CValue<CGPoint>, withEvent: UIEvent?): UIView? {
        if (hidden) return null
        var inBoundsOfOther = false
        if (bounds.useContents {
                val rect = this
                point.useContents {
                    val point = this
                    point.x >= rect.origin.x &&
                            point.y >= rect.origin.y &&
                            point.x <= rect.origin.x + rect.size.width &&
                            point.y <= rect.origin.y + rect.size.height
                }
            }) {
            subviews.asReversed().forEach {
                it as UIView
                if (it.hidden) return@forEach
                val c = it.convertPoint(point = point, fromCoordinateSpace = this as UICoordinateSpaceProtocol)
                inBoundsOfOther = inBoundsOfOther || c.useContents {
                    val point = this
                    it.bounds.useContents {
                        val rect = this
                        point.x >= rect.origin.x &&
                                point.y >= rect.origin.y &&
                                point.x <= rect.origin.x + rect.size.width &&
                                point.y <= rect.origin.y + rect.size.height
                    }
                }
                it.hitTest(
                    c,
                    withEvent
                )
                    ?.let { return it }
            }
            return if (inBoundsOfOther) null else this
        } else {
            return null
        }
    }

    init {
        addTarget(this, sel_registerName("onclick"), UIControlEventTouchUpInside)
    }

    @ObjCAction
    fun onclick() {
        this.calculationContext.launch { onClick() }
    }
}