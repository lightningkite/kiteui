package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.views.ViewWriter
import android.animation.Animator
import android.animation.ValueAnimator
import android.content.Context
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import androidx.core.animation.doOnEnd
import androidx.core.view.ViewCompat

import androidx.core.widget.NestedScrollView
import com.lightningkite.kiteui.ViewWrapper
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.navigation.Screen
import com.lightningkite.kiteui.reactive.CalculationContext
import com.lightningkite.kiteui.reactive.reactiveScope
import com.lightningkite.kiteui.utils.fitInsideBox
import com.lightningkite.kiteui.views.*

@ViewModifierDsl3
actual fun ViewWriter.weight(amount: Float): ViewWrapper {
    beforeNextElementSetup {
        try {
            val lp = (native.lparams as SimplifiedLinearLayoutLayoutParams)
            lp.weight = amount
            if ((native.parent as SimplifiedLinearLayout).orientation == SimplifiedLinearLayout.HORIZONTAL) {
                lp.width = 0
            } else {
                lp.height = 0
            }
        } catch (ex: Throwable) {
            RuntimeException("Weight is only available within a column or row, but the parent is a ${native.parent?.let { it::class.simpleName }}").printStackTrace()
        }

    }
    return ViewWrapper
}


@ViewModifierDsl3
actual fun ViewWriter.changingWeight(amount: suspend () -> Float): ViewWrapper {
    afterNextElementSetup {
        val originalSize = try {
            val lp = (native.lparams as SimplifiedLinearLayoutLayoutParams)
            if ((native.parent as SimplifiedLinearLayout).orientation == SimplifiedLinearLayout.HORIZONTAL) {
                lp.width
            } else {
                lp.height
            }
        } catch (ex: Throwable) {
            RuntimeException("Weight is only available within a column or row, but the parent is a ${native.parent?.let { it::class.simpleName }}").printStackTrace()
            WRAP_CONTENT
        }

        reactiveScope {
            try {
                val lp = (native.lparams as SimplifiedLinearLayoutLayoutParams)
                lp.weight = amount()
                if ((native.parent as SimplifiedLinearLayout).orientation == SimplifiedLinearLayout.HORIZONTAL) {
                    lp.width = if (lp.weight != 0f) 0 else originalSize
                } else {
                    lp.height = if (lp.weight != 0f) 0 else originalSize
                }
            } catch (ex: Throwable) {
                RuntimeException("Weight is only available within a column or row, but the parent is a ${native.parent?.let { it::class.simpleName }}").printStackTrace()
            }
        }

    }
    return ViewWrapper
}

@ViewModifierDsl3
actual fun ViewWriter.gravity(horizontal: Align, vertical: Align): ViewWrapper {
    afterNextElementSetup {
        val params = native.lparams
        val horizontalGravity = when (horizontal) {
            Align.Start -> Gravity.START
            Align.Center -> Gravity.CENTER_HORIZONTAL
            Align.End -> Gravity.END
            else -> Gravity.CENTER_HORIZONTAL
        }
        val verticalGravity = when (vertical) {
            Align.Start -> Gravity.TOP
            Align.Center -> Gravity.CENTER_VERTICAL
            Align.End -> Gravity.BOTTOM
            else -> Gravity.CENTER_VERTICAL
        }
        if (params is SimplifiedLinearLayoutLayoutParams)
            params.gravity = horizontalGravity or verticalGravity
        else if (params is FrameLayout.LayoutParams)
            params.gravity = horizontalGravity or verticalGravity
        else
            println("Unknown layout params kind ${params::class.qualifiedName}; I am ${this::class.qualifiedName}")
        if (horizontal == Align.Stretch && (this.parent as? SimplifiedLinearLayout)?.orientation != SimplifiedLinearLayout.HORIZONTAL) {
            params.width = ViewGroup.LayoutParams.MATCH_PARENT
        } else if (params.width == ViewGroup.LayoutParams.MATCH_PARENT) {
            params.width = ViewGroup.LayoutParams.WRAP_CONTENT
        }
        if (vertical == Align.Stretch && (this.parent as? SimplifiedLinearLayout)?.orientation != SimplifiedLinearLayout.VERTICAL) {
            params.height = ViewGroup.LayoutParams.MATCH_PARENT
        } else if (params.height == ViewGroup.LayoutParams.MATCH_PARENT) {
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT
        }
    }
    return ViewWrapper
}

@ViewModifierDsl3
actual val ViewWriter.scrolls: ViewWrapper
    get() {
        wrapNextIn(object : RView(context) {
            override val native: View = NestedScrollView(context.activity).apply {
                isFillViewport = true
            }

            override fun applyForeground(theme: Theme) { /*Do nothing*/
            }

            override fun applyBackground(theme: Theme, fullyApply: Boolean) { /*Do nothing*/
            }
        })
        return ViewWrapper
    }

