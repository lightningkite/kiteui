package com.lightningkite.kiteui.views.direct

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewDebug
import android.view.ViewGroup
import android.widget.LinearLayout


typealias SimplifiedLinearLayoutLayoutParams = SimplifiedLinearLayout.LayoutParams
/**
 * A drastically simplified variant of LinearLayout.
 */
open class SimplifiedLinearLayout(context: Context?, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) :
    ViewGroup(context, attrs, defStyleAttr, defStyleRes) {

    var isBaselineAligned = true
    private var mBaselineAlignedChildIndex = -1
    private var mBaselineChildTop = 0
    private var mOrientation = 0
    private var mGravity = Gravity.START or Gravity.TOP
    private var mTotalLength = 0
    var isMeasureWithLargestChildEnabled: Boolean = false
    private var mMaxAscent: IntArray = IntArray(4)
    private var mMaxDescent: IntArray = IntArray(4)

    private var mLayoutDirection: Int = -1

    @JvmOverloads
    constructor(context: Context?, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : this(
        context,
        attrs,
        defStyleAttr,
        0
    )

    override fun shouldDelayChildPressedState(): Boolean {
        return false
    }
    
    var gap: Int = 0
        set(value) {
            field = value
            requestLayout()
        }


    override fun getBaseline(): Int {
        if (mBaselineAlignedChildIndex < 0) {
            return super.getBaseline()
        }
        if (childCount <= mBaselineAlignedChildIndex) {
            throw RuntimeException(
                "mBaselineAlignedChildIndex of LinearLayout "
                        + "set to an index that is out of bounds."
            )
        }
        val child = getChildAt(mBaselineAlignedChildIndex)
        val childBaseline = child.baseline
        if (childBaseline == -1) {
            if (mBaselineAlignedChildIndex == 0) {
                // this is just the default case, safe to return -1
                return -1
            }
            // the user picked an index that points to something that doesn't
            // know how to calculate its baseline.
            throw RuntimeException(
                ("mBaselineAlignedChildIndex of LinearLayout "
                        + "points to a View that doesn't know how to get its baseline.")
            )
        }

        // TODO: This should try to take into account the virtual offsets
        // (See getNextLocationOffset and getLocationOffset)
        // We should add to childTop:
        // sum([getNextLocationOffset(getChildAt(i)) / i < mBaselineAlignedChildIndex])
        // and also add:
        // getLocationOffset(child)
        var childTop = mBaselineChildTop
        if (mOrientation == VERTICAL) {
            val majorGravity = mGravity and Gravity.VERTICAL_GRAVITY_MASK
            if (majorGravity != Gravity.TOP) {
                when (majorGravity) {
                    Gravity.BOTTOM -> childTop = bottom - top - paddingBottom - mTotalLength
                    Gravity.CENTER_VERTICAL -> childTop += ((bottom - top - paddingTop - paddingBottom) -
                            mTotalLength) / 2
                }
            }
        }
        val lp = child.layoutParams as LayoutParams
        return childTop + /*lp.topMargin +*/ childBaseline
    }

    var baselineAlignedChildIndex: Int
        /**
         * @return The index of the child that will be used if this layout is
         * part of a larger layout that is baseline aligned, or -1 if none has
         * been set.
         */
        get() = mBaselineAlignedChildIndex
        /**
         * @param i The index of the child that will be used if this layout is
         * part of a larger layout that is baseline aligned.
         *
         * @attr ref android.R.styleable#LinearLayout_baselineAlignedChildIndex
         */
        set(i) {
            if ((i < 0) || (i >= childCount)) {
                throw IllegalArgumentException(
                    ("base aligned child index out "
                            + "of range (0, " + childCount + ")")
                )
            }
            mBaselineAlignedChildIndex = i
        }
    
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (mOrientation == VERTICAL) {
            measureVertical(widthMeasureSpec, heightMeasureSpec)
        } else {
            measureHorizontal(widthMeasureSpec, heightMeasureSpec)
        }
    }

    /**
     * Checks whether all (virtual) child views before the given index are gone.
     */
    private fun allViewsAreGoneBefore(childIndex: Int): Boolean {
        for (i in childIndex - 1 downTo 0) {
            val child = getChildAt(i)
            if (child != null && child.visibility != GONE) {
                return false
            }
        }
        return true
    }

    /**
     * Checks whether all (virtual) child views after the given index are gone.
     */
    private fun allViewsAreGoneAfter(childIndex: Int): Boolean {
        val count = childCount
        for (i in childIndex + 1 until count) {
            val child = getChildAt(i)
            if (child != null && child.visibility != GONE) {
                return false
            }
        }
        return true
    }

    /**
     * Measures the children when the orientation of this LinearLayout is set
     * to [.VERTICAL].
     *
     * @param widthMeasureSpec Horizontal space requirements as imposed by the parent.
     * @param heightMeasureSpec Vertical space requirements as imposed by the parent.
     *
     * @see .getOrientation
     * @see .setOrientation
     * @see .onMeasure
     */
    fun measureVertical(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        mTotalLength = 0
        var maxWidth = 0
        var childState = 0
        var alternativeMaxWidth = 0
        var weightedMaxWidth = 0
        var allFillParent = true
        var totalWeight = 0f
        val count = childCount
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        var matchWidth = false
        var skippedMeasure = false
        val baselineChildIndex = mBaselineAlignedChildIndex
        val useLargestChild = isMeasureWithLargestChildEnabled
        var largestChildHeight = Int.MIN_VALUE
        var consumedExcessSpace = 0
        var nonSkippedChildCount = 0

        // See how tall everyone is. Also remember max width.
        var i = 0
        while (i < count) {
            val child = getChildAt(i)
            if (child == null) {
                mTotalLength += measureNullChild(i)
                ++i
                continue
            }
            if (child.visibility == GONE) {
                i += getChildrenSkipCount(child, i)
                ++i
                continue
            }
            val lp = child.layoutParams as LayoutParams
            if (i > 0) mTotalLength += (gap * lp.gapRatio).toInt()
            nonSkippedChildCount++
            totalWeight += lp.weight
            val useExcessSpace = lp.height == 0 && lp.weight > 0
            if (heightMode == MeasureSpec.EXACTLY && useExcessSpace) {
                // Optimization: don't bother measuring children who are only
                // laid out using excess space. These views will get measured
                // later if we have space to distribute.
                val totalLength = mTotalLength
                mTotalLength = Math.max(totalLength, totalLength)
                skippedMeasure = true
            } else {
                if (useExcessSpace) {
                    // The heightMode is either UNSPECIFIED or AT_MOST, and
                    // this child is only laid out using excess space. Measure
                    // using WRAP_CONTENT so that we can find out the view's
                    // optimal height. We'll restore the original height of 0
                    // after measurement.
                    lp.height = ViewGroup.LayoutParams.WRAP_CONTENT
                }

                // Determine how big this child would like to be. If this or
                // previous children have given a weight, then we allow it to
                // use all available space (and we will shrink things later
                // if needed).
                val usedHeight = if (totalWeight == 0f) mTotalLength else 0
                measureChildBeforeLayout(
                    child, i, widthMeasureSpec, 0,
                    heightMeasureSpec, usedHeight
                )
                val childHeight = child.measuredHeight
                if (useExcessSpace) {
                    // Restore the original height and record how much space
                    // we've allocated to excess-only children so that we can
                    // match the behavior of EXACTLY measurement.
                    lp.height = 0
                    consumedExcessSpace += childHeight
                }
                val totalLength = mTotalLength
                mTotalLength = Math.max(
                    totalLength, (totalLength + childHeight + getNextLocationOffset(child))
                )
                if (useLargestChild) {
                    largestChildHeight = Math.max(childHeight, largestChildHeight)
                }
            }
            /**
             * If applicable, compute the additional offset to the child's baseline
             * we'll need later when asked [.getBaseline].
             */
            if ((baselineChildIndex >= 0) && (baselineChildIndex == i + 1)) {
                mBaselineChildTop = mTotalLength
            }

            // if we are trying to use a child index for our baseline, the above
            // book keeping only works if there are no children above it with
            // weight.  fail fast to aid the developer.
            if (i < baselineChildIndex && lp.weight > 0) {
                throw RuntimeException(
                    ("A child of LinearLayout with index "
                            + "less than mBaselineAlignedChildIndex has weight > 0, which "
                            + "won't work.  Either remove the weight, or don't set "
                            + "mBaselineAlignedChildIndex.")
                )
            }
            var matchWidthLocally = false
            if (widthMode != MeasureSpec.EXACTLY && lp.width == ViewGroup.LayoutParams.MATCH_PARENT) {
                // The width of the linear layout will scale, and at least one
                // child said it wanted to match our width. Set a flag
                // indicating that we need to remeasure at least that view when
                // we know our width.
                matchWidth = true
                matchWidthLocally = true
            }
            val margin = 0
            val measuredWidth = child.measuredWidth + margin
            maxWidth = Math.max(maxWidth, measuredWidth)
            childState = combineMeasuredStates(childState, child.measuredState)
            allFillParent = allFillParent && lp.width == ViewGroup.LayoutParams.MATCH_PARENT
            if (lp.weight > 0) {
                /*
                 * Widths of weighted Views are bogus if we end up
                 * remeasuring, so keep them separate.
                 */
                weightedMaxWidth = Math.max(
                    weightedMaxWidth,
                    if (matchWidthLocally) margin else measuredWidth
                )
            } else {
                alternativeMaxWidth = Math.max(
                    alternativeMaxWidth,
                    if (matchWidthLocally) margin else measuredWidth
                )
            }
            i += getChildrenSkipCount(child, i)
            ++i
        }
        if (useLargestChild &&
            (heightMode == MeasureSpec.AT_MOST || heightMode == MeasureSpec.UNSPECIFIED)
        ) {
            mTotalLength = 0
            var i = 0
            while (i < count) {
                val child = getChildAt(i)
                if (child == null) {
                    mTotalLength += measureNullChild(i)
                    ++i
                    continue
                }
                if (child.visibility == GONE) {
                    i += getChildrenSkipCount(child, i)
                    ++i
                    continue
                }
                val lp = child.layoutParams as LayoutParams
                // Account for negative margins
                val totalLength = mTotalLength
                mTotalLength = Math.max(
                    totalLength, (totalLength + largestChildHeight + getNextLocationOffset(child))
                )
                ++i
            }
        }

        // Add in our padding
        mTotalLength += paddingTop + paddingBottom
        var heightSize = mTotalLength

        // Check against our minimum height
        heightSize = Math.max(heightSize, suggestedMinimumHeight)

        // Reconcile our calculated size with the heightMeasureSpec
        val heightSizeAndState = resolveSizeAndState(heightSize, heightMeasureSpec, 0)
        heightSize = heightSizeAndState and MEASURED_SIZE_MASK
        // Either expand children with weight to take up available space or
        // shrink them if they extend beyond our current bounds. If we skipped
        // measurement on any children, we need to measure them now.
        var remainingExcess = (heightSize - mTotalLength
                + (consumedExcessSpace))
        if ((skippedMeasure
                    || (totalWeight > 0.0f))
        ) {
            var remainingWeightSum = totalWeight
            mTotalLength = 0
            for (i in 0 until count) {
                val child = getChildAt(i)
                if (child == null || child.visibility == GONE) {
                    continue
                }
                val lp = child.layoutParams as LayoutParams
                val childWeight = lp.weight
                if (childWeight > 0) {
                    val share = (childWeight * remainingExcess / remainingWeightSum).toInt()
                    remainingExcess -= share
                    remainingWeightSum -= childWeight
                    val childHeight: Int
                    if (isMeasureWithLargestChildEnabled && heightMode != MeasureSpec.EXACTLY) {
                        childHeight = largestChildHeight
                    } else if (lp.height == 0 && ((!false
                                || heightMode == MeasureSpec.EXACTLY))
                    ) {
                        // This child needs to be laid out from scratch using
                        // only its share of excess space.
                        childHeight = share
                    } else {
                        // This child had some intrinsic height to which we
                        // need to add its share of excess space.
                        childHeight = child.measuredHeight + share
                    }
                    val childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(
                        Math.max(0, childHeight), MeasureSpec.EXACTLY
                    )
                    val childWidthMeasureSpec = getChildMeasureSpec(
                        widthMeasureSpec,
                        paddingLeft + paddingRight,
                        lp.width
                    )
                    child.measure(childWidthMeasureSpec, childHeightMeasureSpec)

                    // Child may now not fit in vertical dimension.
                    childState = combineMeasuredStates(
                        childState, (child.measuredState
                                and (MEASURED_STATE_MASK shr MEASURED_HEIGHT_STATE_SHIFT))
                    )
                }
                val margin = 0
                val measuredWidth = child.measuredWidth + margin
                maxWidth = Math.max(maxWidth, measuredWidth)
                val matchWidthLocally = widthMode != MeasureSpec.EXACTLY &&
                        lp.width == ViewGroup.LayoutParams.MATCH_PARENT
                alternativeMaxWidth = Math.max(
                    alternativeMaxWidth,
                    if (matchWidthLocally) margin else measuredWidth
                )
                allFillParent = allFillParent && lp.width == ViewGroup.LayoutParams.MATCH_PARENT
                val totalLength = mTotalLength
                mTotalLength = Math.max(
                    totalLength, (totalLength + child.measuredHeight +
                            getNextLocationOffset(child))
                )
            }

            // Add in our padding
            mTotalLength += paddingTop + paddingBottom
            // TODO: Should we recompute the heightSpec based on the new total length?
        } else {
            alternativeMaxWidth = Math.max(
                alternativeMaxWidth,
                weightedMaxWidth
            )


            // We have no limit, so make all weighted views as tall as the largest child.
            // Children will have already been measured once.
            if (useLargestChild && heightMode != MeasureSpec.EXACTLY) {
                for (i in 0 until count) {
                    val child = getChildAt(i)
                    if (child == null || child.visibility == GONE) {
                        continue
                    }
                    val lp = child.layoutParams as LayoutParams
                    val childExtra = lp.weight
                    if (childExtra > 0) {
                        child.measure(
                            MeasureSpec.makeMeasureSpec(
                                child.measuredWidth,
                                MeasureSpec.EXACTLY
                            ),
                            MeasureSpec.makeMeasureSpec(
                                largestChildHeight,
                                MeasureSpec.EXACTLY
                            )
                        )
                    }
                }
            }
        }
//        if (!allFillParent && widthMode != MeasureSpec.EXACTLY) {
//            maxWidth = alternativeMaxWidth
//        }
        maxWidth += paddingLeft + paddingRight

        // Check against our minimum width
        maxWidth = Math.max(maxWidth, suggestedMinimumWidth)
        setMeasuredDimension(
            resolveSizeAndState(maxWidth, widthMeasureSpec, childState),
            heightSizeAndState
        )
        if (matchWidth) {
            forceUniformWidth(count, heightMeasureSpec)
        }
    }

    private fun forceUniformWidth(count: Int, heightMeasureSpec: Int) {
        // Pretend that the linear layout has an exact size.
        val uniformMeasureSpec = MeasureSpec.makeMeasureSpec(
            measuredWidth,
            MeasureSpec.EXACTLY
        )
        for (i in 0 until count) {
            val child = getChildAt(i)
            if (child != null && child.visibility != GONE) {
                val lp = (child.layoutParams as LayoutParams)
                if (lp.width == ViewGroup.LayoutParams.MATCH_PARENT) {
                    // Temporarily force children to reuse their old measured height
                    // FIXME: this may not be right for something like wrapping text?
                    val oldHeight = lp.height
                    lp.height = child.measuredHeight

                    // Remeasue with new dimensions
                    measureChild(child, uniformMeasureSpec, heightMeasureSpec)
                    lp.height = oldHeight
                }
            }
        }
    }

    /**
     * Measures the children when the orientation of this LinearLayout is set
     * to [.HORIZONTAL].
     *
     * @param widthMeasureSpec Horizontal space requirements as imposed by the parent.
     * @param heightMeasureSpec Vertical space requirements as imposed by the parent.
     *
     * @see .getOrientation
     * @see .setOrientation
     * @see .onMeasure
     */
    fun measureHorizontal(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        mTotalLength = 0
        var maxHeight = 0
        var childState = 0
        var alternativeMaxHeight = 0
        var weightedMaxHeight = 0
        var allFillParent = true
        var totalWeight = 0f
        val count = childCount
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        var matchHeight = false
        var skippedMeasure = false
        if (mMaxAscent == null || mMaxDescent == null) {
            mMaxAscent = IntArray(4)
            mMaxDescent = IntArray(4)
        }
        val maxAscent: IntArray = mMaxAscent
        val maxDescent: IntArray = mMaxDescent
        maxAscent[3] = -1
        maxAscent[2] = maxAscent[3]
        maxAscent[1] = maxAscent[2]
        maxAscent[0] = maxAscent[1]
        maxDescent[3] = -1
        maxDescent[2] = maxDescent[3]
        maxDescent[1] = maxDescent[2]
        maxDescent[0] = maxDescent[1]
        val baselineAligned = isBaselineAligned
        val useLargestChild = isMeasureWithLargestChildEnabled
        val isExactly = widthMode == MeasureSpec.EXACTLY
        var largestChildWidth = Int.MIN_VALUE
        var usedExcessSpace = 0
        var nonSkippedChildCount = 0

        // See how wide everyone is. Also remember max height.
        var i = 0
        while (i < count) {
            val child = getChildAt(i)
            if (child == null) {
                mTotalLength += measureNullChild(i)
                ++i
                continue
            }
            if (child.visibility == GONE) {
                i += getChildrenSkipCount(child, i)
                ++i
                continue
            }
            val lp = child.layoutParams as LayoutParams
            if (i > 0) mTotalLength += (gap * lp.gapRatio).toInt()
            nonSkippedChildCount++
            totalWeight += lp.weight
            val useExcessSpace = lp.width == 0 && lp.weight > 0
            if (widthMode == MeasureSpec.EXACTLY && useExcessSpace) {
                // Optimization: don't bother measuring children who are only
                // laid out using excess space. These views will get measured
                // later if we have space to distribute.

                // Baseline alignment requires to measure widgets to obtain the
                // baseline offset (in particular for TextViews). The following
                // defeats the optimization mentioned above. Allow the child to
                // use as much space as it wants because we can shrink things
                // later (and re-measure).
                if (baselineAligned) {
                    val freeWidthSpec: Int = MeasureSpec.makeMeasureSpec(
                        MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.UNSPECIFIED
                    )
                    val freeHeightSpec: Int = MeasureSpec.makeMeasureSpec(
                        MeasureSpec.getSize(heightMeasureSpec), MeasureSpec.UNSPECIFIED
                    )
                    child.measure(freeWidthSpec, freeHeightSpec)
                } else {
                    skippedMeasure = true
                }
            } else {
                if (useExcessSpace) {
                    // The widthMode is either UNSPECIFIED or AT_MOST, and
                    // this child is only laid out using excess space. Measure
                    // using WRAP_CONTENT so that we can find out the view's
                    // optimal width. We'll restore the original width of 0
                    // after measurement.
                    lp.width = ViewGroup.LayoutParams.WRAP_CONTENT
                }

                // Determine how big this child would like to be. If this or
                // previous children have given a weight, then we allow it to
                // use all available space (and we will shrink things later
                // if needed).
                val usedWidth = if (totalWeight == 0f) mTotalLength else 0
                measureChildBeforeLayout(
                    child, i, widthMeasureSpec, usedWidth,
                    heightMeasureSpec, 0
                )
                val childWidth = child.measuredWidth
                if (useExcessSpace) {
                    // Restore the original width and record how much space
                    // we've allocated to excess-only children so that we can
                    // match the behavior of EXACTLY measurement.
                    lp.width = 0
                    usedExcessSpace += childWidth
                }
                if (isExactly) {
                    mTotalLength += (childWidth
                            + getNextLocationOffset(child))
                } else {
                    val totalLength = mTotalLength
                    mTotalLength = Math.max(
                        totalLength, (totalLength + childWidth
                                 + getNextLocationOffset(child))
                    )
                }
                if (useLargestChild) {
                    largestChildWidth = Math.max(childWidth, largestChildWidth)
                }
            }
            var matchHeightLocally = false
            if (heightMode != MeasureSpec.EXACTLY && lp.height == ViewGroup.LayoutParams.MATCH_PARENT) {
                // The height of the linear layout will scale, and at least one
                // child said it wanted to match our height. Set a flag indicating that
                // we need to remeasure at least that view when we know our height.
                matchHeight = true
                matchHeightLocally = true
            }
            val margin = 0
            val childHeight = child.measuredHeight + margin
            childState = combineMeasuredStates(childState, child.measuredState)
            if (baselineAligned) {
                val childBaseline = child.baseline
                if (childBaseline != -1) {
                    // Translates the child's vertical gravity into an index
                    // in the range 0..VERTICAL_GRAVITY_COUNT
                    val gravity = ((if (lp.gravity < 0) mGravity else lp.gravity)
                            and Gravity.VERTICAL_GRAVITY_MASK)
                    val index = (((gravity shr Gravity.AXIS_Y_SHIFT)
                            and Gravity.AXIS_SPECIFIED.inv())) shr 1
                    maxAscent[index] = Math.max(maxAscent[index], childBaseline)
                    maxDescent[index] = Math.max(maxDescent[index], childHeight - childBaseline)
                }
            }
            maxHeight = Math.max(maxHeight, childHeight)
            allFillParent = allFillParent && lp.height == ViewGroup.LayoutParams.MATCH_PARENT
            if (lp.weight > 0) {
                /*
                 * Heights of weighted Views are bogus if we end up
                 * remeasuring, so keep them separate.
                 */
                weightedMaxHeight = Math.max(
                    weightedMaxHeight,
                    if (matchHeightLocally) margin else childHeight
                )
            } else {
                alternativeMaxHeight = Math.max(
                    alternativeMaxHeight,
                    if (matchHeightLocally) margin else childHeight
                )
            }
            i += getChildrenSkipCount(child, i)
            ++i
        }

        // Check mMaxAscent[INDEX_TOP] first because it maps to Gravity.TOP,
        // the most common case
        if ((maxAscent[INDEX_TOP] != -1) || (
                    maxAscent[INDEX_CENTER_VERTICAL] != -1) || (
                    maxAscent[INDEX_BOTTOM] != -1) || (
                    maxAscent[INDEX_FILL] != -1)
        ) {
            val ascent = Math.max(
                maxAscent[INDEX_FILL],
                Math.max(
                    maxAscent[INDEX_CENTER_VERTICAL],
                    Math.max(
                        maxAscent[INDEX_TOP],
                        maxAscent[INDEX_BOTTOM]
                    )
                )
            )
            val descent = Math.max(
                maxDescent[INDEX_FILL],
                Math.max(
                    maxDescent[INDEX_CENTER_VERTICAL],
                    Math.max(
                        maxDescent[INDEX_TOP],
                        maxDescent[INDEX_BOTTOM]
                    )
                )
            )
            maxHeight = Math.max(maxHeight, ascent + descent)
        }
        if (useLargestChild &&
            (widthMode == MeasureSpec.AT_MOST || widthMode == MeasureSpec.UNSPECIFIED)
        ) {
            mTotalLength = 0
            nonSkippedChildCount = 0
            var i = 0
            while (i < count) {
                val child = getChildAt(i)
                if (child == null) {
                    mTotalLength += measureNullChild(i)
                    ++i
                    continue
                }
                if (child.visibility == GONE) {
                    i += getChildrenSkipCount(child, i)
                    ++i
                    continue
                }
                nonSkippedChildCount++
                val lp = child.layoutParams as LayoutParams
                if (isExactly) {
                    mTotalLength += (largestChildWidth  +
                            getNextLocationOffset(child))
                } else {
                    val totalLength = mTotalLength
                    mTotalLength = Math.max(
                        totalLength, (totalLength + largestChildWidth + getNextLocationOffset(child))
                    )
                }
                ++i
            }
        }

        // Add in our padding
        mTotalLength += paddingLeft + paddingRight
        var widthSize = mTotalLength

        // Check against our minimum width
        widthSize = Math.max(widthSize, suggestedMinimumWidth)

        // Reconcile our calculated size with the widthMeasureSpec
        val widthSizeAndState = resolveSizeAndState(widthSize, widthMeasureSpec, 0)
        widthSize = widthSizeAndState and MEASURED_SIZE_MASK

        // Either expand children with weight to take up available space or
        // shrink them if they extend beyond our current bounds. If we skipped
        // measurement on any children, we need to measure them now.
        var remainingExcess = (widthSize - mTotalLength
                + (if (false) 0 else usedExcessSpace))
        if ((skippedMeasure
                    || ((true || remainingExcess != 0) && totalWeight > 0.0f))
        ) {
            var remainingWeightSum = totalWeight
            maxAscent[3] = -1
            maxAscent[2] = maxAscent[3]
            maxAscent[1] = maxAscent[2]
            maxAscent[0] = maxAscent[1]
            maxDescent[3] = -1
            maxDescent[2] = maxDescent[3]
            maxDescent[1] = maxDescent[2]
            maxDescent[0] = maxDescent[1]
            maxHeight = -1
            mTotalLength = 0
            nonSkippedChildCount = 0
            for (i in 0 until count) {
                val child = getChildAt(i)
                if (child == null || child.visibility == GONE) {
                    continue
                }
                nonSkippedChildCount++
                val lp = child.layoutParams as LayoutParams
                val childWeight = lp.weight
                if (childWeight > 0) {
                    val share = (childWeight * remainingExcess / remainingWeightSum).toInt()
                    remainingExcess -= share
                    remainingWeightSum -= childWeight
                    val childWidth: Int
                    if (isMeasureWithLargestChildEnabled && widthMode != MeasureSpec.EXACTLY) {
                        childWidth = largestChildWidth
                    } else if (lp.width == 0 && ((!false
                                || widthMode == MeasureSpec.EXACTLY))
                    ) {
                        // This child needs to be laid out from scratch using
                        // only its share of excess space.
                        childWidth = share
                    } else {
                        // This child had some intrinsic width to which we
                        // need to add its share of excess space.
                        childWidth = child.measuredWidth + share
                    }
                    val childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(
                        Math.max(0, childWidth), MeasureSpec.EXACTLY
                    )
                    val childHeightMeasureSpec = getChildMeasureSpec(
                        heightMeasureSpec,
                        paddingTop + paddingBottom,
                        lp.height
                    )
                    child.measure(childWidthMeasureSpec, childHeightMeasureSpec)

                    // Child may now not fit in horizontal dimension.
                    childState = combineMeasuredStates(
                        childState,
                        child.measuredState and MEASURED_STATE_MASK
                    )
                }
                if (isExactly) {
                    mTotalLength += (child.measuredWidth +
                            getNextLocationOffset(child))
                } else {
                    val totalLength = mTotalLength
                    mTotalLength = Math.max(
                        totalLength, (totalLength + child.measuredWidth + getNextLocationOffset(child))
                    )
                }
                val matchHeightLocally = heightMode != MeasureSpec.EXACTLY &&
                        lp.height == ViewGroup.LayoutParams.MATCH_PARENT
                val margin = 0
                val childHeight = child.measuredHeight + margin
                maxHeight = Math.max(maxHeight, childHeight)
                alternativeMaxHeight = Math.max(
                    alternativeMaxHeight,
                    if (matchHeightLocally) margin else childHeight
                )
                allFillParent = allFillParent && lp.height == ViewGroup.LayoutParams.MATCH_PARENT
                if (baselineAligned) {
                    val childBaseline = child.baseline
                    if (childBaseline != -1) {
                        // Translates the child's vertical gravity into an index in the range 0..2
                        val gravity = ((if (lp.gravity < 0) mGravity else lp.gravity)
                                and Gravity.VERTICAL_GRAVITY_MASK)
                        val index = (((gravity shr Gravity.AXIS_Y_SHIFT)
                                and Gravity.AXIS_SPECIFIED.inv())) shr 1
                        maxAscent[index] = Math.max(maxAscent[index], childBaseline)
                        maxDescent[index] = Math.max(
                            maxDescent[index],
                            childHeight - childBaseline
                        )
                    }
                }
            }

            // Add in our padding
            mTotalLength += paddingLeft + paddingRight
            // TODO: Should we update widthSize with the new total length?

            // Check mMaxAscent[INDEX_TOP] first because it maps to Gravity.TOP,
            // the most common case
            if ((maxAscent[INDEX_TOP] != -1) || (
                        maxAscent[INDEX_CENTER_VERTICAL] != -1) || (
                        maxAscent[INDEX_BOTTOM] != -1) || (
                        maxAscent[INDEX_FILL] != -1)
            ) {
                val ascent = Math.max(
                    maxAscent[INDEX_FILL],
                    Math.max(
                        maxAscent[INDEX_CENTER_VERTICAL],
                        Math.max(
                            maxAscent[INDEX_TOP],
                            maxAscent[INDEX_BOTTOM]
                        )
                    )
                )
                val descent = Math.max(
                    maxDescent[INDEX_FILL],
                    Math.max(
                        maxDescent[INDEX_CENTER_VERTICAL],
                        Math.max(
                            maxDescent[INDEX_TOP],
                            maxDescent[INDEX_BOTTOM]
                        )
                    )
                )
                maxHeight = Math.max(maxHeight, ascent + descent)
            }
        } else {
            alternativeMaxHeight = Math.max(alternativeMaxHeight, weightedMaxHeight)

            // We have no limit, so make all weighted views as wide as the largest child.
            // Children will have already been measured once.
            if (useLargestChild && widthMode != MeasureSpec.EXACTLY) {
                for (i in 0 until count) {
                    val child = getChildAt(i)
                    if (child == null || child.visibility == GONE) {
                        continue
                    }
                    val lp = child.layoutParams as LayoutParams
                    val childExtra = lp.weight
                    if (childExtra > 0) {
                        child.measure(
                            MeasureSpec.makeMeasureSpec(largestChildWidth, MeasureSpec.EXACTLY),
                            MeasureSpec.makeMeasureSpec(
                                child.measuredHeight,
                                MeasureSpec.EXACTLY
                            )
                        )
                    }
                }
            }
        }
//        if (!allFillParent && heightMode != MeasureSpec.EXACTLY) {
//            maxHeight = alternativeMaxHeight
//        }
        maxHeight += paddingTop + paddingBottom

        // Check against our minimum height
        maxHeight = Math.max(maxHeight, suggestedMinimumHeight)
        setMeasuredDimension(
            widthSizeAndState or (childState and MEASURED_STATE_MASK),
            resolveSizeAndState(
                maxHeight, heightMeasureSpec,
                (childState shl MEASURED_HEIGHT_STATE_SHIFT)
            )
        )
        if (matchHeight) {
            forceUniformHeight(count, widthMeasureSpec)
        }
    }

    private fun forceUniformHeight(count: Int, widthMeasureSpec: Int) {
        // Pretend that the linear layout has an exact size. This is the measured height of
        // ourselves. The measured height should be the max height of the children, changed
        // to accommodate the heightMeasureSpec from the parent
        val uniformMeasureSpec = MeasureSpec.makeMeasureSpec(
            measuredHeight,
            MeasureSpec.EXACTLY
        )
        for (i in 0 until count) {
            val child = getChildAt(i)
            if (child != null && child.visibility != GONE) {
                val lp = child.layoutParams as LayoutParams
                if (lp.height == ViewGroup.LayoutParams.MATCH_PARENT) {
                    // Temporarily force children to reuse their old measured width
                    // FIXME: this may not be right for something like wrapping text?
                    val oldWidth = lp.width
                    lp.width = child.measuredWidth

                    // Remeasure with new dimensions
                    measureChild(child, widthMeasureSpec, uniformMeasureSpec)
                    lp.width = oldWidth
                }
            }
        }
    }

    /**
     *
     * Returns the number of children to skip after measuring/laying out
     * the specified child.
     *
     * @param child the child after which we want to skip children
     * @param index the index of the child after which we want to skip children
     * @return the number of children to skip, 0 by default
     */
    fun getChildrenSkipCount(child: View?, index: Int): Int {
        return 0
    }

    /**
     *
     * Returns the size (width or height) that should be occupied by a null
     * child.
     *
     * @param childIndex the index of the null child
     * @return the width or height of the child depending on the orientation
     */
    fun measureNullChild(childIndex: Int): Int {
        return 0
    }

    /**
     *
     * Measure the child according to the parent's measure specs. This
     * method should be overridden by subclasses to force the sizing of
     * children. This method is called by [.measureVertical] and
     * [.measureHorizontal].
     *
     * @param child the child to measure
     * @param childIndex the index of the child in this view
     * @param widthMeasureSpec horizontal space requirements as imposed by the parent
     * @param totalWidth extra space that has been used up by the parent horizontally
     * @param heightMeasureSpec vertical space requirements as imposed by the parent
     * @param totalHeight extra space that has been used up by the parent vertically
     */
    fun measureChildBeforeLayout(
        child: View?, childIndex: Int,
        widthMeasureSpec: Int, totalWidth: Int, heightMeasureSpec: Int,
        totalHeight: Int
    ) {
        measureChild(
            child, widthMeasureSpec,
            heightMeasureSpec
        )
    }

    /**
     *
     * Return the location offset of the specified child. This can be used
     * by subclasses to change the location of a given widget.
     *
     * @param child the child for which to obtain the location offset
     * @return the location offset in pixels
     */
    fun getLocationOffset(child: View?): Int {
        return 0
    }

    /**
     *
     * Return the size offset of the next sibling of the specified child.
     * This can be used by subclasses to change the location of the widget
     * following `child`.
     *
     * @param child the child whose next sibling will be moved
     * @return the location offset of the next child in pixels
     */
    fun getNextLocationOffset(child: View?): Int {
        return 0
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        if (mOrientation == VERTICAL) {
            layoutVertical(l, t, r, b)
        } else {
            layoutHorizontal(l, t, r, b)
        }
    }

    /**
     * Position the children during a layout pass if the orientation of this
     * LinearLayout is set to [.VERTICAL].
     *
     * @see .getOrientation
     * @see .setOrientation
     * @see .onLayout
     * @param left
     * @param top
     * @param right
     * @param bottom
     */
    fun layoutVertical(left: Int, top: Int, right: Int, bottom: Int) {
        val paddingLeft: Int = paddingLeft
        var childTop: Int
        var childLeft: Int

        // Where right end of child should go
        val width = right - left
        val childRight: Int = width - paddingRight

        // Space available for child
        val childSpace: Int = width - paddingLeft - paddingRight
        val count = childCount
        val majorGravity = mGravity and Gravity.VERTICAL_GRAVITY_MASK
        val minorGravity = mGravity and Gravity.RELATIVE_HORIZONTAL_GRAVITY_MASK
        when (majorGravity) {
            Gravity.BOTTOM ->                // mTotalLength contains the padding already
                childTop = (paddingTop + bottom) - top - mTotalLength

            Gravity.CENTER_VERTICAL -> childTop = paddingTop + (bottom - top - mTotalLength) / 2
            Gravity.TOP -> childTop = paddingTop
            else -> childTop = paddingTop
        }
        var i = 0
        while (i < count) {
            val child = getChildAt(i)
            if (child == null) {
                childTop += measureNullChild(i)
            } else if (child.visibility != GONE) {
                val childWidth = child.measuredWidth
                val childHeight = child.measuredHeight
                val lp = child.layoutParams as LayoutParams
                var gravity = lp.gravity
                if (gravity < 0) {
                    gravity = minorGravity
                }
                val layoutDirection = layoutDirection
                val absoluteGravity = Gravity.getAbsoluteGravity(gravity, layoutDirection)
                when (absoluteGravity and Gravity.HORIZONTAL_GRAVITY_MASK) {
                    Gravity.CENTER_HORIZONTAL -> childLeft = (paddingLeft + ((childSpace - childWidth) / 2))

                    Gravity.RIGHT -> childLeft = childRight - childWidth
                    Gravity.LEFT -> childLeft = paddingLeft
                    else -> childLeft = paddingLeft
                }
                setChildFrame(
                    child, childLeft, childTop + getLocationOffset(child),
                    childWidth, childHeight
                )
                childTop += childHeight + (gap * lp.gapRatio).toInt() + getNextLocationOffset(child)
                i += getChildrenSkipCount(child, i)
            }
            i++
        }
    }

    override fun onRtlPropertiesChanged(layoutDirection: Int) {
        super.onRtlPropertiesChanged(layoutDirection)
        if (layoutDirection != mLayoutDirection) {
            mLayoutDirection = layoutDirection
            if (mOrientation == HORIZONTAL) {
                requestLayout()
            }
        }
    }

    /**
     * Position the children during a layout pass if the orientation of this
     * LinearLayout is set to [.HORIZONTAL].
     *
     * @see .getOrientation
     * @see .setOrientation
     * @see .onLayout
     * @param left
     * @param top
     * @param right
     * @param bottom
     */
    fun layoutHorizontal(left: Int, top: Int, right: Int, bottom: Int) {
        val isLayoutRtl: Boolean = layoutDirection == LAYOUT_DIRECTION_RTL
        val paddingTop: Int = paddingTop
        var childTop: Int
        var childLeft: Int

        // Where bottom of child should go
        val height = bottom - top
        val childBottom: Int = height - paddingBottom

        // Space available for child
        val childSpace: Int = height - paddingTop - paddingBottom
        val count = childCount
        val majorGravity = mGravity and Gravity.RELATIVE_HORIZONTAL_GRAVITY_MASK
        val minorGravity = mGravity and Gravity.VERTICAL_GRAVITY_MASK
        val baselineAligned = isBaselineAligned
        val maxAscent = mMaxAscent
        val maxDescent = mMaxDescent
        val layoutDirection = layoutDirection
        when (Gravity.getAbsoluteGravity(majorGravity, layoutDirection)) {
            Gravity.RIGHT ->                 // mTotalLength contains the padding already
                childLeft = (paddingLeft + right) - left - mTotalLength

            Gravity.CENTER_HORIZONTAL ->                 // mTotalLength contains the padding already
                childLeft = paddingLeft + (right - left - mTotalLength) / 2

            Gravity.LEFT -> childLeft = paddingLeft
            else -> childLeft = paddingLeft
        }
        var start = 0
        var dir = 1
        //In case of RTL, start drawing from the last child.
        if (isLayoutRtl) {
            start = count - 1
            dir = -1
        }
        var i = 0
        while (i < count) {
            val childIndex = start + dir * i
            val child = getChildAt(childIndex)
            if (child == null) {
                childLeft += measureNullChild(childIndex)
            } else if (child.visibility != GONE) {
                val childWidth = child.measuredWidth
                val childHeight = child.measuredHeight
                var childBaseline = -1
                val lp = child.layoutParams as LayoutParams
                if (baselineAligned && lp.height != ViewGroup.LayoutParams.MATCH_PARENT) {
                    childBaseline = child.baseline
                }
                var gravity = lp.gravity
                if (gravity < 0) {
                    gravity = minorGravity
                }
                when (gravity and Gravity.VERTICAL_GRAVITY_MASK) {
                    Gravity.TOP -> {
                        childTop = paddingTop
                        if (childBaseline != -1) {
                            childTop += maxAscent!![INDEX_TOP] - childBaseline
                        }
                    }

                    Gravity.CENTER_VERTICAL ->                         // Removed support for baseline alignment when layout_gravity or
                        // gravity == center_vertical. See bug #1038483.
                        // Keep the code around if we need to re-enable this feature
                        // if (childBaseline != -1) {
                        //     // Align baselines vertically only if the child is smaller than us
                        //     if (childSpace - childHeight > 0) {
                        //         childTop = paddingTop + (childSpace / 2) - childBaseline;
                        //     } else {
                        //         childTop = paddingTop + (childSpace - childHeight) / 2;
                        //     }
                        // } else {
                        childTop = (paddingTop + ((childSpace - childHeight) / 2))

                    Gravity.BOTTOM -> {
                        childTop = childBottom - childHeight
                        if (childBaseline != -1) {
                            val descent = child.measuredHeight - childBaseline
                            childTop -= (maxDescent!![INDEX_BOTTOM] - descent)
                        }
                    }

                    else -> childTop = paddingTop
                }
                setChildFrame(
                    child, childLeft + getLocationOffset(child), childTop,
                    childWidth, childHeight
                )
                childLeft += (childWidth + (gap * lp.gapRatio).toInt() +
                        getNextLocationOffset(child))
                i += getChildrenSkipCount(child, childIndex)
            }
            i++
        }
    }

    private fun setChildFrame(child: View, left: Int, top: Int, width: Int, height: Int) {
        child.layout(left, top, left + width, top + height)
    }

    open var orientation: Int
        /**
         * Returns the current orientation.
         *
         * @return either [.HORIZONTAL] or [.VERTICAL]
         */
        get() = mOrientation
        /**
         * Should the layout be a column or a row.
         * @param orientation Pass [.HORIZONTAL] or [.VERTICAL]. Default
         * value is [.HORIZONTAL].
         *
         * @attr ref android.R.styleable#LinearLayout_orientation
         */
        set(orientation) {
            if (mOrientation != orientation) {
                mOrientation = orientation
                requestLayout()
            }
        }

    var gravity: Int
        /**
         * Returns the current gravity. See [android.view.Gravity]
         *
         * @return the current gravity.
         * @see .setGravity
         */
        get() = mGravity
        /**
         * Describes how the child views are positioned. Defaults to GRAVITY_TOP. If
         * this layout has a VERTICAL orientation, this controls where all the child
         * views are placed if there is extra vertical space. If this layout has a
         * HORIZONTAL orientation, this controls the alignment of the children.
         *
         * @param gravity See [android.view.Gravity]
         *
         * @attr ref android.R.styleable#LinearLayout_gravity
         */
        set(gravity) {
            var gravity = gravity
            if (mGravity != gravity) {
                if ((gravity and Gravity.RELATIVE_HORIZONTAL_GRAVITY_MASK) == 0) {
                    gravity = gravity or Gravity.START
                }
                if ((gravity and Gravity.VERTICAL_GRAVITY_MASK) == 0) {
                    gravity = gravity or Gravity.TOP
                }
                mGravity = gravity
                requestLayout()
            }
        }

    fun setHorizontalGravity(horizontalGravity: Int) {
        val gravity = horizontalGravity and Gravity.RELATIVE_HORIZONTAL_GRAVITY_MASK
        if ((mGravity and Gravity.RELATIVE_HORIZONTAL_GRAVITY_MASK) != gravity) {
            mGravity = (mGravity and Gravity.RELATIVE_HORIZONTAL_GRAVITY_MASK.inv()) or gravity
            requestLayout()
        }
    }

    fun setVerticalGravity(verticalGravity: Int) {
        val gravity = verticalGravity and Gravity.VERTICAL_GRAVITY_MASK
        if ((mGravity and Gravity.VERTICAL_GRAVITY_MASK) != gravity) {
            mGravity = (mGravity and Gravity.VERTICAL_GRAVITY_MASK.inv()) or gravity
            requestLayout()
        }
    }

    override fun generateLayoutParams(attrs: AttributeSet): LayoutParams {
        return LayoutParams(0, 0)
    }

    override fun generateDefaultLayoutParams(): LayoutParams? {
        if (orientation == HORIZONTAL) {
            return LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        } else if (orientation == VERTICAL) {
            return LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        return null
    }

    override fun generateLayoutParams(lp: ViewGroup.LayoutParams): LayoutParams {
        if (lp is LayoutParams) {
            return LayoutParams(lp)
        } else if (lp is MarginLayoutParams) {
            return LayoutParams(lp)
        } else TODO()
    }

    // Override to allow type-checking of LayoutParams.
    override fun checkLayoutParams(p: ViewGroup.LayoutParams): Boolean {
        return p is LayoutParams
    }

    override fun getAccessibilityClassName(): CharSequence {
        return "LinearLayout"
    }

    /**
     * Per-child layout information associated with ViewLinearLayout.
     *
     * @attr ref android.R.styleable#LinearLayout_Layout_layout_weight
     * @attr ref android.R.styleable#LinearLayout_Layout_layout_gravity
     */
    open class LayoutParams : UseMarginsLayoutParams {
        /**
         * Indicates how much of the extra space in the LinearLayout will be
         * allocated to the view associated with these LayoutParams. Specify
         * 0 if the view should not be stretched. Otherwise the extra pixels
         * will be pro-rated among all views whose weight is greater than 0.
         */
        @ViewDebug.ExportedProperty(category = "layout")
        var weight = 0f
        /**
         * Indicates how much of the extra space in the LinearLayout will be
         * allocated to the view associated with these LayoutParams. Specify
         * 0 if the view should not be stretched. Otherwise the extra pixels
         * will be pro-rated among all views whose weight is greater than 0.
         */
        @ViewDebug.ExportedProperty(category = "layout")
        var gapRatio = 1f

        /**
         * Gravity for the view associated with these LayoutParams.
         *
         * @see android.view.Gravity
         */
        @ViewDebug.ExportedProperty(
            category = "layout",
            mapping = [ViewDebug.IntToString(from = -1, to = "NONE"), ViewDebug.IntToString(
                from = Gravity.NO_GRAVITY,
                to = "NONE"
            ), ViewDebug.IntToString(from = Gravity.TOP, to = "TOP"), ViewDebug.IntToString(
                from = Gravity.BOTTOM,
                to = "BOTTOM"
            ), ViewDebug.IntToString(from = Gravity.LEFT, to = "LEFT"), ViewDebug.IntToString(
                from = Gravity.RIGHT,
                to = "RIGHT"
            ), ViewDebug.IntToString(from = Gravity.START, to = "START"), ViewDebug.IntToString(
                from = Gravity.END,
                to = "END"
            ), ViewDebug.IntToString(
                from = Gravity.CENTER_VERTICAL,
                to = "CENTER_VERTICAL"
            ), ViewDebug.IntToString(
                from = Gravity.FILL_VERTICAL,
                to = "FILL_VERTICAL"
            ), ViewDebug.IntToString(from = Gravity.CENTER_HORIZONTAL, to = "CENTER_HORIZONTAL"), ViewDebug.IntToString(
                from = Gravity.FILL_HORIZONTAL,
                to = "FILL_HORIZONTAL"
            ), ViewDebug.IntToString(from = Gravity.CENTER, to = "CENTER"), ViewDebug.IntToString(
                from = Gravity.FILL,
                to = "FILL"
            )]
        )
        var gravity = -1

        /**
         * {@inheritDoc}
         */
        constructor(width: Int, height: Int) : super(width, height) {
            weight = 0f
        }

        /**
         * Creates a new set of layout parameters with the specified width, height
         * and weight.
         *
         * @param width the width, either [.MATCH_PARENT],
         * [.WRAP_CONTENT] or a fixed size in pixels
         * @param height the height, either [.MATCH_PARENT],
         * [.WRAP_CONTENT] or a fixed size in pixels
         * @param weight the weight
         */
        constructor(width: Int, height: Int, weight: Float) : super(width, height) {
            this.weight = weight
        }

        /**
         * {@inheritDoc}
         */
        constructor(p: ViewGroup.LayoutParams) : super(p)

        /**
         * Copy constructor. Clones the width, height, margin values, weight,
         * and gravity of the source.
         *
         * @param source The layout params to copy from.
         */
        constructor(source: LayoutParams) : super(source) {
            weight = source.weight
            gravity = source.gravity
        }
    }

    companion object {
        const val HORIZONTAL = 0
        const val VERTICAL = 1
        private const val VERTICAL_GRAVITY_COUNT = 4

        private const val INDEX_CENTER_VERTICAL = 0
        private const val INDEX_TOP = 1
        private const val INDEX_BOTTOM = 2
        private const val INDEX_FILL = 3
    }
}


abstract class UseMarginsLayoutParams: ViewGroup.LayoutParams {
    constructor(width: Int, height: Int):super(width, height)
    constructor(c: Context, attrs: AttributeSet):super(c, attrs)
    constructor(source: ViewGroup.LayoutParams):super(source) {
        useMargins = (source as? UseMarginsLayoutParams)?.useMargins ?: false
    }
    var useMargins: Boolean = false
}
