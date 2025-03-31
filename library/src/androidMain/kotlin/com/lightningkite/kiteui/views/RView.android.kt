package com.lightningkite.kiteui.views

import android.animation.ValueAnimator
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ScrollView
import androidx.core.view.OnApplyWindowInsetsListener
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.NestedScrollView
import com.lightningkite.kiteui.afterTimeout
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.views.direct.DesiredSizeView
import com.lightningkite.kiteui.views.direct.colorInt
import com.lightningkite.readable.onRemove
import kotlin.math.min

actual abstract class RView actual constructor(context: RContext) : RViewHelper(context) {
    abstract val native: View
    open fun childTouches(child: RView): Int = Gravity.LEFT or Gravity.TOP or Gravity.RIGHT or Gravity.BOTTOM

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

    protected fun updateCorners() {
        val cr = when (val it = theme.cornerRadii) {
            is CornerRadii.ForceConstant -> it.value.value
            is CornerRadii.RatioOfSize -> if (it.ratio >= 0.5f) 9999f else it.ratio * min(native.width, native.height)
            is CornerRadii.Constant -> min(parentSpacing.value, it.value.value)
            is CornerRadii.RatioOfSpacing -> it.value * parentSpacing.value
            is CornerRadii.PerCorner -> it.value.value
        }

        val asPerCorner = theme.cornerRadii as? CornerRadii.PerCorner
        val topLeft = if (asPerCorner?.topLeft != false) cr else 0f
        val topRight = if (asPerCorner?.topRight != false) cr else 0f
        val bottomRight = if (asPerCorner?.bottomRight != false) cr else 0f
        val bottomLeft = if (asPerCorner?.bottomLeft != false) cr else 0f
        backgroundBlock?.cornerRadii =
            floatArrayOf(topLeft, topLeft, topRight, topRight, bottomRight, bottomRight, bottomLeft, bottomLeft)
    }

    private var edgeToEdgePadding: Edges? = null
        set(value) {
            if(field != value) {
                field = value
                updatePadding()
                children.forEach {
                    if(value != null) {
                        fun walkdown(it: RView) {
                            if(it.edgeToEdgePadding != null) {
                                it.edgeToEdgePadding = null
                            }
                            it.children.forEach { walkdown(it) }
                        }
                        walkdown(it)
                    }
                    ViewCompat.requestApplyInsets(it.native)
                }
            }
        }

    protected fun updatePadding() {
        val padding = (paddingByEdge ?: when {
            !themeAndBack.padding -> null
            else -> themeAndBack.theme.padding
        })?.let {
            edgeToEdgePadding?.let { e -> it + e } ?: it
        }
        native.setPadding(
            padding?.left?.value?.toInt() ?: 0,
            padding?.top?.value?.toInt() ?: 0,
            padding?.right?.value?.toInt() ?: 0,
            padding?.bottom?.value?.toInt() ?: 0,
        )
    }

    actual override fun applyTheme(theme: ThemeAndBack) {
        ViewCompat.requestApplyInsets(native)
//        ViewCompat.dispatchApplyWindowInsets(native, ViewCompat.computeSystemWindowInsets())
        if (theme.drawBackground) {
            native.elevation = theme.theme.elevation.value
        } else {
            native.elevation = 0f
        }
        updatePadding()
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
        val hasInteractiveParent =
            generateSequence(this) { it.parent }.any { it.native.isClickable || it.native.isFocusable }
//        val previousTrace =
//            generateSequence(this) { it.parent }.map { "  ${it} - ${it.native}, clickable: ${it.native.isClickable}, focusable: ${it.native.isFocusable}" }
//                .toList()

        if (!hasInteractiveParent && !wasClickable && !wasFocusable && !ignoreInteraction) {
            native.setOnClickListener {
                println("$this ($it) blocked the touch, because hasInteractiveParent = $hasInteractiveParent and wasClickable: ${wasClickable} and wasFocusable: ${wasFocusable}")
            }
        }

        if (!cannotBeCovered) {
            val l = OnApplyWindowInsetsListener { v: View, insetsGetter: WindowInsetsCompat ->
                if (insetsGetter === WindowInsetsCompat.CONSUMED) {
                    edgeToEdgePadding = null
                    return@OnApplyWindowInsetsListener insetsGetter
                }
                val padding = paddingByEdge ?: when {
                    !themeAndBack.padding -> Edges.ZERO
                    else -> themeAndBack.theme.padding
                }
                val insets = insetsGetter.getInsets(WindowInsetsCompat.Type.systemBars())
                fun shouldApply(direction: Int, alreadyHasPadding: Boolean): Boolean {
                    return generateSequence(this) { it.parent }
                        .zipWithNext()
                        .all { (child, parent) -> parent.childTouches(child) and direction == direction }
                        .and(
                            alreadyHasPadding ||
                                    (children.asSequence()
                                        .any { childTouches(it) and direction == direction && it.cannotBeCovered })
                        )
                }

                val shouldApplyLeft = shouldApply(Gravity.LEFT, padding.left.value > 0)
                val shouldApplyTop = shouldApply(Gravity.TOP, padding.top.value > 0)
                val shouldApplyRight = shouldApply(Gravity.RIGHT, padding.right.value > 0)
                val shouldApplyBottom = shouldApply(Gravity.BOTTOM, padding.bottom.value > 0)
                val shouldApplyAny = shouldApplyLeft || shouldApplyTop || shouldApplyRight || shouldApplyBottom
                if (!shouldApplyAny) {
                    edgeToEdgePadding = null
                    return@OnApplyWindowInsetsListener insetsGetter
                }
                edgeToEdgePadding = Edges(
                    left = if (shouldApplyLeft) insets.left.px else 0.px,
                    top = if (shouldApplyTop) insets.top.px else 0.px,
                    right = if (shouldApplyRight) insets.right.px else 0.px,
                    bottom = if (shouldApplyBottom) insets.bottom.px else 0.px,
                )
                WindowInsetsCompat.CONSUMED
            }
            ViewCompat.setOnApplyWindowInsetsListener(native, l)
            onRemove { ViewCompat.setOnApplyWindowInsetsListener(native, null) }
        }
    }

    actual override fun internalAddChild(index: Int, view: RView) {
        (native as ViewGroup).addView(view.native, index)
        if(fullyStarted) ViewCompat.requestApplyInsets(view.native)
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
        updatePadding()
        if (theme.drawBackground) {
            native.elevation = theme.theme.elevation.value
        } else {
            native.elevation = 0f
        }
        background = getBackgroundWithRipple(theme.theme, theme.drawBackground, background as? RippleDrawable)
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
