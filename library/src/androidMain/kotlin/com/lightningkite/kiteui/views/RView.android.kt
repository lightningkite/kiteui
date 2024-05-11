package com.lightningkite.kiteui.views

import android.animation.ValueAnimator
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.view.View
import android.view.ViewGroup
import android.widget.ScrollView
import androidx.core.view.ScrollingView
import com.lightningkite.kiteui.afterTimeout
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.views.direct.DesiredSizeView
import com.lightningkite.kiteui.views.direct.colorInt
import com.lightningkite.kiteui.views.direct.setPaddingAll
import kotlin.math.min
import kotlin.math.roundToInt

actual abstract class RView(context: RContext) : RViewHelper(context) {
    abstract val native: View

    actual override fun opacitySet(value: Double) {
        if (animationsEnabled) {
            ValueAnimator.ofFloat(native.alpha, value.toFloat()).apply {
                addUpdateListener {
                    native.alpha = animatedValue as Float
                }
            }.start()
        } else {
            native.alpha = value.toFloat()
        }
    }

    actual override fun existsSet(value: Boolean) {
        native.visibility = if (value) {
            View.VISIBLE
        } else {
            View.GONE
        }
        (parent?.native as? DesiredSizeView)?.apply {
            visibility = native.visibility
        }
    }

    actual override fun visibleSet(value: Boolean) {
        native.visibility = if (value) {
            View.VISIBLE
        } else {
            View.INVISIBLE
        }
    }

    actual override fun spacingSet(value: Dimension?) {
        if(useBackground != UseBackground.No) {
            native.setPaddingAll((value ?: theme.spacing).value.roundToInt())
        }
        for(child in children) child.updateCorners()
    }

    actual override fun ignoreInteractionSet(value: Boolean) {
        native.isClickable = !value
        native.isFocusable = !value
    }

    actual override fun forcePaddingSet(value: Boolean?) {
    }

    actual override fun scrollIntoView(horizontal: Align?, vertical: Align?, animate: Boolean) {
        generateSequence(native) {
            it.parent as? View
        }.first {
            when (it) {
                is ScrollView -> {
                    scrollToView(it, native, animate)
                    true
                }

                is ScrollingView -> {
                    // TODO
                    true
                }

                else -> false
            }
        }
    }

    actual override fun requestFocus() {
        afterTimeout(16) {
            native.requestFocus()
        }
    }

    protected var background: Drawable? = null
        set(value) {
            field = value
            native.background = value
        }
    protected var backgroundBlock: GradientDrawable? = null
    protected fun updateCorners() {
        val cr = when (val it = theme.cornerRadii) {
            is CornerRadii.ForceConstant -> it.value.value
            is CornerRadii.RatioOfSize -> 10000f
            is CornerRadii.Constant -> min(parentSpacing.value, it.value.value)
            is CornerRadii.RatioOfSpacing -> it.value * parentSpacing.value
        }
        backgroundBlock?.cornerRadii = floatArrayOf(cr, cr, cr, cr, cr, cr, cr, cr)
//        native.elevation = native.elevation.coerceAtMost(parentSpacing)
    }

    actual override fun applyElevation(dimension: Dimension) { native.elevation = dimension.value }
    actual override fun applyPadding(dimension: Dimension?) { native.setPaddingAll(dimension?.value?.roundToInt() ?: 0) }
    actual override fun applyBackground(theme: Theme, fullyApply: Boolean) {
        val view = native
        if (fullyApply) {
            val backgroundDrawable = theme.backgroundDrawableWithoutCorners(background as? GradientDrawable)
            backgroundBlock = backgroundDrawable
            updateCorners()
            background = backgroundDrawable
        } else {
            backgroundBlock = null
            view.elevation = 0f
            background = null
        }
    }

    actual override fun applyForeground(theme: Theme) {

    }

    actual override fun internalAddChild(index: Int, view: RView) {
        (native as ViewGroup).addView(view.native, index)
    }

    actual override fun internalRemoveChild(index: Int) {
        (native as ViewGroup).removeViewAt(index)
    }

    actual override fun internalClearChildren() {
        (native as ViewGroup).removeAllViews()
    }

    protected fun getBackgroundWithRipple(theme: Theme, fullyApply: Boolean, oldRippleDrawable: RippleDrawable?): RippleDrawable {
        val rippleColor = ColorStateList.valueOf(theme.hover().background.colorInt())
        val backgroundDrawable = if (fullyApply) {
            theme.backgroundDrawableWithoutCorners(oldRippleDrawable?.getDrawable(0) as? GradientDrawable)
        } else {
            GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                colors = intArrayOf(theme.background.applyAlpha(0.01f).colorInt(), theme.background.applyAlpha(0.01f).colorInt())
            }
        }
        backgroundBlock = backgroundDrawable
        return oldRippleDrawable?.apply {
            setColor(rippleColor)
            setDrawable(0, backgroundDrawable)
        } ?: RippleDrawable(rippleColor, backgroundDrawable, null)
    }
    protected fun applyBackgroundWithRipple(theme: Theme, fullyApply: Boolean) {
        background = getBackgroundWithRipple(theme, fullyApply, background as? RippleDrawable)
        updateCorners()
    }
}

var animationsEnabled: Boolean = true
actual inline fun RView.withoutAnimation(action: () -> Unit) = native.withoutAnimation(action)
inline fun View.withoutAnimation(action: () -> Unit) {
    if (!animationsEnabled) {
        action()
        return
    }
    try {
        animationsEnabled = false
        action()
    } finally {
        animationsEnabled = true
    }
}
