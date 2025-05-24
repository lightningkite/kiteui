package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.Dimension
import com.lightningkite.kiteui.models.ThemeAndBack
import com.lightningkite.kiteui.reactive.AppState
import com.lightningkite.readable.reactiveScope
import com.lightningkite.kiteui.views.*


actual class RowOrCol actual constructor(context: RContext, cannotBeCovered: Boolean) : RView(context) {
    override val native = LinearLayout()
    override val cannotBeCovered: Boolean = cannotBeCovered

    override fun childTouchesEdge(child: RView, edge: SafeAreaEdge): Boolean {
        return when(edge) {
            SafeAreaEdge.LEFT -> if(!vertical) child == children.firstOrNull() else child.native.extensionHorizontalAlign?.touchesStart != false
            SafeAreaEdge.TOP -> if(vertical) child == children.firstOrNull() else child.native.extensionVerticalAlign?.touchesStart != false
            SafeAreaEdge.RIGHT -> if(!vertical) child == children.lastOrNull() else child.native.extensionHorizontalAlign?.touchesEnd != false
            SafeAreaEdge.BOTTOM -> if(vertical) child == children.lastOrNull() else child.native.extensionVerticalAlign?.touchesEnd != false
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

    override var gap: Dimension?
        get() = super.gap
        set(value) {
            super.gap = value
            native.gap = (value ?: theme.gap).value
        }

    override fun applyTheme(theme: ThemeAndBack) {
        super.applyTheme(theme);
        native.gap = (gap ?: theme.theme.gap).value
    }
}

actual class RowCollapsingToColumn actual constructor(context: RContext, breakpoints: List<Dimension>) :
    RView(context) {
    override val cannotBeCovered: Boolean get() = false
    override val native = LinearLayout()
    override fun childTouchesEdge(child: RView, edge: SafeAreaEdge): Boolean {
        return when(edge) {
            SafeAreaEdge.LEFT -> if(native.horizontal) child == children.firstOrNull() else child.native.extensionHorizontalAlign?.touchesStart != false
            SafeAreaEdge.TOP -> if(!native.horizontal) child == children.firstOrNull() else child.native.extensionVerticalAlign?.touchesStart != false
            SafeAreaEdge.RIGHT -> if(native.horizontal) child == children.lastOrNull() else child.native.extensionHorizontalAlign?.touchesEnd != false
            SafeAreaEdge.BOTTOM -> if(!native.horizontal) child == children.lastOrNull() else child.native.extensionVerticalAlign?.touchesEnd != false
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

    override var gap: Dimension?
        get() = super.gap
        set(value) {
            super.gap = value
            native.gap = (value ?: theme.gap).value
        }

    override fun applyTheme(theme: ThemeAndBack) {
        super.applyTheme(theme);
        native.gap = (gap ?: theme.theme.gap).value
    }
}

actual class Frame actual constructor(context: RContext, cannotBeCovered: Boolean) : RView(context) {
    override val cannotBeCovered: Boolean = cannotBeCovered
    override val native = FrameLayout()
    override fun childTouchesEdge(child: RView, edge: SafeAreaEdge): Boolean {
        return when(edge) {
            SafeAreaEdge.LEFT -> child.native.extensionHorizontalAlign?.touchesStart != false
            SafeAreaEdge.TOP -> child.native.extensionVerticalAlign?.touchesStart != false
            SafeAreaEdge.RIGHT -> child.native.extensionHorizontalAlign?.touchesEnd != false
            SafeAreaEdge.BOTTOM -> child.native.extensionVerticalAlign?.touchesEnd != false
        }
    }
}
