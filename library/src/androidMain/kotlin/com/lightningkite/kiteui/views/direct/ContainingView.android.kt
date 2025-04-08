package com.lightningkite.kiteui.views.direct

import android.content.Context
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout

import com.lightningkite.kiteui.models.Dimension
import com.lightningkite.kiteui.models.ThemeAndBack
import com.lightningkite.kiteui.reactive.AppState
import com.lightningkite.readable.*
import com.lightningkite.kiteui.views.*
import kotlin.math.roundToInt


actual class Frame actual constructor(context: RContext) : RView(context) {
    override val cannotBeCovered: Boolean get() = false
    override val native = FrameLayout(context.activity)
    override fun childTouches(child: RView): Int {
        val p = child.lparams as FrameLayout.LayoutParams
        var total = 0
        if(p.width == ViewGroup.LayoutParams.MATCH_PARENT) total = total or Gravity.LEFT or Gravity.RIGHT
        if(p.height == ViewGroup.LayoutParams.MATCH_PARENT) total = total or Gravity.TOP or Gravity.BOTTOM
        if(p.gravity and Gravity.LEFT > 0) total = total or Gravity.LEFT
        if(p.gravity and Gravity.RIGHT > 0) total = total or Gravity.RIGHT
        if(p.gravity and Gravity.TOP > 0) total = total or Gravity.TOP
        if(p.gravity and Gravity.BOTTOM > 0) total = total or Gravity.BOTTOM
        return total
    }
    override fun defaultLayoutParams(): ViewGroup.LayoutParams =
        FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
}

actual class RowOrCol actual constructor(context: RContext) : RView(context) {
    override val cannotBeCovered: Boolean get() = false
    override val native = SlightlyModifiedLinearLayout(context.activity)
    override fun defaultLayoutParams(): ViewGroup.LayoutParams =
        SimplifiedLinearLayout.LayoutParams(
            if (vertical) ViewGroup.LayoutParams.MATCH_PARENT else ViewGroup.LayoutParams.WRAP_CONTENT,
            if (vertical) ViewGroup.LayoutParams.WRAP_CONTENT else ViewGroup.LayoutParams.MATCH_PARENT,
        )
    override fun childTouches(child: RView): Int {
        val p = child.lparams as SimplifiedLinearLayout.LayoutParams
        var total = 0
        if(vertical && p.width == ViewGroup.LayoutParams.MATCH_PARENT) total = total or Gravity.LEFT or Gravity.RIGHT
        if(!vertical && p.height == ViewGroup.LayoutParams.MATCH_PARENT) total = total or Gravity.TOP or Gravity.BOTTOM
        if(vertical && p.gravity and Gravity.LEFT > 0) total = total or Gravity.LEFT
        if(vertical && p.gravity and Gravity.RIGHT > 0) total = total or Gravity.RIGHT
        if(!vertical && p.gravity and Gravity.TOP > 0) total = total or Gravity.TOP
        if(!vertical && p.gravity and Gravity.BOTTOM > 0) total = total or Gravity.BOTTOM
        if(child == children.firstOrNull()) {
            if(vertical) total = total or Gravity.TOP
            else total = total or Gravity.LEFT
        }
        if(child == children.lastOrNull()) {
            if(vertical) total = total or Gravity.BOTTOM
            else total = total or Gravity.RIGHT
        }
        return total
    }
    actual var vertical: Boolean
        get() = native.orientation == SimplifiedLinearLayout.VERTICAL
        set(value) {
            native.orientation = if (value) SimplifiedLinearLayout.VERTICAL else SimplifiedLinearLayout.HORIZONTAL
            native.gravity = if (value) Gravity.CENTER_HORIZONTAL else Gravity.CENTER_VERTICAL
        }
    actual fun spacingOverrideBeforeNext(amount: Dimension): Unit {
        beforeNextElementSetup {
            try {
                val lp = (lparams as SimplifiedLinearLayoutLayoutParams)
                lp.gapBeforeOverride = amount.value.toInt()
            } catch (ex: Throwable) {
                RuntimeException("Weight is only available within a column or row, but the parent is a ${parent?.native?.let { it::class.simpleName }}").printStackTrace()
            }
        }
    }
    override var gap: Dimension?
        get() = super.gap
        set(value) {
            super.gap = value
            native.gap = (value ?: theme.gap).value.roundToInt()
        }
    override fun applyTheme(theme: ThemeAndBack) { super.applyTheme(theme); val theme = theme.theme
        native.gap = (spacing ?: theme.gap).value.roundToInt()
    }
}

actual class RowCollapsingToColumn actual constructor(context: RContext, breakpoints: List<Dimension>) : RView(context) {
    override val cannotBeCovered: Boolean get() = false
    override val native = SlightlyModifiedLinearLayout(context.activity)
    override fun defaultLayoutParams(): ViewGroup.LayoutParams =
        SimplifiedLinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    override fun childTouches(child: RView): Int {
        val vertical = native.orientation == SimplifiedLinearLayout.VERTICAL
        val p = child.lparams as SimplifiedLinearLayout.LayoutParams
        var total = 0
        if(vertical && p.width == ViewGroup.LayoutParams.MATCH_PARENT) total = total or Gravity.LEFT or Gravity.RIGHT
        if(!vertical && p.height == ViewGroup.LayoutParams.MATCH_PARENT) total = total or Gravity.TOP or Gravity.BOTTOM
        if(vertical && p.gravity and Gravity.LEFT == Gravity.LEFT) total = total or Gravity.LEFT
        if(vertical && p.gravity and Gravity.RIGHT == Gravity.RIGHT) total = total or Gravity.RIGHT
        if(!vertical && p.gravity and Gravity.TOP == Gravity.TOP) total = total or Gravity.TOP
        if(!vertical && p.gravity and Gravity.BOTTOM == Gravity.BOTTOM) total = total or Gravity.BOTTOM
        if(child == children.firstOrNull()) {
            if(vertical) total = total or Gravity.TOP
            else total = total or Gravity.LEFT
        }
        if(child == children.lastOrNull()) {
            if(vertical) total = total or Gravity.BOTTOM
            else total = total or Gravity.RIGHT
        }
        return total
    }

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
    override var gap: Dimension?
        get() = super.gap
        set(value) {
            super.gap = value
            native.gap = (value ?: theme.gap).value.roundToInt()
        }
    override fun applyTheme(theme: ThemeAndBack) { super.applyTheme(theme); val theme = theme.theme
        native.gap = (spacing ?: theme.gap).value.roundToInt()
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