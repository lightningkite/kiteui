package com.lightningkite.kiteui.views

import android.animation.ValueAnimator
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ScrollView
import androidx.core.widget.NestedScrollView
import com.lightningkite.kiteui.afterTimeout
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.views.direct.DesiredSizeView
import com.lightningkite.kiteui.views.direct.colorInt
import kotlin.math.min

actual abstract class RView actual constructor(context: RContext) : RViewHelper(context) {
    abstract val native: View

    init {
        if (Looper.myLooper() != Looper.getMainLooper())
            throw Exception("Cannot create views on any thread but the main thread")
    }

    actual override var showOnPrint: Boolean = true

    open fun defaultLayoutParams(): LayoutParams =
        FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)

    override var opacity: Double
        get() = super.opacity
        set(value) {
            super.opacity = value
            if (animationsEnabled) {
                ValueAnimator.ofFloat(native.alpha, value.toFloat()).apply {
                    duration = theme.transitionDuration.inWholeMilliseconds
                    addUpdateListener {
                        native.alpha = animatedValue as Float
                    }
                }.start()
            } else {
                native.alpha = value.toFloat()
            }
        }

    override var exists: Boolean
        get() = super.exists
        set(value) {
            super.exists = value
            // Setting visibility to GONE does not work if an animation is running
            if (!exists) {
                native.clearAnimation()
            }
            native.visibility = if (value) {
                View.VISIBLE
            } else {
                View.GONE
            }
            (parent?.native as? DesiredSizeView)?.apply {
                visibility = native.visibility
            }
        }

    override var visible: Boolean
        get() = super.visible
        set(value) {
            super.visible = value
            native.visibility = if (value) {
                View.VISIBLE
            } else {
                View.INVISIBLE
            }
        }

    override var spacing: Dimension?
        get() = super.spacing
        set(value) {
            super.spacing = value
            for (child in children) child.updateCorners()
        }

    override var ignoreInteraction: Boolean
        get() = super.ignoreInteraction
        set(value) {
            super.ignoreInteraction = value
            native.isClickable = !value
            native.isFocusable = !value
        }

    override var paddingByEdge: Edges?
        get() = super.paddingByEdge
        set(value) {
            super.paddingByEdge = value
            for (child in children) child.updateCorners()
        }

    override var transitionId: String?
        get() = super.transitionId
        set(value) {
            super.transitionId = value
            native.transitionName = value
        }

    actual override fun scrollIntoView(horizontal: Align?, vertical: Align?, animate: Boolean) {
        generateSequence(native) {
            it.parent as? View
        }.firstOrNull() {
            when (it) {
                is HorizontalScrollView -> {
                    scrollToView(it, native, animate)
                    true
                }

                is ScrollView -> {
                    scrollToView(it, native, animate)
                    true
                }

                is NestedScrollView -> {
                    scrollToView(it, native, animate)
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

    actual override fun screenRectangle(): Rect? {
        val r = android.graphics.Rect()
        native.getGlobalVisibleRect(r)
        return Rect(
            left = r.left.toDouble(),
            top = r.top.toDouble(),
            right = r.right.toDouble(),
            bottom = r.bottom.toDouble(),
        )
    }

    protected var background: Drawable? = null
        set(value) {
            field = value
            native.background = value
        }
    protected var backgroundBlock: GradientDrawable? = null
    private val layoutChangeListener by lazy {
        { _: View?, _: Int, _: Int, _: Int, _: Int, _: Int, _: Int, _: Int, _: Int ->
            updateCorners()
        }
    }

    protected fun updateCorners() {
        val cr = when (val it = theme.cornerRadii) {
            is CornerRadii.ForceConstant -> it.value.value
            is CornerRadii.RatioOfSize -> if (it.ratio >= 0.5f) 9999f else it.ratio * min(native.width, native.height)
            is CornerRadii.Constant -> min(parentSpacing.value, it.value.value)
            is CornerRadii.RatioOfSpacing -> it.value * parentSpacing.value
            // TODO: Implement per-corner radii on Android
            is CornerRadii.PerCorner -> 0f
        }
        // Disabling because this is REALLY slow; we'll need to find a more optimized way to do corner radius based on
        // size on Android
        /*        if (theme.cornerRadii is CornerRadii.RatioOfSize) {
                    native.addOnLayoutChangeListener(layoutChangeListener)
                } else {
                    native.removeOnLayoutChangeListener(layoutChangeListener)
                }*/
        backgroundBlock?.cornerRadii = floatArrayOf(cr, cr, cr, cr, cr, cr, cr, cr)
//        native.elevation = native.elevation.coerceAtMost(parentSpacing)
    }

    actual override fun applyTheme(theme: ThemeAndBack) {
        val padding = paddingByEdge ?: when {
            !theme.padding -> null
            else -> theme.theme.padding
        }
        native.setPadding(
            padding?.left?.value?.toInt() ?: 0,
            padding?.top?.value?.toInt() ?: 0,
            padding?.right?.value?.toInt() ?: 0,
            padding?.bottom?.value?.toInt() ?: 0,
        )
        if (theme.drawBackground) {
            native.elevation = theme.theme.elevation.value
        } else {
            native.elevation = 0f
        }
        if (theme.drawBackground) {
            val backgroundDrawable = theme.theme.backgroundDrawableWithoutCorners(background as? GradientDrawable)
            backgroundBlock = backgroundDrawable
            updateCorners()
            background = backgroundDrawable
        } else {
            backgroundBlock = null
            background = null
        }
    }

    override fun postSetup() {
        super.postSetup()
        // Block touches below
        val wasClickable = native.isClickable
        val wasFocusable = native.isFocusable
        val hasInteractiveParent = generateSequence(this) { it.parent }.any { it.native.isClickable || it.native.isFocusable }
        if(!hasInteractiveParent && !wasClickable && !wasFocusable && !ignoreInteraction) {
            native.setOnClickListener {
                println("$this ($it) blocked the touch, because hasInteractiveParent = $hasInteractiveParent and wasClickable: ${wasClickable} and wasFocusable: ${wasFocusable}")
            }
        }
    }

    actual override fun internalAddChild(index: Int, view: RView) {
        (native as ViewGroup).addView(view.native, index)
        if ((native as ViewGroup).childCount != children.size) throw IllegalStateException("Native child count ${(native as ViewGroup).childCount} != RView count ${children.size} on ${this::class.qualifiedName}")
    }

    actual override fun internalRemoveChild(index: Int) {
        if ((native as ViewGroup).childCount != children.size) throw IllegalStateException("Native child count ${(native as ViewGroup).childCount} != RView count ${children.size} on ${this::class.qualifiedName}")
        (native as ViewGroup).removeViewAt(index)
    }

    actual override fun internalClearChildren() {
        if ((native as ViewGroup).childCount != children.size) throw IllegalStateException("Native child count ${(native as ViewGroup).childCount} != RView count ${children.size} on ${this::class.qualifiedName}")
        (native as ViewGroup).removeAllViews()
    }

    protected fun getBackgroundWithRipple(
        theme: Theme,
        fullyApply: Boolean,
        oldRippleDrawable: RippleDrawable?
    ): RippleDrawable {
        val rippleColor = ColorStateList.valueOf(theme[HoverSemantic].theme.background.colorInt())
        val backgroundDrawable = if (fullyApply) {
            theme.backgroundDrawableWithoutCorners(oldRippleDrawable?.getDrawable(0) as? GradientDrawable)
        } else {
            GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                colors = intArrayOf(
                    theme.background.applyAlpha(0.01f).colorInt(),
                    theme.background.applyAlpha(0.01f).colorInt()
                )
            }
        }
        backgroundBlock = backgroundDrawable
        return oldRippleDrawable?.apply {
            setColor(rippleColor)
            setDrawable(0, backgroundDrawable)
        } ?: RippleDrawable(rippleColor, backgroundDrawable, null)
    }

    protected fun applyThemeWithRipple(theme: ThemeAndBack) {
        val padding = paddingByEdge ?: when {
            !theme.padding -> null
            else -> theme.theme.padding
        }
        native.setPadding(
            padding?.left?.value?.toInt() ?: 0,
            padding?.top?.value?.toInt() ?: 0,
            padding?.right?.value?.toInt() ?: 0,
            padding?.bottom?.value?.toInt() ?: 0,
        )
        if (theme.drawBackground) {
            native.elevation = theme.theme.elevation.value
        } else {
            native.elevation = 0f
        }
        background = getBackgroundWithRipple(theme.theme, theme.drawBackground, background as? RippleDrawable)
        updateCorners()
    }

    protected fun applyThemeWithClipping(theme: ThemeAndBack) {
        val padding = paddingByEdge ?: when {
            !theme.padding -> null
            else -> theme.theme.padding
        }
        native.setPadding(
            padding?.left?.value?.toInt() ?: 0,
            padding?.top?.value?.toInt() ?: 0,
            padding?.right?.value?.toInt() ?: 0,
            padding?.bottom?.value?.toInt() ?: 0,
        )
        if (theme.drawBackground) {
            native.elevation = theme.theme.elevation.value
        } else {
            native.elevation = 0f
        }
        if (theme.drawBackground) {
            val backgroundDrawable = theme.theme.backgroundDrawableWithoutCorners(background as? GradientDrawable)
            backgroundBlock = backgroundDrawable
            updateCorners()
            background = backgroundDrawable
        } else {
            val backgroundDrawable = theme.theme.backgroundClippingDrawableWithoutCorners()
            backgroundBlock = backgroundDrawable
            updateCorners()
            background = backgroundDrawable
        }
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