@ViewModifierDsl3
actual val ViewWriter.scrollsHorizontally: ViewWrapper
    get() {
        wrapNextIn(object : RView(context) {
            override val native: View = HorizontalScrollView(context.activity).apply {
                isFillViewport = true
            }

            override fun applyForeground(theme: Theme) { /*Do nothing*/
            }

            override fun applyBackground(theme: Theme, fullyApply: Boolean) { /*Do nothing*/
            }
        })
        return ViewWrapper
    }

@ViewModifierDsl3
actual fun ViewWriter.sizedBox(constraints: SizeConstraints): ViewWrapper {
    if (constraints.maxHeight != null || constraints.maxWidth != null || constraints.width != null || constraints.height != null || constraints.aspectRatio != null) {
        wrapNextIn(object : RView(context) {
            override val native: View = DesiredSizeView(context.activity).apply {
                this.constraints = constraints
            }
        })
    } else {
        beforeNextElementSetup {
            constraints.width?.let { it: Dimension -> native.lparams.width = it.value.toInt() }
            constraints.height?.let { it: Dimension -> native.lparams.height = it.value.toInt() }
            constraints.maxWidth?.let { it: Dimension ->
                (native.lparams as? MaxSizeLayoutParams)?.maxWidth = it.value.toInt()
            }
            constraints.maxHeight?.let { it: Dimension ->
                (native.lparams as? MaxSizeLayoutParams)?.maxHeight = it.value.toInt()
            }
            constraints.minWidth?.let { native.minimumWidth = it.value.toInt() }
            constraints.minHeight?.let { native.minimumHeight = it.value.toInt() }
        }
    }
    return ViewWrapper
}

@ViewModifierDsl3
actual fun ViewWriter.changingSizeConstraints(constraints: suspend () -> SizeConstraints): ViewWrapper {
    wrapNextIn(object : RView(context) {
        override val native: View = DesiredSizeView(context.activity).apply {
            reactiveScope {
                this.constraints = constraints()
            }
        }
    })
    return ViewWrapper
}

interface MaxSizeLayoutParams {
    var maxWidth: Int
    var maxHeight: Int
}

class DesiredSizeView(context: Context) : ViewGroup(context) {
    var constraints: SizeConstraints = SizeConstraints()
        set(value) {
            field = value
            requestLayout()
        }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        getChildAt(0).measure(
            MeasureSpec.makeMeasureSpec(r - l, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(b - t, MeasureSpec.EXACTLY)
        )
        getChildAt(0).layout(0, 0, r - l, b - t)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val f = getChildAt(0)
        fun preprocess(baseSpec: Int, min: Int?, max: Int?, set: Int?): Int {
            var out = baseSpec
            when (MeasureSpec.getMode(baseSpec)) {
                MeasureSpec.UNSPECIFIED -> {
                    max?.let {
                        out = MeasureSpec.makeMeasureSpec(it, MeasureSpec.AT_MOST)
                    }
                    set?.let {
                        out = MeasureSpec.makeMeasureSpec(it, MeasureSpec.AT_MOST)
                    }
                }

                MeasureSpec.EXACTLY -> {
                    val value = MeasureSpec.getSize(baseSpec)
                    out = MeasureSpec.makeMeasureSpec(
                        value.let {
                            set?.let { limit ->
                                it.coerceAtMost(limit)
                            } ?: it
                        }.let {
                            max?.let { limit ->
                                it.coerceAtMost(limit)
                            } ?: it
                        },
                        MeasureSpec.EXACTLY
                    )
                }

                MeasureSpec.AT_MOST -> {
                    val value = MeasureSpec.getSize(baseSpec)
                    out = MeasureSpec.makeMeasureSpec(
                        value.let {
                            set?.let { limit ->
                                it.coerceAtMost(limit)
                            } ?: it
                        }.let {
                            max?.let { limit ->
                                it.coerceAtMost(limit)
                            } ?: it
                        },
                        MeasureSpec.AT_MOST
                    )
                }
            }
            return out
        }
        f.measure(
            preprocess(
                widthMeasureSpec,
                constraints.minWidth?.value?.toInt(),
                constraints.maxWidth?.value?.toInt(),
                constraints.width?.value?.toInt()
            ),
            preprocess(
                heightMeasureSpec,
                constraints.minHeight?.value?.toInt(),
                constraints.maxHeight?.value?.toInt(),
                constraints.height?.value?.toInt()
            )
        )
        fun postprocess(originalSpec: Int, baseSpec: Int, min: Int?, max: Int?, set: Int?): Int {
            val measuredSize = MeasureSpec.getSize(baseSpec)
            val outerRulesSize = MeasureSpec.getSize(originalSpec)

            var outMode = MeasureSpec.getMode(baseSpec)
            var outSize = measuredSize

            set?.let {
                outSize = outSize.coerceAtLeast(it)
            }

            when (MeasureSpec.getMode(originalSpec)) {
                MeasureSpec.EXACTLY -> {
                    outMode = MeasureSpec.EXACTLY
                    outSize = outSize.coerceAtMost(outerRulesSize)
                }

                MeasureSpec.AT_MOST -> {
                    if (outMode == MeasureSpec.EXACTLY) {
                        outSize = outSize.coerceAtMost(outerRulesSize)
                    } else {
                        outMode = MeasureSpec.AT_MOST
                        outSize = outSize.coerceAtMost(outerRulesSize)
                    }
                }
            }

            min?.let {
                outSize = outSize.coerceAtLeast(it)
            }
            max?.let {
                outSize = outSize.coerceAtMost(it)
                if (outMode == MeasureSpec.UNSPECIFIED) {
                    outMode = MeasureSpec.AT_MOST
                }
            }
            return MeasureSpec.makeMeasureSpec(outSize, outMode)
        }

        var processedWidthSpec = postprocess(
            widthMeasureSpec,
            f.measuredWidth,
            constraints.minWidth?.value?.toInt(),
            constraints.maxWidth?.value?.toInt(),
            constraints.width?.value?.toInt()
        )
        var processedHeightSpec = postprocess(
            heightMeasureSpec,
            f.measuredHeight,
            constraints.minHeight?.value?.toInt(),
            constraints.maxHeight?.value?.toInt(),
            constraints.height?.value?.toInt()
        )

        constraints.aspectRatio?.let { aspectRatio ->
            aspectRatio.fitInsideBox(
                MeasureSpec.getSize(processedWidthSpec).toDouble(),
                MeasureSpec.getSize(processedHeightSpec).toDouble(),
            ).let { innerBox ->
                processedWidthSpec = MeasureSpec.makeMeasureSpec(innerBox.first.toInt(), MeasureSpec.EXACTLY)
                processedHeightSpec = MeasureSpec.makeMeasureSpec(innerBox.second.toInt(), MeasureSpec.EXACTLY)
            }
        }

        setMeasuredDimension(processedWidthSpec, processedHeightSpec)
    }
}

