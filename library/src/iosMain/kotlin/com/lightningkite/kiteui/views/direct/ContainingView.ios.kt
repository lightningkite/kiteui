package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.Dimension
import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.reactive.AppState
import com.lightningkite.kiteui.reactive.WindowInfo
import com.lightningkite.kiteui.reactive.invoke
import com.lightningkite.kiteui.reactive.reactiveScope
import com.lightningkite.kiteui.views.*

import platform.UIKit.UIView
import kotlin.math.absoluteValue


actual class RowOrCol actual constructor(context: RContext): RView(context) {
    override val native = LinearLayout()

    actual var vertical: Boolean
        get() = native.horizontal.not()
        set(value) {
            native.horizontal = !value
        }


    override fun spacingSet(value: Dimension?) {
        super.spacingSet(value)
        native.gap = (value ?: if(useNavSpacing) theme.navSpacing else theme.spacing).value
    }
    override fun applyForeground(theme: Theme) {
        native.gap = (spacing ?: if(useNavSpacing) theme.navSpacing else theme.spacing).value
    }
}

actual class RowCollapsingToColumn actual constructor(context: RContext, breakpoints: List<Dimension>): RView(context) {
    override val native = LinearLayout()
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


    override fun spacingSet(value: Dimension?) {
        super.spacingSet(value)
        native.gap = (value ?: if(useNavSpacing) theme.navSpacing else theme.spacing).value
    }
    override fun applyForeground(theme: Theme) {
        native.gap = (spacing ?: if(useNavSpacing) theme.navSpacing else theme.spacing).value
    }
}

actual class Stack actual constructor(context: RContext): RView(context) {
    override val native = FrameLayout()
}
