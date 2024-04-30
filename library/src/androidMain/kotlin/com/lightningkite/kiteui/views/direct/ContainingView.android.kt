package com.lightningkite.kiteui.views.direct

import android.animation.LayoutTransition
import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout

import com.lightningkite.kiteui.models.Dimension
import com.lightningkite.kiteui.models.LinearGradient
import com.lightningkite.kiteui.models.px
import com.lightningkite.kiteui.reactive.Property
import com.lightningkite.kiteui.reactive.WindowInfo
import com.lightningkite.kiteui.reactive.await
import com.lightningkite.kiteui.reactive.invoke
import com.lightningkite.kiteui.views.ViewDsl
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.reactiveScope

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual typealias NContainingView = ViewGroup

@ViewDsl
actual inline fun ViewWriter.stackActual(crossinline setup: ContainingView.() -> Unit) = viewElement(
    factory = ::SlightlyModifiedFrameLayout,
    wrapper = ::ContainingView
) {
    handleTheme(native, viewDraws = false) {
        setup(this)
    }
}

@ViewDsl
actual inline fun ViewWriter.colActual(crossinline setup: ContainingView.() -> Unit) {
    viewElement(factory = ::SlightlyModifiedLinearLayout, wrapper = ::ContainingView) {
        val l = native as SlightlyModifiedLinearLayout
        l.orientation = SimplifiedLinearLayout.VERTICAL
        l.gravity = Gravity.CENTER_HORIZONTAL
        handleTheme(l, viewDraws = false, foreground = { t, v ->
            v.gap = (v.spacingOverride.value ?: t.spacing).value.toInt()
        }) {
            setup(ContainingView(l))
        }
    }
}

@ViewDsl
actual inline fun ViewWriter.rowActual(crossinline setup: ContainingView.() -> Unit) {
    viewElement(factory = ::SlightlyModifiedLinearLayout, wrapper = ::ContainingView) {
        val l = native as SlightlyModifiedLinearLayout
        l.orientation = SimplifiedLinearLayout.HORIZONTAL
        l.gravity = Gravity.CENTER_VERTICAL
        handleTheme(l, viewDraws = false, foreground = { t, v ->
            v.gap = (v.spacingOverride.value ?: t.spacing).value.toInt()
        }) {
            setup(ContainingView(l))
        }
    }
}

private fun LinearGradient.orientation(): GradientDrawable.Orientation {
    return when (angle.degrees.toInt()) {
        in 0..90 -> GradientDrawable.Orientation.LEFT_RIGHT
        in 91..180 -> GradientDrawable.Orientation.TOP_BOTTOM
        in 181..270 -> GradientDrawable.Orientation.RIGHT_LEFT
        in 271..360 -> GradientDrawable.Orientation.BOTTOM_TOP
        else -> GradientDrawable.Orientation.LEFT_RIGHT
    }
}

interface HasSpacingMultiplier {
    val spacingOverride: Property<Dimension?>
}

open  class SlightlyModifiedFrameLayout(context: Context) : FrameLayout(context), HasSpacingMultiplier {
    override val spacingOverride: Property<Dimension?> = Property<Dimension?>(null)
}

open class SlightlyModifiedLinearLayout(context: Context) : SimplifiedLinearLayout(context), HasSpacingMultiplier {
    override val spacingOverride: Property<Dimension?> = Property<Dimension?>(null).apply {
        addListener {
            value?.value?.toInt()?.let {
                this@SlightlyModifiedLinearLayout.gap = it
            }
        }
    }
    override fun generateDefaultLayoutParams(): LayoutParams? {
        if (orientation == HORIZONTAL) {
            return LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT)
        } else if (orientation == VERTICAL) {
            return LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        return null
    }
}

actual var ContainingView.vertical: Boolean
    get() = (native as? SimplifiedLinearLayout)?.orientation == SimplifiedLinearLayout.VERTICAL
    set(value) {
        (native as? SimplifiedLinearLayout)?.orientation = if (value) SimplifiedLinearLayout.VERTICAL else SimplifiedLinearLayout.HORIZONTAL
    }

@ViewDsl
actual fun ViewWriter.rowCollapsingToColumnActual(
    breakpoint: Dimension,
    setup: ContainingView.() -> Unit
) {
    viewElement(factory = ::SlightlyModifiedLinearLayout, wrapper = ::ContainingView) {
        val l = native as SlightlyModifiedLinearLayout
        l.orientation = SimplifiedLinearLayout.VERTICAL
        l.gravity = Gravity.CENTER_HORIZONTAL
        reactiveScope {
            if(WindowInfo().width < breakpoint) {
                l.orientation = SimplifiedLinearLayout.VERTICAL
                l.gravity = Gravity.CENTER_HORIZONTAL
                l.ignoreWeights = true
            } else {
                l.orientation = SimplifiedLinearLayout.HORIZONTAL
                l.gravity = Gravity.CENTER_VERTICAL
                l.ignoreWeights = false
            }
        }
        handleTheme(l, viewDraws = false, foreground = { t, v ->
            v.gap = (v.spacingOverride.value ?: t.spacing).value.toInt()
        }) {
            setup(ContainingView(l))
        }
    }
}