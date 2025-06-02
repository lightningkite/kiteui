package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.views.ViewWriter
import android.animation.Animator
import android.animation.ValueAnimator
import android.content.Context
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.animation.doOnEnd

import com.lightningkite.kiteui.ViewWrapper
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.navigation.dialogPageNavigator
import com.lightningkite.readable.CalculationContext
import com.lightningkite.readable.ReactiveContext
import com.lightningkite.readable.reactiveScope
import com.lightningkite.kiteui.views.*

@ViewModifierDsl3
actual fun ViewWriter.weight(amount: Float): ViewWrapper {
    beforeNextElementSetup {
        try {
            lastSetWeight = amount
            val lp = (lparams as SimplifiedLinearLayoutLayoutParams)
            lp.weight = amount
            if ((parent?.native as SimplifiedLinearLayout).orientation == SimplifiedLinearLayout.HORIZONTAL) {
                lp.width = 0
            } else {
                lp.height = 0
            }
        } catch (ex: Throwable) {
            RuntimeException("Weight is only available within a column or row, but the parent is a ${parent?.native?.let { it::class.simpleName }}").printStackTrace()
        }

    }
    return ViewWrapper
}


@ViewModifierDsl3
actual fun ViewWriter.changingWeight(amount: ReactiveContext.() -> Float): ViewWrapper {
    beforeNextElementSetup {
        val originalSize = try {
            val lp = (lparams as SimplifiedLinearLayoutLayoutParams)
            if ((parent?.native as SimplifiedLinearLayout).orientation == SimplifiedLinearLayout.HORIZONTAL) {
                lp.width
            } else {
                lp.height
            }
        } catch (ex: Throwable) {
            RuntimeException("Weight is only available within a column or row, but the parent is a ${parent?.native?.let { it::class.simpleName }}").printStackTrace()
            WRAP_CONTENT
        }

        reactiveScope {
            try {
                val lp = (lparams as SimplifiedLinearLayoutLayoutParams)
                val amount = amount()
                lp.weight = amount
                lastSetWeight = amount
                if ((parent?.native as SimplifiedLinearLayout).orientation == SimplifiedLinearLayout.HORIZONTAL) {
                    lp.width = if (lp.weight != 0f) 0 else originalSize
                } else {
                    lp.height = if (lp.weight != 0f) 0 else originalSize
                }
            } catch (ex: Throwable) {
                RuntimeException("Weight is only available within a column or row, but the parent is a ${parent?.native?.let { it::class.simpleName }}").printStackTrace()
            }
        }

    }
    return ViewWrapper
}

@ViewModifierDsl3
actual fun ViewWriter.align(horizontal: Align, vertical: Align): ViewWrapper {
    beforeNextElementSetup {
        lastSetHorizontalAlign = horizontal
        lastSetVerticalAlign = vertical
        val params = lparams
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
        else if (params is CoordinatorLayout.LayoutParams)
            params.gravity = horizontalGravity or verticalGravity
        else
            println("Unknown layout params kind ${params::class.qualifiedName}; I am ${this::class.qualifiedName}")
        if (horizontal == Align.Stretch && (parent?.native as? SimplifiedLinearLayout)?.orientation != SimplifiedLinearLayout.HORIZONTAL) {
            params.width = ViewGroup.LayoutParams.MATCH_PARENT
        } else if (params.width == ViewGroup.LayoutParams.MATCH_PARENT) {
            params.width = ViewGroup.LayoutParams.WRAP_CONTENT
        }
        if (vertical == Align.Stretch && (parent?.native as? SimplifiedLinearLayout)?.orientation != SimplifiedLinearLayout.VERTICAL) {
            params.height = ViewGroup.LayoutParams.MATCH_PARENT
        } else if (params.height == ViewGroup.LayoutParams.MATCH_PARENT) {
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT
        }
    }
    return ViewWrapper
}

@ViewModifierDsl3
actual inline fun ViewWriter.__scrollsUncontracted(vertical: Boolean, horizontal: Boolean, crossinline setup: ScrollingBehaviors.()->Unit): ViewWrapper {
    wrapNextIn(ScrollView(context, horizontal = horizontal, vertical = vertical).apply(setup))
    return ViewWrapper
}

