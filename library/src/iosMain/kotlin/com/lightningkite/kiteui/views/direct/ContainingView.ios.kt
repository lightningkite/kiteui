package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.Dimension
import com.lightningkite.kiteui.models.ThemeAndBack
import com.lightningkite.kiteui.reactive.AppState
import com.lightningkite.readable.reactiveScope
import com.lightningkite.kiteui.views.*


actual class RowOrCol actual constructor(context: RContext) : RView(context) {
    override val native = LinearLayout()
    override val cannotBeCovered: Boolean get() = false

    override fun childTouches(side: Side, child: RView): Boolean {
        return when(side) {
            Side.Left -> if(!vertical) child == children.firstOrNull() else child.native.extensionHorizontalAlign?.touchesStart != false
            Side.Top -> if(vertical) child == children.firstOrNull() else child.native.extensionVerticalAlign?.touchesStart != false
            Side.Right -> if(!vertical) child == children.lastOrNull() else child.native.extensionHorizontalAlign?.touchesEnd != false
            Side.Bottom -> if(vertical) child == children.lastOrNull() else child.native.extensionVerticalAlign?.touchesEnd != false
        }
    }
    actual var vertical: Boolean
        get() = native.horizontal.not()
        set(value) {
            native.horizontal = !value
        }

    actual fun spacingOverrideBeforeNext(amount: Dimension): Unit {
        beforeNextElementSetup {
            native.extensionSpacingBeforeOverride = amount
        }
    }

    override var spacing: Dimension?
        get() = super.spacing
        set(value) {
            super.spacing = value
            native.gap = (value ?: theme.gap).value
        }

    override fun applyTheme(theme: ThemeAndBack) {
        super.applyTheme(theme);
        native.gap = (spacing ?: theme.theme.gap).value
    }
}

actual class RowCollapsingToColumn actual constructor(context: RContext, breakpoints: List<Dimension>) :
    RView(context) {
    override val cannotBeCovered: Boolean get() = false
    override val native = LinearLayout()
    override fun childTouches(side: Side, child: RView): Boolean {
        return when(side) {
            Side.Left -> if(native.horizontal) child == children.firstOrNull() else child.native.extensionHorizontalAlign?.touchesStart != false
            Side.Top -> if(!native.horizontal) child == children.firstOrNull() else child.native.extensionVerticalAlign?.touchesStart != false
            Side.Right -> if(native.horizontal) child == children.lastOrNull() else child.native.extensionHorizontalAlign?.touchesEnd != false
            Side.Bottom -> if(!native.horizontal) child == children.lastOrNull() else child.native.extensionVerticalAlign?.touchesEnd != false
        }
    }

    init {
        reactiveScope {
            val w = AppState.windowInfo().width
            val index = breakpoints.indexOfFirst { w > it }
            if (index == -1 || index % 2 == 1) {
                native.horizontal = false
                native.ignoreWeights = true
            } else {
                native.horizontal = true
                native.ignoreWeights = false
            }
        }
    }

    override var spacing: Dimension?
        get() = super.spacing
        set(value) {
            super.spacing = value
            native.gap = (value ?: theme.gap).value
        }

    override fun applyTheme(theme: ThemeAndBack) {
        super.applyTheme(theme);
        native.gap = (spacing ?: theme.theme.gap).value
    }
}

actual class Frame actual constructor(context: RContext) : RView(context) {
    override val cannotBeCovered: Boolean get() = false
    override val native = FrameLayout()
    override fun childTouches(side: Side, child: RView): Boolean {
        return when(side) {
            Side.Left -> child.native.extensionHorizontalAlign?.touchesStart != false
            Side.Top -> child.native.extensionVerticalAlign?.touchesStart != false
            Side.Right -> child.native.extensionHorizontalAlign?.touchesEnd != false
            Side.Bottom -> child.native.extensionVerticalAlign?.touchesEnd != false
        }
    }
}
