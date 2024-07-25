package com.lightningkite.kiteui.views.direct

import android.animation.LayoutTransition
import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.FrameLayout

import com.lightningkite.kiteui.models.Dimension
import com.lightningkite.kiteui.models.LinearGradient
import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.models.px
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import kotlin.math.absoluteValue
import kotlin.math.roundToInt


actual class Stack actual constructor(context: RContext) : RView(context) {
    override val native = FrameLayout(context.activity)
    override fun defaultLayoutParams(): ViewGroup.LayoutParams =
        FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
}

actual class RowOrCol actual constructor(context: RContext) : RView(context) {
    override val native = SlightlyModifiedLinearLayout(context.activity)
    override fun defaultLayoutParams(): ViewGroup.LayoutParams =
        SimplifiedLinearLayout.LayoutParams(
            if (vertical) ViewGroup.LayoutParams.MATCH_PARENT else ViewGroup.LayoutParams.WRAP_CONTENT,
            if (vertical) ViewGroup.LayoutParams.WRAP_CONTENT else ViewGroup.LayoutParams.MATCH_PARENT,
        )
    actual var vertical: Boolean
        get() = native.orientation == SimplifiedLinearLayout.VERTICAL
        set(value) {
            native.orientation = if (value) SimplifiedLinearLayout.VERTICAL else SimplifiedLinearLayout.HORIZONTAL
            native.gravity = if (value) Gravity.CENTER_HORIZONTAL else Gravity.CENTER_VERTICAL
        }

    override fun spacingSet(value: Dimension?) {
        super.spacingSet(value)
        native.gap = (value ?: if(useNavSpacing) theme.navSpacing else theme.spacing).value.roundToInt()
    }
    override fun applyForeground(theme: Theme) {
        native.gap = (spacing ?: if(useNavSpacing) theme.navSpacing else theme.spacing).value.roundToInt()
    }
}

actual class RowCollapsingToColumn actual constructor(context: RContext, breakpoints: List<Dimension>) : RView(context) {
    override val native = SlightlyModifiedLinearLayout(context.activity)
    override fun defaultLayoutParams(): ViewGroup.LayoutParams =
        SimplifiedLinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

    init {
        native.orientation = SimplifiedLinearLayout.VERTICAL
        native.gravity = Gravity.CENTER_HORIZONTAL
        reactiveScope {
            val w = AppState.windowInfo().width
            val index = breakpoints.indexOfFirst { w > it }
            if (index == -1 || index % 2 == 1) {
                native.orientation = SimplifiedLinearLayout.VERTICAL
                native.gravity = Gravity.CENTER_HORIZONTAL
                native.ignoreWeights = true
            } else {
                native.orientation = SimplifiedLinearLayout.HORIZONTAL
                native.gravity = Gravity.CENTER_VERTICAL
                native.ignoreWeights = false
            }
        }
    }
    override fun spacingSet(value: Dimension?) {
        super.spacingSet(value)
        native.gap = (value ?: theme.spacing).value.roundToInt()
    }
    override fun applyForeground(theme: Theme) {
        native.gap = (spacing ?: theme.spacing).value.roundToInt()
    }
}

open class SlightlyModifiedLinearLayout(context: Context) : SimplifiedLinearLayout(context) {
    override fun generateDefaultLayoutParams(): LayoutParams? {
        if (orientation == HORIZONTAL) {
            return LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT)
        } else if (orientation == VERTICAL) {
            return LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        return null
    }
}