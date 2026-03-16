package com.lightningkite.kiteui.views.direct

import android.animation.Animator
import android.animation.ValueAnimator
import android.content.Context
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.animation.doOnEnd
import androidx.core.view.ViewCompat
import androidx.core.view.children
import com.lightningkite.kiteui.Log

import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.navigation.dialogPageNavigator
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*

@ViewModifierDsl3
actual fun ViewWriter.weight(amount: Float): ViewWriter {
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
        .let { return it }
}


@ViewModifierDsl3
actual fun ViewWriter.changingWeight(amount: ReactiveContext.() -> Float): ViewWriter {
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
        .let { return it }
}

@ViewModifierDsl3
actual fun ViewWriter.align(horizontal: Align, vertical: Align): ViewWriter {
    beforeNextElementSetup {
        lastSetHorizontalAlign = horizontal
        lastSetVerticalAlign = vertical
        val params = lparams

        // Use parent's default alignment if not explicitly set (Align.Stretch means not set)
        val effectiveHorizontal = if (horizontal == Align.Stretch) {
            parent?.newChildHorizontalAlign ?: horizontal
        } else horizontal

        val effectiveVertical = if (vertical == Align.Stretch) {
            parent?.newChildVerticalAlign ?: vertical
        } else vertical

        val horizontalGravity = when (effectiveHorizontal) {
            Align.Start -> Gravity.START
            Align.Center -> Gravity.CENTER_HORIZONTAL
            Align.End -> Gravity.END
            else -> Gravity.CENTER_HORIZONTAL
        }
        val verticalGravity = when (effectiveVertical) {
            Align.Start -> Gravity.TOP
            Align.Center -> Gravity.CENTER_VERTICAL
            Align.End -> Gravity.BOTTOM
            else -> Gravity.CENTER_VERTICAL
        }


        params.width = when (horizontal) {
            Align.Stretch -> LayoutParams.MATCH_PARENT
            else -> LayoutParams.WRAP_CONTENT
        }
        params.height = when (vertical) {
            Align.Stretch -> LayoutParams.MATCH_PARENT
            else -> LayoutParams.WRAP_CONTENT
        }

        if (params is SimplifiedLinearLayoutLayoutParams)
            params.gravity = horizontalGravity or verticalGravity
        else if (params is FrameLayout.LayoutParams)
            params.gravity = horizontalGravity or verticalGravity
        else if (params is CoordinatorLayout.LayoutParams)
            params.gravity = horizontalGravity or verticalGravity
        else
            Log.warn("Unknown layout params kind ${params::class.qualifiedName}; I am ${this::class.qualifiedName}")
        if (effectiveHorizontal == Align.Stretch && (parent?.native as? SimplifiedLinearLayout)?.orientation != SimplifiedLinearLayout.HORIZONTAL) {
            params.width = ViewGroup.LayoutParams.MATCH_PARENT
        } else if (params.width == ViewGroup.LayoutParams.MATCH_PARENT && (parent?.native as? SimplifiedLinearLayout)?.orientation == SimplifiedLinearLayout.HORIZONTAL) {
            // In a horizontal row, MATCH_PARENT width conflicts with weighted siblings - use WRAP_CONTENT instead
            params.width = ViewGroup.LayoutParams.WRAP_CONTENT
        }
        if (effectiveVertical == Align.Stretch && (parent?.native as? SimplifiedLinearLayout)?.orientation != SimplifiedLinearLayout.VERTICAL) {
            params.height = ViewGroup.LayoutParams.MATCH_PARENT
        } else if (params.height == ViewGroup.LayoutParams.MATCH_PARENT && (parent?.native as? SimplifiedLinearLayout)?.orientation == SimplifiedLinearLayout.VERTICAL) {
            // In a vertical col, MATCH_PARENT height conflicts with weighted siblings - use WRAP_CONTENT instead
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT
        }
    }
        .let { return it }
}

@ViewModifierDsl3
actual inline fun ViewWriter.__scrollsUncontracted(vertical: Boolean, horizontal: Boolean, crossinline setup: ScrollingBehaviors.()->Unit): ViewWriter {
    return write(ScrollView(context, horizontal = horizontal, vertical = vertical), setup)
}