@ViewModifierDsl3
actual fun ViewWriter.sizedBox(constraints: SizeConstraints): ViewWrapper {
    if (constraints.maxHeight != null || constraints.maxWidth != null || constraints.width != null || constraints.height != null || constraints.aspectRatio != null) {
        wrapNextIn(object : RViewWrapper(context) {
            override val native: View = DesiredSizeView(context.activity).apply {
                this.constraints = constraints
            }
        })
    } else {
        beforeNextElementSetup {
            constraints.width?.let { it: Dimension -> lparams.width = it.value.toInt() }
            constraints.height?.let { it: Dimension -> lparams.height = it.value.toInt() }
            constraints.maxWidth?.let { it: Dimension ->
                (lparams as? MaxSizeLayoutParams)?.maxWidth = it.value.toInt()
            }
            constraints.maxHeight?.let { it: Dimension ->
                (lparams as? MaxSizeLayoutParams)?.maxHeight = it.value.toInt()
            }
            constraints.minWidth?.let { native.minimumWidth = it.value.toInt() }
            constraints.minHeight?.let { native.minimumHeight = it.value.toInt() }
        }
    }
    return ViewWrapper
}

@ViewModifierDsl3
actual fun ViewWriter.changingSizeConstraints(constraints: ReactiveContext.() -> SizeConstraints): ViewWrapper {
    wrapNextIn(object : RViewWrapper(context) {
        override val native: View = DesiredSizeView(context.activity).apply {
            reactiveScope {
                this@apply.constraints = constraints()
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

//    private var clickListenerForChild: OnClickListener? = null
//    override fun setOnClickListener(l: OnClickListener?) {
//        clickListenerForChild = l
//        getChildAt(0)?.setOnClickListener(l)
//    }

    override fun onViewAdded(child: View?) {
        super.onViewAdded(child)

//        if (child != null && clickListenerForChild != null) {
//            child.setOnClickListener(clickListenerForChild)
//        }
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        if(childCount == 0) return
        getChildAt(0).measure(
            MeasureSpec.makeMeasureSpec(r - l - paddingLeft - paddingRight, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(b - t - paddingTop - paddingBottom, MeasureSpec.EXACTLY)
        )
        getChildAt(0).layout(paddingLeft, paddingTop, r - l - paddingRight, b - t - paddingBottom)
    }

    val Int.measureSpecMode get() = MeasureSpec.getMode(this)
    val Int.measureSpecSize get() = MeasureSpec.getSize(this)
    fun Int.measureSpecConstrainMax(value: Int): Int = MeasureSpec.makeMeasureSpec(
        if (measureSpecMode != MeasureSpec.UNSPECIFIED) measureSpecSize.coerceAtMost(value) else value,
        when (measureSpecMode) {
            MeasureSpec.UNSPECIFIED -> MeasureSpec.AT_MOST
            MeasureSpec.EXACTLY -> MeasureSpec.EXACTLY
            MeasureSpec.AT_MOST -> MeasureSpec.AT_MOST
            else -> 0
        }
    )

    fun Int.measureSpecConstrainSet(value: Int): Int = MeasureSpec.makeMeasureSpec(
        if (measureSpecMode == MeasureSpec.UNSPECIFIED) value
        else value.coerceAtMost(this.measureSpecSize),
//        value,
        MeasureSpec.EXACTLY
    )

    fun Int.measureSpecConstrain(min: Int?, max: Int?, set: Int?): Int {
        var out = this
        set?.let { out = out.measureSpecConstrainSet(it) }
        max?.let { out = out.measureSpecConstrainMax(it) }
        return out
    }

    infix fun Int.measureSpecPlus(value: Int): Int = MeasureSpec.makeMeasureSpec(
        MeasureSpec.getSize(this) + value,
        MeasureSpec.getMode(this)
    )

    val Int.measureSpecString: String get() = when(measureSpecMode) {
        MeasureSpec.UNSPECIFIED -> "UNSPECIFIED $measureSpecSize"
        MeasureSpec.EXACTLY -> "EXACTLY $measureSpecSize"
        MeasureSpec.AT_MOST -> "AT_MOST $measureSpecSize"
        else -> "??? $measureSpecSize"
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if(childCount == 0) return super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val f = getChildAt(0)
        f.minimumWidth = constraints.minWidth?.value?.toInt() ?: 0
        f.minimumHeight = constraints.minHeight?.value?.toInt() ?: 0
//        println("$f widthMeasureSpec: ${widthMeasureSpec.measureSpecString} / ${heightMeasureSpec.measureSpecString}")
        var widthMeasureSpec2 = widthMeasureSpec.measureSpecConstrain(
            min = constraints.minWidth?.value?.toInt(),
            max = constraints.maxWidth?.value?.toInt(),
            set = constraints.width?.value?.toInt()
        ).measureSpecPlus(-paddingLeft - paddingRight)
        var heightMeasureSpec2 = heightMeasureSpec.measureSpecConstrain(
            min = constraints.minHeight?.value?.toInt(),
            max = constraints.maxHeight?.value?.toInt(),
            set = constraints.height?.value?.toInt()
        ).measureSpecPlus(-paddingTop - paddingBottom)
//        println("$f widthMeasureSpec2: ${widthMeasureSpec2.measureSpecString} / ${heightMeasureSpec2.measureSpecString}")
        val xConstrained = widthMeasureSpec2.measureSpecMode == MeasureSpec.EXACTLY
        val yConstrained = heightMeasureSpec2.measureSpecMode == MeasureSpec.EXACTLY
        constraints.aspectRatio?.let { aspectRatio ->
            if (xConstrained && !yConstrained) {
                heightMeasureSpec2 = MeasureSpec.makeMeasureSpec(
                    (widthMeasureSpec2 / aspectRatio).toInt(),
                    MeasureSpec.EXACTLY,
                )
            } else if (!xConstrained && yConstrained) {
                widthMeasureSpec2 = MeasureSpec.makeMeasureSpec(
                    (heightMeasureSpec2 * aspectRatio).toInt(),
                    MeasureSpec.EXACTLY,
                )
            }
        }
//        println("$f widthMeasureSpec3: ${widthMeasureSpec2.measureSpecString} / ${heightMeasureSpec2.measureSpecString}")
        f.measure(
            widthMeasureSpec2,
            heightMeasureSpec2,
        )
//        println("$f inner: ${f.measuredWidth} / ${f.measuredHeight}")
        var mWidth = (f.measuredWidth + paddingLeft + paddingRight)
        var mHeight = (f.measuredHeight + paddingTop + paddingBottom)
//        println("$f mWidth: ${mWidth} / ${mHeight}")
        constraints.aspectRatio?.let { aspectRatio ->
            if (xConstrained && !yConstrained) {
                mHeight = (mWidth / aspectRatio).toInt()
            } else if (!xConstrained && yConstrained) {
                mHeight = (mHeight * aspectRatio).toInt()
            }
        }
//        println("$f mWidth2: ${mWidth} / ${mHeight}")
        setMeasuredDimension(
            mWidth,
            mHeight,
        )
    }

    init {
        clipChildren = false
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
            dialogPageNavigator.navigate(object : Page {
                override fun ViewWriter.render(): ViewModifiable = run {
                    return dismissBackground {
                        centered - frame {
                            setup(object : PopoverContext {
                                override val calculationContext: CalculationContext
                                    get() = this@beforeNextElementSetup

                                override fun close() {
                                    dialogPageNavigator.dismiss()
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            native.tooltipText = message
        }
    }
    return ViewWrapper
}


@ViewModifierDsl3
actual fun ViewWriter.shownWhen(default: Boolean, condition: ReactiveContext.() -> Boolean): ViewWrapper {
    beforeNextElementSetup {
//        exists = default
//        ::exists.invoke(condition)
//        (parent as? SimplifiedLinearLayout)?.let {
//            if(it.layoutTransition == null) {
//                it.layoutTransition = KiteUiLayoutTransition()
//            }
//        }

        shown = default
        var existingAnimator: ValueAnimator? = null
        var goal = default
        reactiveScope {
            val value = condition()
            if (goal == value) return@reactiveScope
            goal = value
            if (native.layoutParams == null) {
                shown = value
                return@reactiveScope
            }
            existingAnimator?.cancel()
            existingAnimator = null
            val parent = parent
            shown = true
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
                        shown = value
                    }
                    it.start()
                }
            } else {
                shown = value
            }
        }
    }
    return ViewWrapper
}

internal val animatingSize = HashSet<View>()

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
        if (!this@heightAnimator.isInLayout) {
            requestLayout()
        } else {
            println("Size animator blocked because we're in layout.")
        }
    }.apply {
        animatingSize.add(this@heightAnimator)
        addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {}
            override fun onAnimationCancel(animation: Animator) {
                animatingSize.remove(this@heightAnimator)
            }
            override fun onAnimationRepeat(animation: Animator) {}
            override fun onAnimationEnd(animation: Animator) {
                animatingSize.remove(this@heightAnimator)
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
        if (!this@widthAnimator.isInLayout) {
            requestLayout()
        } else {
            println("Size animator blocked because we're in layout.")
        }
        animatingSize.add(this@widthAnimator)
    }.apply {
        addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {}
            override fun onAnimationCancel(animation: Animator) {
                animatingSize.remove(this@widthAnimator)
            }
            override fun onAnimationRepeat(animation: Animator) {}
            override fun onAnimationEnd(animation: Animator) {
                layoutParams.width = toWidth
                animatingSize.remove(this@widthAnimator)
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
