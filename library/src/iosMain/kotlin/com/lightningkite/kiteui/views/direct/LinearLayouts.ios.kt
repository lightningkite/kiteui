package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.Dimension
import com.lightningkite.kiteui.models.ThemeAndBack
import com.lightningkite.kiteui.reactive.AppState
import com.lightningkite.kiteui.views.*
import com.lightningkite.reactive.context.reactive

public abstract class NativeLinearLayoutElement(context: ElementContext) : NativeContainerElement(context), LinearLayoutElement {
    override val native = LinearLayout()

    override var gap: Dimension? = null
        set(value) {
            field = value
            native.spacingOverride.value = value
            val gap = mySpacing.value
            for (child in children) {
                child.native.layoutLayers(gap)
            }
        }

    override fun nativeApplyTheme(theme: ThemeAndBack) {
        super.nativeApplyTheme(theme);
        native.gap = (gap ?: theme.theme.gap).value
    }

    override fun nativeAddChild(index: Int, element: Element) {
        if (index == native.arrangedSubviews.size)
            native.addArrangedSubview(element.native)
        else
            native.insertArrangedSubview(element.native, index.toLong())
        if (children[index].native != native.arrangedSubviews[index]) throw IllegalStateException("Children mismatch! ${children.map { it.native }} vs ${native.arrangedSubviews}")
    }

    override fun nativeRemoveChild(index: Int) {
        if (children[index].native != native.arrangedSubviews[index]) throw IllegalStateException("Children mismatch! ${children.map { it.native }} vs ${native.arrangedSubviews}")
        if (index >= native.arrangedSubviews.size || index < 0) {
            throw IllegalStateException("Index $index not in 0..<${native.arrangedSubviews.size}")
        }
        native.arrangedSubviews[index].removeFromSuperview()
    }

    override fun nativeClearChildren() {
        native.arrangedSubviews.toList().forEach {
            it.removeFromSuperview()
        }
    }
}

public actual class RowOrCol actual constructor(context: ElementContext) : NativeLinearLayoutElement(context) {
    public actual var vertical: Boolean
        get() = native.horizontal.not()
        set(value) {
            native.horizontal = !value
        }

    @Deprecated(message = "This no longer works.")
    public actual fun spacingOverrideBeforeNext(amount: Dimension) {
    }
}

public actual class RowCollapsingToColumn actual constructor(context: ElementContext, breakpoints: List<Dimension>) : NativeLinearLayoutElement(context) {
    init {
        reactive {
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
}

public actual class RowWrapping actual constructor(context: ElementContext) : NativeContainerElement(context), LinearLayoutElement {
    override val native = FlexLayout()

    actual override var gap: Dimension? = null
        set(value) {
            field = value
            native.gap = (value ?: theme.gap).value
            native.lineGap = (value ?: theme.gap).value
        }

    override fun nativeApplyTheme(theme: ThemeAndBack) {
        super.nativeApplyTheme(theme)
        native.gap = (gap ?: theme.theme.gap).value
        native.lineGap = (gap ?: theme.theme.gap).value
    }

    override fun nativeAddChild(index: Int, element: Element) {
        if (index == native.arrangedSubviews.size)
            native.addArrangedSubview(element.native)
        else
            native.insertArrangedSubview(element.native, index.toLong())
        if (children[index].native != native.arrangedSubviews.get(index)) throw IllegalStateException("Children mismatch! ${children.map { it.native }} vs ${native.arrangedSubviews}")
    }

    override fun nativeRemoveChild(index: Int) {
        if (children[index].native != native.arrangedSubviews.get(index)) throw IllegalStateException("Children mismatch! ${children.map { it.native }} vs ${native.arrangedSubviews}")
        if (index >= native.arrangedSubviews.size || index < 0) {
            throw IllegalStateException("Index $index not in 0..<${native.arrangedSubviews.size}")
        }
        native.arrangedSubviews[index].removeFromSuperview()
    }

    override fun nativeClearChildren() {
        native.arrangedSubviews.toList().forEach {
            it.removeFromSuperview()
        }
    }
}