@ViewModifierDsl3
actual inline fun ViewWriter.__scrollsWithRefreshUncontracted(
    vertical: Boolean,
    horizontal: Boolean,
    refreshAction: Action,
    crossinline setup: ScrollingBehaviors.() -> Unit
): ViewWriter {
    val scrollView = ScrollView(context, horizontal = horizontal, vertical = vertical).apply(setup)

    val view = if (vertical) {
        val refreshLayout = androidx.swiperefreshlayout.widget.SwipeRefreshLayout(context.activity)
        refreshLayout.setOnRefreshListener {
            refreshAction.startAction(this)
            reactiveScope {
                refreshLayout.isRefreshing = refreshAction.state().handle(
                    success = { false },
                    exception = { false },
                    notReady = { true }
                )
            }
        }
        object: RViewWriter(context) {
            override val native: View = refreshLayout

            val myChildren: ArrayList<View> = ArrayList()
            override fun internalAddChild(index: Int, view: RView) {
                // Apply parent's default alignment if child doesn't have explicit alignment set
                var needsLayoutParamUpdate = false

                if (view.lastSetHorizontalAlign == com.lightningkite.kiteui.models.Align.Stretch && newChildHorizontalAlign != null) {
                    view.lastSetHorizontalAlign = newChildHorizontalAlign!!
                    needsLayoutParamUpdate = true
                }
                if (view.lastSetVerticalAlign == com.lightningkite.kiteui.models.Align.Stretch && newChildVerticalAlign != null) {
                    view.lastSetVerticalAlign = newChildVerticalAlign!!
                    needsLayoutParamUpdate = true
                }

                // If we applied defaults, update layout params (align() modifier wasn't called)
                if (needsLayoutParamUpdate) {
                    val params = view.lparams
                    val horizontalGravity = when (view.lastSetHorizontalAlign) {
                        com.lightningkite.kiteui.models.Align.Start -> android.view.Gravity.START
                        com.lightningkite.kiteui.models.Align.Center -> android.view.Gravity.CENTER_HORIZONTAL
                        com.lightningkite.kiteui.models.Align.End -> android.view.Gravity.END
                        else -> android.view.Gravity.CENTER_HORIZONTAL
                    }
                    val verticalGravity = when (view.lastSetVerticalAlign) {
                        com.lightningkite.kiteui.models.Align.Start -> android.view.Gravity.TOP
                        com.lightningkite.kiteui.models.Align.Center -> android.view.Gravity.CENTER_VERTICAL
                        com.lightningkite.kiteui.models.Align.End -> android.view.Gravity.BOTTOM
                        else -> android.view.Gravity.CENTER_VERTICAL
                    }

                    if (params is com.lightningkite.kiteui.views.direct.SimplifiedLinearLayoutLayoutParams)
                        params.gravity = horizontalGravity or verticalGravity
                    else if (params is android.widget.FrameLayout.LayoutParams)
                        params.gravity = horizontalGravity or verticalGravity
                    else if (params is androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams)
                        params.gravity = horizontalGravity or verticalGravity
                }

                myChildren.add(index, view.native)
                (native as ViewGroup).addView(view.native, index)
            }

            override fun internalRemoveChild(index: Int) {
                (native as ViewGroup).let {
                    it.removeViewAt(it.children.indexOf(myChildren.removeAt(index)))
                }
            }

            override fun internalClearChildren() {
                (native as ViewGroup).let {
                    for(child in myChildren) {
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
actual fun ViewWriter.sizedBox(constraints: SizeConstraints): ViewWriter {
    if (constraints.maxHeight != null || constraints.maxWidth != null || constraints.width != null || constraints.height != null || constraints.aspectRatio != null) {
        write(object : RViewWriter(context) {
            override val native: View = DesiredSizeView(context.activity).apply {
                this.constraints = constraints
            }
        }, {})
            .let { return it }
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
        .let { return it }
}

@ViewModifierDsl3
actual fun ViewWriter.changingSizeConstraints(constraints: ReactiveContext.() -> SizeConstraints): ViewWriter {
    write(object : RViewWriter(context) {
        override val native: View = DesiredSizeView(context.activity).apply {
            reactiveScope {
                this@apply.constraints = constraints()
            }
        }
    }, {})
        .let { return it }
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
        clipToPadding = false
    }

    override fun dispatchDraw(canvas: android.graphics.Canvas) {
        drawNeumorphicOuterShadows(canvas)
        super.dispatchDraw(canvas)
    }

    private fun drawNeumorphicOuterShadows(canvas: android.graphics.Canvas) {
        for (i in 0 until childCount) {
            val child = getChildAt(i) ?: continue
            if (child.visibility == GONE) continue
            val bg = child.background as? com.lightningkite.kiteui.views.NeumorphicDrawable ?: continue
            bg.drawOuterShadowsFromParent(canvas, child.left, child.top)
        }
    }
}

@ViewModifierDsl3
actual fun ViewWriter.hintPopover(
    preferredDirection: PopoverPreferredDirection,
    setup: ViewWriter.() -> Unit,
): ViewWriter {
    beforeNextElementSetup {
        native.setOnLongClickListener {
            // TODO
//            toast(inner = setup)
            true
        }
    }
        .let { return it }
}

@ViewModifierDsl3
actual fun ViewWriter.hasPopover(
    requiresClick: Boolean,
    preferredDirection: PopoverPreferredDirection,
    setup: ViewWriter.(popoverContext: PopoverContext) -> Unit,
): ViewWriter {
    beforeNextElementSetup {
        native.setOnClickListener {
            dialogPageNavigator.navigate(object : Page {
                override fun ViewWriter.render(): Unit = run {
                    dismissBackground {
                        centered.frame {
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
        .let { return it }
}

@ViewModifierDsl3
actual fun ViewWriter.textPopover(message: String): ViewWriter {
    beforeNextElementSetup {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            native.tooltipText = message
        }
    }
        .let { return it }
}


@ViewModifierDsl3
actual fun ViewWriter.shownWhen(default: Boolean, condition: ReactiveContext.() -> Boolean): ViewWriter {
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
        .let { return it }
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
