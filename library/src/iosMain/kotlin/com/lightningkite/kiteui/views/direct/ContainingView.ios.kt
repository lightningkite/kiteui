package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.Dimension
import com.lightningkite.kiteui.models.ThemeAndBack
import com.lightningkite.kiteui.reactive.AppState
import com.lightningkite.readable.reactiveScope
import com.lightningkite.kiteui.views.*
import platform.UIKit.UIView


actual class RowOrCol actual constructor(context: RContext) : RView(context) {
    override val native = LinearLayout()
    override val edgeTouchHelper: EdgeTouchHelper = LinearEdgeTouchHelper(this)
    init { cannotBeCovered = false }

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
    override fun internalAddChild(index: Int, view: RView) {
        if (index == native.arrangedSubviews.size)
            native.addArrangedSubview(view.native)
        else
            native.insertArrangedSubview(view.native, index.toLong())
        if (children[index].native != native.arrangedSubviews.get(index)) throw IllegalStateException("Children mismatch! ${children.map { it.native }} vs ${native.arrangedSubviews}")
    }

    override fun internalRemoveChild(index: Int) {
        if (children[index].native != native.arrangedSubviews.get(index)) throw IllegalStateException("Children mismatch! ${children.map { it.native }} vs ${native.arrangedSubviews}")
        if (index >= native.arrangedSubviews.size || index < 0) {
            throw IllegalStateException("Index $index not in 0..<${native.arrangedSubviews.size}")
        }
        (native.arrangedSubviews[index] as UIView).removeFromSuperview()
    }

    override fun internalClearChildren() {
        native.arrangedSubviews.toList().forEach {
            (it as UIView).let {
                it.removeFromSuperview()
            }
        }
    }
}

actual class RowCollapsingToColumn actual constructor(context: RContext, breakpoints: List<Dimension>) :
    RView(context) {
    init { cannotBeCovered = false }
    override val edgeTouchHelper: EdgeTouchHelper = RowCollapsingEdgeTouchHelper(this)
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

    override fun internalAddChild(index: Int, view: RView) {
        if (index == native.arrangedSubviews.size)
            native.addArrangedSubview(view.native)
        else
            native.insertArrangedSubview(view.native, index.toLong())
        if (children[index].native != native.arrangedSubviews.get(index)) throw IllegalStateException("Children mismatch! ${children.map { it.native }} vs ${native.arrangedSubviews}")
    }

    override fun internalRemoveChild(index: Int) {
        if (children[index].native != native.arrangedSubviews.get(index)) throw IllegalStateException("Children mismatch! ${children.map { it.native }} vs ${native.arrangedSubviews}")
        if (index >= native.arrangedSubviews.size || index < 0) {
            throw IllegalStateException("Index $index not in 0..<${native.arrangedSubviews.size}")
        }
        (native.arrangedSubviews[index] as UIView).removeFromSuperview()
    }

    override fun internalClearChildren() {
        native.arrangedSubviews.toList().forEach {
            (it as UIView).let {
                it.removeFromSuperview()
            }
        }
    }
}

actual class Frame actual constructor(context: RContext) : RView(context) {
    init { cannotBeCovered = false }
    override val native = FrameLayout()
}
