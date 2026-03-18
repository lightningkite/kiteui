package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.Dimension
import com.lightningkite.kiteui.models.ThemeAndBack
import com.lightningkite.kiteui.reactive.AppState
import com.lightningkite.kiteui.views.*
import com.lightningkite.reactive.context.*


actual class RowOrCol actual constructor(context: ElementContext) : RView(context) {
    override val native = LinearLayout()
    

    actual var vertical: Boolean
        get() = native.horizontal.not()
        set(value) {
            native.horizontal = !value
        }

    actual fun spacingOverrideBeforeNext(amount: Dimension): Unit {
        // TODO: This won't work
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
        // Apply parent's default alignment if child doesn't have explicit alignment set
        if (view.lastSetHorizontalAlign == com.lightningkite.kiteui.models.Align.Stretch && newChildHorizontalAlign != null) {
            view.lastSetHorizontalAlign = newChildHorizontalAlign!!
            view.native.extensionHorizontalAlign = newChildHorizontalAlign
        }
        if (view.lastSetVerticalAlign == com.lightningkite.kiteui.models.Align.Stretch && newChildVerticalAlign != null) {
            view.lastSetVerticalAlign = newChildVerticalAlign!!
            view.native.extensionVerticalAlign = newChildVerticalAlign
        }

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
        native.arrangedSubviews[index].removeFromSuperview()
    }

    override fun internalClearChildren() {
        native.arrangedSubviews.toList().forEach {
            it.removeFromSuperview()
        }
    }
}

actual class RowCollapsingToColumn actual constructor(context: ElementContext, breakpoints: List<Dimension>) :
    RView(context) {
    
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
        // Apply parent's default alignment if child doesn't have explicit alignment set
        if (view.lastSetHorizontalAlign == com.lightningkite.kiteui.models.Align.Stretch && newChildHorizontalAlign != null) {
            view.lastSetHorizontalAlign = newChildHorizontalAlign!!
            view.native.extensionHorizontalAlign = newChildHorizontalAlign
        }
        if (view.lastSetVerticalAlign == com.lightningkite.kiteui.models.Align.Stretch && newChildVerticalAlign != null) {
            view.lastSetVerticalAlign = newChildVerticalAlign!!
            view.native.extensionVerticalAlign = newChildVerticalAlign
        }

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
        native.arrangedSubviews[index].removeFromSuperview()
    }

    override fun internalClearChildren() {
        native.arrangedSubviews.toList().forEach {
            it.removeFromSuperview()
        }
    }
}

actual class Frame actual constructor(context: ElementContext) : RView(context) {
    
    override val native = FrameLayout()
}
