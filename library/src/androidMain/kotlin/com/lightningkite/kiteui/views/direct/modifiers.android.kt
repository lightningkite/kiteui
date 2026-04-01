package com.lightningkite.kiteui.views.direct

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
import androidx.core.view.children
import com.lightningkite.kiteui.Log

import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.beforeSetup
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.context.reactive
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*

@ViewModifierDsl3
actual fun ElementWriter.CanAddWeight.weight(amount: Float): ElementWriter.CanAddShownWhen {
    return beforeSetup {
        try {
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
}


@ViewModifierDsl3
actual fun ElementWriter.CanAddWeight.changingWeight(amount: ReactiveContext.() -> Float): ElementWriter.CanAddShownWhen {
    return beforeSetup {
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

        reactive {
            try {
                val lp = (lparams as SimplifiedLinearLayoutLayoutParams)
                val amount = amount()
                lp.weight = amount
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
}

@ViewModifierDsl3
actual fun ElementWriter.CanAddAlignment.align(horizontal: Align, vertical: Align): ElementWriter.CanAddWeight {
    return this@align.beforeSetup {
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

        params.width = when (horizontal) {
            Align.Stretch -> MATCH_PARENT
            else -> WRAP_CONTENT
        }
        params.height = when (vertical) {
            Align.Stretch -> MATCH_PARENT
            else -> WRAP_CONTENT
        }

        if (params is SimplifiedLinearLayoutLayoutParams)
            params.gravity = horizontalGravity or verticalGravity
        else if (params is FrameLayout.LayoutParams)
            params.gravity = horizontalGravity or verticalGravity
        else if (params is CoordinatorLayout.LayoutParams)
            params.gravity = horizontalGravity or verticalGravity
        else
            Log.warn("Unknown layout params kind ${params::class.qualifiedName}; I am ${this::class.qualifiedName}")
        if (horizontal == Align.Stretch && (parent?.native as? SimplifiedLinearLayout)?.orientation != SimplifiedLinearLayout.HORIZONTAL) {
            params.width = MATCH_PARENT
        } else if (params.width == MATCH_PARENT && (parent?.native as? SimplifiedLinearLayout)?.orientation == SimplifiedLinearLayout.HORIZONTAL) {
            // In a horizontal row, MATCH_PARENT width conflicts with weighted siblings - use WRAP_CONTENT instead
            params.width = WRAP_CONTENT
        }
        if (vertical == Align.Stretch && (parent?.native as? SimplifiedLinearLayout)?.orientation != SimplifiedLinearLayout.VERTICAL) {
            params.height = MATCH_PARENT
        } else if (params.height == MATCH_PARENT && (parent?.native as? SimplifiedLinearLayout)?.orientation == SimplifiedLinearLayout.VERTICAL) {
            // In a vertical col, MATCH_PARENT height conflicts with weighted siblings - use WRAP_CONTENT instead
            params.height = WRAP_CONTENT
        }
    }
}

@ViewModifierDsl3
actual inline fun ElementWriter.CanAddScrolling.__scrollsUncontracted(vertical: Boolean, horizontal: Boolean, crossinline setup: ScrollingBehaviors.() -> Unit): ElementWriter {
    return write(ScrollView(context, horizontal = horizontal, vertical = vertical), setup)
}

@ViewModifierDsl3
actual inline fun ElementWriter.CanAddScrolling.__scrollsWithRefreshUncontracted(
    vertical: Boolean,
    horizontal: Boolean,
    refreshAction: Action,
    crossinline setup: ScrollingBehaviors.() -> Unit
): ElementWriter {
    val scrollView = ScrollView(context, horizontal = horizontal, vertical = vertical).apply(setup)

    val view = if (vertical) {
        val refreshLayout = androidx.swiperefreshlayout.widget.SwipeRefreshLayout(context.activity)
        refreshLayout.setOnRefreshListener {
            refreshAction.startAction(this)
            reactive {
                refreshLayout.isRefreshing = refreshAction.state().handle(
                    success = { false },
                    exception = { false },
                    notReady = { true }
                )
            }
        }
        object : NativeContainerElement(context) {
            override val native: ViewGroup = refreshLayout

            val myChildren: ArrayList<View> = ArrayList()

            override fun nativeAddChild(index: Int, element: Element) {

                myChildren.add(index, element.native)
                native.addView(element.native, index)
            }

            override fun nativeRemoveChild(index: Int) {
                native.let {
                    it.removeViewAt(it.children.indexOf(myChildren.removeAt(index)))
                }
            }

            override fun nativeClearChildren() {
                native.let {
                    for (child in myChildren) {
                        it.removeViewAt(it.children.indexOf(child))
                    }
                    myChildren.clear()
                }
            }
        }
    } else {
        // For horizontal scrolling, just use regular scrolling as SwipeRefreshLayout only supports vertical
        scrollView
    }

    return write(view, {})
}

@ViewModifierDsl3
actual fun ElementWriter.CanAddSizing.sizedBox(constraints: SizeConstraints): ElementWriter.CanAddTheme {
    if (constraints.maxHeight != null || constraints.maxWidth != null || constraints.width != null || constraints.height != null || constraints.aspectRatio != null) {
        return write(object : NativeContainerElement(context) {
            override val native: ViewGroup = DesiredSizeView(context.activity).apply {
                this.constraints = constraints
            }
        })
    } else {
        return beforeSetup {
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
}

@ViewModifierDsl3
actual fun ElementWriter.CanAddSizing.changingSizeConstraints(constraints: ReactiveContext.() -> SizeConstraints): ElementWriter.CanAddTheme {
    return write(object : NativeContainerElement(context) {
        override val native: ViewGroup = DesiredSizeView(context.activity).apply {
            reactive {
                this@apply.constraints = constraints()
            }
        }
    })
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
        if (childCount == 0) return
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

    val Int.measureSpecString: String
        get() = when (measureSpecMode) {
            MeasureSpec.UNSPECIFIED -> "UNSPECIFIED $measureSpecSize"
            MeasureSpec.EXACTLY -> "EXACTLY $measureSpecSize"
            MeasureSpec.AT_MOST -> "AT_MOST $measureSpecSize"
            else -> "??? $measureSpecSize"
        }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (childCount == 0) return super.onMeasure(widthMeasureSpec, heightMeasureSpec)
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
actual fun ElementWriter.hintPopover(
    preferredDirection: PopoverPreferredDirection,
    setup: ViewWriter.() -> Unit,
): ElementWriter {
    return this@hintPopover.beforeSetup {
        native.setOnLongClickListener {
            // TODO: implement popover
            // toast(inner = setup)
            true
        }
    }
}

@ViewModifierDsl3
actual fun ElementWriter.textPopover(message: String): ElementWriter {
    return beforeSetup {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            native.tooltipText = message
        }
    }
}


@ViewModifierDsl3
actual fun ElementWriter.CanAddShownWhen.shownWhen(default: Boolean, condition: ReactiveContext.() -> Boolean): ElementWriter.CanAddSizing {
    return this@shownWhen.beforeSetup {
        shown = default
        var existingAnimator: ValueAnimator? = null
        var goal = default
        reactive {
            val value = condition()
            if (goal == value) return@reactive
            goal = value
            if (native.layoutParams == null) {
                shown = value
                return@reactive
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