@ViewModifierDsl3
actual fun ViewWriter.hintPopover(
    preferredDirection: PopoverPreferredDirection,
    setup: ViewWriter.() -> Unit,
): ViewWrapper {
    beforeNextElementSetup {
        native.setOnLongClickListener {
            // TODO
//            toast(inner = setup)
            true
        }
    }
    return ViewWrapper
}

@ViewModifierDsl3
actual fun ViewWriter.hasPopover(
    requiresClick: Boolean,
    preferredDirection: PopoverPreferredDirection,
    setup: ViewWriter.(popoverContext: PopoverContext) -> Unit,
): ViewWrapper {
    beforeNextElementSetup {
        native.setOnClickListener {
            navigator.dialog.navigate(object : Screen {
                override fun ViewWriter.render() {
                    dismissBackground {
                        centered - stack {
                            setup(object : PopoverContext {
                                override val calculationContext: CalculationContext
                                    get() = this@beforeNextElementSetup

                                override fun close() {
                                    navigator.dialog.dismiss()
                                }
                            })
                        }
                    }
                }
            })
        }
    }
    return ViewWrapper
}

@ViewModifierDsl3
actual fun ViewWriter.textPopover(message: String): ViewWrapper {
    beforeNextElementSetup {
        native.tooltipText = message
    }
    return ViewWrapper
}


@ViewModifierDsl3
actual fun ViewWriter.onlyWhen(default: Boolean, condition: suspend () -> Boolean): ViewWrapper {
    afterNextElementSetup {
//        exists = default
//        ::exists.invoke(condition)
//        (parent as? SimplifiedLinearLayout)?.let {
//            if(it.layoutTransition == null) {
//                it.layoutTransition = KiteUiLayoutTransition()
//            }
//        }

        exists = default
        var existingAnimator: ValueAnimator? = null
        var goal = default
        reactiveScope {
            val value = condition()
            if (goal == value) return@reactiveScope
            goal = value
            existingAnimator?.cancel()
            existingAnimator = null
            val parent = parent
            exists = true
            val p = parent?.native
            if (animationsEnabled) {
                existingAnimator = if (value) {
                    if (p is SimplifiedLinearLayout) {
                        if (p.orientation == SimplifiedLinearLayout.HORIZONTAL) {
                            native.widthAnimator(WRAP_CONTENT)
                        } else {
                            native.heightAnimator(WRAP_CONTENT)
                        }.also {
                            it.addUpdateListener {
                                (native.layoutParams as? SimplifiedLinearLayoutLayoutParams)?.gapRatio =
                                    it.animatedFraction
                            }
                        }
                    } else {
                        TypedValueAnimator.FloatAnimator(0f, 1f).onUpdate { native.alpha = it }
                    }
                } else {
                    if (p is SimplifiedLinearLayout) {
                        if (p.orientation == SimplifiedLinearLayout.HORIZONTAL) {
                            native.widthAnimator(0)
                        } else {
                            native.heightAnimator(0)
                        }.also {
                            it.addUpdateListener {
                                (native.layoutParams as? SimplifiedLinearLayoutLayoutParams)?.gapRatio =
                                    1f - it.animatedFraction
                            }
                        }
                    } else {
                        TypedValueAnimator.FloatAnimator(1f, 0f).onUpdate { native.alpha = it }
                    }
                }.setDuration(theme.transitionDuration.inWholeMilliseconds).also {
                    it.doOnEnd {
                        exists = value
                    }
                    it.start()
                }
            } else {
                exists = value
            }
        }
    }
    return ViewWrapper
}

/**
 * Creates an animator that will animate from the current height to a new height.
 */
private fun View.heightAnimator(toHeight: Int): TypedValueAnimator.IntAnimator {
    val currentHeight = layoutParams.height.let {
        when (it) {
            WRAP_CONTENT, MATCH_PARENT -> height
            else -> it
        }
    }
    val fixedToHeight = when (toHeight) {
        WRAP_CONTENT -> {
            measure(
                View.MeasureSpec.makeMeasureSpec(
                    (parent as? View)?.width
                        ?: (Int.MAX_VALUE / 2 - 1), View.MeasureSpec.AT_MOST
                ),
                View.MeasureSpec.makeMeasureSpec(Int.MAX_VALUE / 2 - 1, View.MeasureSpec.AT_MOST)
            )
            measuredHeight
        }

        else -> toHeight
    }
    return TypedValueAnimator.IntAnimator(currentHeight, fixedToHeight).onUpdate {
        layoutParams.height = it
        if (!ViewCompat.isInLayout(this@heightAnimator)) requestLayout()
    }.apply {
        addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {}
            override fun onAnimationCancel(animation: Animator) {}
            override fun onAnimationRepeat(animation: Animator) {}
            override fun onAnimationEnd(animation: Animator) {
                println("ENDING, set to $toHeight")
                layoutParams.height = toHeight
            }
        })
    }
}

/**
 * Creates an animator that will animate from the current width to a new width.
 */
private fun View.widthAnimator(toWidth: Int): TypedValueAnimator.IntAnimator {
    val currentWidth = layoutParams.width.let {
        when (it) {
            WRAP_CONTENT, MATCH_PARENT -> width
            else -> it
        }
    }
    val fixedToWidth = when (toWidth) {
        WRAP_CONTENT -> {
            measure(
                View.MeasureSpec.makeMeasureSpec(Int.MAX_VALUE / 2 - 1, View.MeasureSpec.AT_MOST),
                View.MeasureSpec.makeMeasureSpec(
                    (parent as? View)?.height
                        ?: (Int.MAX_VALUE / 2 - 1), View.MeasureSpec.AT_MOST
                ),
            )
            measuredWidth
        }

        else -> toWidth
    }
    return TypedValueAnimator.IntAnimator(currentWidth, fixedToWidth).onUpdate {
        layoutParams.width = it
        println("SETTING WIDTH TO $it")
        if (!ViewCompat.isInLayout(this@widthAnimator)) requestLayout()
    }.apply {
        addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {}
            override fun onAnimationCancel(animation: Animator) {}
            override fun onAnimationRepeat(animation: Animator) {}
            override fun onAnimationEnd(animation: Animator) {
                println("ENDING, set to $toWidth")
                layoutParams.width = toWidth
            }
        })
    }
}

internal object TypedValueAnimator {

    /**
     * A value animator that animates between [Int] values.
     */
    class IntAnimator(vararg values: Int) : ValueAnimator() {
        init {
            setIntValues(*values)
        }

        fun onUpdate(lambda: IntAnimator.(Int) -> Unit): IntAnimator {
            addUpdateListener {
                lambda(animatedValue as Int)
            }
            return this
        }
    }

    /**
     * A value animator that animates between [Float] values.
     */
    class FloatAnimator(vararg values: Float) : ValueAnimator() {
        init {
            setFloatValues(*values)
        }

        fun onUpdate(lambda: FloatAnimator.(Float) -> Unit): FloatAnimator {
            addUpdateListener {
                lambda(animatedValue as Float)
            }
            return this
        }
    }
}
