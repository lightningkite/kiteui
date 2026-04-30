package com.lightningkite.kiteui.views.direct

import android.R
import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.*
import android.view.accessibility.AccessibilityEvent
import android.view.animation.AnimationUtils
import android.widget.EdgeEffect
import android.widget.FrameLayout
import android.widget.OverScroller
import android.widget.ScrollView
import androidx.annotation.RestrictTo
import androidx.core.view.*
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.core.view.accessibility.AccessibilityRecordCompat
import androidx.core.widget.EdgeEffectCompat
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min


/**
 * TwoWayNestedScrollView is just like [android.widget.ScrollView], but it supports acting
 * as both a nested scrolling parent and child on both new and old versions of Android.
 * Nested scrolling is enabled by default.
 */
class TwoWayNestedScrollView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) :
    FrameLayout(context, attrs, defStyleAttr), NestedScrollingParent3, NestedScrollingChild3,
    ScrollingView {
    /**
     * Interface definition for a callback to be invoked when the scroll
     * X or Y positions of a view change.
     *
     *
     * This version of the interface works on all versions of Android, back to API v4.
     *
     * @see .setOnScrollChangeListener
     */
    interface OnScrollChangeListener {
        /**
         * Called when the scroll position of a view changes.
         *
         * @param v The view whose scroll position has changed.
         * @param scrollX Current horizontal scroll origin.
         * @param scrollY Current vertical scroll origin.
         * @param oldScrollX Previous horizontal scroll origin.
         * @param oldScrollY Previous vertical scroll origin.
         */
        fun onScrollChange(
            v: TwoWayNestedScrollView?, scrollX: Int, scrollY: Int,
            oldScrollX: Int, oldScrollY: Int
        )
    }

    private var mLastScroll: Long = 0

    private val mTempRect = Rect()
    var mScroller: OverScroller? = null
    private var mEdgeGlowLeft: EdgeEffect? = null
    private var mEdgeGlowTop: EdgeEffect? = null
    private var mEdgeGlowRight: EdgeEffect? = null
    private var mEdgeGlowBottom: EdgeEffect? = null

    /**
     * Position of the last motion event.
     */
    private var mLastMotionX = 0
    private var mLastMotionY = 0

    /**
     * True when the layout has changed but the traversal has not come through yet.
     * Ideally the view hierarchy would keep track of this for us.
     */
    private var mIsLayoutDirty = true
    private var mIsLaidOut = false

    /**
     * The child to give focus to in the event that a child has requested focus while the
     * layout is dirty. This prevents the scroll from being wrong if the child has not been
     * laid out before requesting focus.
     */
    private var mChildToScrollTo: View? = null

    /**
     * True if the user is currently dragging this ScrollView around. This is
     * not the same as 'is being flinged', which can be checked by
     * mScroller.isFinished() (flinging begins when the user lifts his finger).
     */
    private var mIsBeingDragged = false

    /**
     * Determines speed during touch scrolling
     */
    private var mVelocityTracker: VelocityTracker? = null

    /**
     * When set to true, the scroll view measure its child to make it fill the currently
     * visible area.
     */
    private var mFillViewport = false

    /**
     * @return Whether arrow scrolling will animate its transition.
     */
    /**
     * Set whether arrow scrolling will animate its transition.
     *
     * @param smoothScrollingEnabled whether arrow scrolling will animate its transition
     */
    /**
     * Whether arrow scrolling is animated.
     */
    var isSmoothScrollingEnabled: Boolean = true

    private var mTouchSlop = 0
    private var mMinimumVelocity = 0
    private var mMaximumVelocity = 0

    /**
     * ID of the active pointer. This is used to retain consistency during
     * drags/flings if multiple pointers are used.
     */
    private var mActivePointerId = INVALID_POINTER

    /**
     * Used during scrolling to retrieve the new offset within the window.
     */
    private val mScrollOffset = IntArray(2)
    private val mScrollConsumed = IntArray(2)
    private var mNestedXOffset = 0
    private var mNestedYOffset = 0

    private var mLastScrollerX = 0
    private var mLastScrollerY = 0

    private var mSavedState: SavedState? = null

    private val mParentHelper: NestedScrollingParentHelper
    private val mChildHelper: NestedScrollingChildHelper

    private var mHorizontalScrollFactor = 0f
    private var mVerticalScrollFactor = 0f

    private var mOnScrollChangeListener: OnScrollChangeListener? = null

    var isFillViewport: Boolean
        /**
         * Indicates whether this ScrollView's content is stretched to fill the viewport.
         *
         * @return True if the content fills the viewport, false otherwise.
         * @attr name android:fillViewport
         */
        get() = mFillViewport
        /**
         * Set whether this ScrollView should stretch its content height to fill the viewport or not.
         *
         * @param fillViewport True to stretch the content's height to the viewport's
         * boundaries, false otherwise.
         * @attr name android:fillViewport
         */
        set(fillViewport) {
            if (fillViewport != mFillViewport) {
                mFillViewport = fillViewport
                requestLayout()
            }
        }

    init {
        initScrollView()

        val a = context.obtainStyledAttributes(
            attrs, SCROLLVIEW_STYLEABLE, defStyleAttr, 0
        )

        isFillViewport = a.getBoolean(0, false)

        a.recycle()

        mParentHelper = NestedScrollingParentHelper(this)
        mChildHelper = NestedScrollingChildHelper(this)

        // ...because why else would you be using this widget?
        isNestedScrollingEnabled = true

        ViewCompat.setAccessibilityDelegate(this, ACCESSIBILITY_DELEGATE)
    }

    // NestedScrollingChild3
    override fun dispatchNestedScroll(
        dxConsumed: Int, dyConsumed: Int, dxUnconsumed: Int,
        dyUnconsumed: Int, offsetInWindow: IntArray?, type: Int, consumed: IntArray
    ) {
        mChildHelper.dispatchNestedScroll(
            dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed,
            offsetInWindow, type, consumed
        )
    }

    // NestedScrollingChild2
    override fun startNestedScroll(axes: Int, type: Int): Boolean {
        return mChildHelper.startNestedScroll(axes, type)
    }

    override fun stopNestedScroll(type: Int) {
        mChildHelper.stopNestedScroll(type)
    }

    override fun hasNestedScrollingParent(type: Int): Boolean {
        return mChildHelper.hasNestedScrollingParent(type)
    }

    override fun dispatchNestedScroll(
        dxConsumed: Int, dyConsumed: Int, dxUnconsumed: Int,
        dyUnconsumed: Int, offsetInWindow: IntArray?, type: Int
    ): Boolean {
        return mChildHelper.dispatchNestedScroll(
            dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed,
            offsetInWindow, type
        )
    }

    override fun dispatchNestedPreScroll(
        dx: Int, dy: Int, consumed: IntArray?, offsetInWindow: IntArray?,
        type: Int
    ): Boolean {
        return mChildHelper.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow, type)
    }

    // NestedScrollingChild
    override fun setNestedScrollingEnabled(enabled: Boolean) {
        mChildHelper.isNestedScrollingEnabled = enabled
    }

    override fun isNestedScrollingEnabled(): Boolean {
        return mChildHelper.isNestedScrollingEnabled
    }

    override fun startNestedScroll(axes: Int): Boolean {
        return startNestedScroll(axes, ViewCompat.TYPE_TOUCH)
    }

    override fun stopNestedScroll() {
        stopNestedScroll(ViewCompat.TYPE_TOUCH)
    }

    override fun hasNestedScrollingParent(): Boolean {
        return hasNestedScrollingParent(ViewCompat.TYPE_TOUCH)
    }

    override fun dispatchNestedScroll(
        dxConsumed: Int, dyConsumed: Int, dxUnconsumed: Int,
        dyUnconsumed: Int, offsetInWindow: IntArray?
    ): Boolean {
        return mChildHelper.dispatchNestedScroll(
            dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed,
            offsetInWindow
        )
    }

    override fun dispatchNestedPreScroll(dx: Int, dy: Int, consumed: IntArray?, offsetInWindow: IntArray?): Boolean {
        return dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow, ViewCompat.TYPE_TOUCH)
    }

    override fun dispatchNestedFling(velocityX: Float, velocityY: Float, consumed: Boolean): Boolean {
        return mChildHelper.dispatchNestedFling(velocityX, velocityY, consumed)
    }

    override fun dispatchNestedPreFling(velocityX: Float, velocityY: Float): Boolean {
        return mChildHelper.dispatchNestedPreFling(velocityX, velocityY)
    }

    // NestedScrollingParent3
    override fun onNestedScroll(
        target: View, dxConsumed: Int, dyConsumed: Int,
        dxUnconsumed: Int, dyUnconsumed: Int, type: Int, consumed: IntArray
    ) {
        onNestedScrollInternal(dxUnconsumed, dyUnconsumed, type, consumed)
    }

    private fun onNestedScrollInternal(dxUnconsumed: Int, dyUnconsumed: Int, type: Int, consumed: IntArray?) {
        val oldScrollX = scrollX
        val oldScrollY = scrollY
        scrollBy(dxUnconsumed, dyUnconsumed)
        val myConsumedX = scrollX - oldScrollX
        val myConsumedY = scrollY - oldScrollY

        if (consumed != null) {
            consumed[0] += myConsumedX
            consumed[1] += myConsumedY
        }
        val myUnconsumedX = dxUnconsumed - myConsumedX
        val myUnconsumedY = dyUnconsumed - myConsumedY

        mChildHelper.dispatchNestedScroll(myConsumedX, myConsumedY, myUnconsumedX, myUnconsumedY, null, type, consumed)
    }

    // NestedScrollingParent2
    override fun onStartNestedScroll(
        child: View, target: View, axes: Int,
        type: Int
    ): Boolean {
        return (axes and (ViewCompat.SCROLL_AXIS_HORIZONTAL or ViewCompat.SCROLL_AXIS_VERTICAL)) != 0
    }

    override fun onNestedScrollAccepted(
        child: View, target: View, axes: Int,
        type: Int
    ) {
        mParentHelper.onNestedScrollAccepted(child, target, axes, type)
        startNestedScroll(ViewCompat.SCROLL_AXIS_HORIZONTAL or ViewCompat.SCROLL_AXIS_VERTICAL, type)
    }

    override fun onStopNestedScroll(target: View, type: Int) {
        mParentHelper.onStopNestedScroll(target, type)
        stopNestedScroll(type)
    }

    override fun onNestedScroll(
        target: View, dxConsumed: Int, dyConsumed: Int,
        dxUnconsumed: Int, dyUnconsumed: Int, type: Int
    ) {
        onNestedScrollInternal(dxUnconsumed, dyUnconsumed, type, null)
    }

    override fun onNestedPreScroll(
        target: View, dx: Int, dy: Int, consumed: IntArray,
        type: Int
    ) {
        dispatchNestedPreScroll(dx, dy, consumed, null, type)
    }

    // NestedScrollingParent
    override fun onStartNestedScroll(
        child: View, target: View, nestedScrollAxes: Int
    ): Boolean {
        return onStartNestedScroll(child, target, nestedScrollAxes, ViewCompat.TYPE_TOUCH)
    }

    override fun onNestedScrollAccepted(
        child: View, target: View, nestedScrollAxes: Int
    ) {
        onNestedScrollAccepted(child, target, nestedScrollAxes, ViewCompat.TYPE_TOUCH)
    }

    override fun onStopNestedScroll(target: View) {
        onStopNestedScroll(target, ViewCompat.TYPE_TOUCH)
    }

    override fun onNestedScroll(
        target: View, dxConsumed: Int, dyConsumed: Int,
        dxUnconsumed: Int, dyUnconsumed: Int
    ) {
        onNestedScrollInternal(dxUnconsumed, dyUnconsumed, ViewCompat.TYPE_TOUCH, null)
    }

    override fun onNestedPreScroll(target: View, dx: Int, dy: Int, consumed: IntArray) {
        onNestedPreScroll(target, dx, dy, consumed, ViewCompat.TYPE_TOUCH)
    }

    override fun onNestedFling(
        target: View, velocityX: Float, velocityY: Float, consumed: Boolean
    ): Boolean {
        if (!consumed) {
            dispatchNestedFling(velocityX, velocityY, true)
            fling(velocityX.toInt(), velocityY.toInt())
            return true
        }
        return false
    }

    override fun onNestedPreFling(target: View, velocityX: Float, velocityY: Float): Boolean {
        return dispatchNestedPreFling(velocityX, velocityY)
    }

    override fun getNestedScrollAxes(): Int {
        return mParentHelper.nestedScrollAxes
    }

    // ScrollView import
    override fun shouldDelayChildPressedState(): Boolean {
        return true
    }

    override fun getLeftFadingEdgeStrength(): Float {
        if (childCount == 0) {
            return 0.0f
        }

        val length = horizontalFadingEdgeLength
        val scrollX = scrollX
        if (scrollX < length) {
            return scrollX / length.toFloat()
        }

        return 1.0f
    }

    override fun getTopFadingEdgeStrength(): Float {
        if (childCount == 0) {
            return 0.0f
        }

        val length = verticalFadingEdgeLength
        val scrollY = scrollY
        if (scrollY < length) {
            return scrollY / length.toFloat()
        }

        return 1.0f
    }

    override fun getRightFadingEdgeStrength(): Float {
        if (childCount == 0) {
            return 0.0f
        }

        val child = getChildAt(0)
        val lp = child.layoutParams as LayoutParams
        val length = horizontalFadingEdgeLength
        val rightEdge = width - paddingRight
        val span = child.right + lp.rightMargin - scrollX - rightEdge
        if (span < length) {
            return span / length.toFloat()
        }

        return 1.0f
    }

    override fun getBottomFadingEdgeStrength(): Float {
        if (childCount == 0) {
            return 0.0f
        }

        val child = getChildAt(0)
        val lp = child.layoutParams as LayoutParams
        val length = verticalFadingEdgeLength
        val bottomEdge = height - paddingBottom
        val span = child.bottom + lp.bottomMargin - scrollY - bottomEdge
        if (span < length) {
            return span / length.toFloat()
        }

        return 1.0f
    }

    val maxScrollAmountX: Int
        /**
         * @return The maximum amount this scroll view will scroll in response to
         * an arrow event.
         */
        get() = (MAX_SCROLL_FACTOR * width).toInt()

    val maxScrollAmountY: Int
        /**
         * @return The maximum amount this scroll view will scroll in response to
         * an arrow event.
         */
        get() = (MAX_SCROLL_FACTOR * height).toInt()

    private fun initScrollView() {
        mScroller = OverScroller(context)
        isFocusable = true
        descendantFocusability = FOCUS_AFTER_DESCENDANTS
        setWillNotDraw(false)
        val configuration = ViewConfiguration.get(context)
        mTouchSlop = configuration.scaledTouchSlop
        mMinimumVelocity = configuration.scaledMinimumFlingVelocity
        mMaximumVelocity = configuration.scaledMaximumFlingVelocity
    }

    override fun addView(child: View) {
        check(childCount <= 0) { "ScrollView can host only one direct child" }

        super.addView(child)
    }

    override fun addView(child: View, index: Int) {
        check(childCount <= 0) { "ScrollView can host only one direct child" }

        super.addView(child, index)
    }

    override fun addView(child: View, params: ViewGroup.LayoutParams) {
        check(childCount <= 0) { "ScrollView can host only one direct child" }

        super.addView(child, params)
    }

    override fun addView(child: View, index: Int, params: ViewGroup.LayoutParams) {
        check(childCount <= 0) { "ScrollView can host only one direct child" }

        super.addView(child, index, params)
    }

    /**
     * Register a callback to be invoked when the scroll X or Y positions of
     * this view change.
     *
     * This version of the method works on all versions of Android, back to API v4.
     *
     * @param l The listener to notify when the scroll X or Y position changes.
     * @see android.view.View.getScrollX
     * @see android.view.View.getScrollY
     */
    fun setOnScrollChangeListener(l: OnScrollChangeListener?) {
        mOnScrollChangeListener = l
    }

    /**
     * @return Returns true this ScrollView can be scrolled
     */
    private fun canScrollX(): Boolean {
        if (childCount > 0) {
            val child = getChildAt(0)
            val lp = child.layoutParams as LayoutParams
            val childSize = child.width + lp.leftMargin + lp.rightMargin
            val parentSpace = width - paddingLeft - paddingRight
            return childSize > parentSpace
        }
        return false
    }

    /**
     * @return Returns true this ScrollView can be scrolled
     */
    private fun canScrollY(): Boolean {
        if (childCount > 0) {
            val child = getChildAt(0)
            val lp = child.layoutParams as LayoutParams
            val childSize = child.height + lp.topMargin + lp.bottomMargin
            val parentSpace = height - paddingTop - paddingBottom
            return childSize > parentSpace
        }
        return false
    }

    override fun onScrollChanged(l: Int, t: Int, oldl: Int, oldt: Int) {
        super.onScrollChanged(l, t, oldl, oldt)

        if (mOnScrollChangeListener != null) {
            mOnScrollChangeListener!!.onScrollChange(this, l, t, oldl, oldt)
        }
    }

    var lockX: Boolean = false
    var lockY: Boolean = false

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        if (!mFillViewport) {
            return
        }

        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        if ((widthMode == MeasureSpec.UNSPECIFIED || lockX) && (heightMode == MeasureSpec.UNSPECIFIED || lockY)) {
            return
        }

        if (childCount > 0) {
            val child = getChildAt(0)
            val lp = child.layoutParams as LayoutParams

            val childWidth = child.measuredWidth
            val parentSpaceX = (measuredWidth
                    - paddingLeft
                    - paddingRight
                    - lp.leftMargin
                    - lp.rightMargin)
            val childHeight = child.measuredHeight
            val parentSpaceY = (measuredHeight
                    - paddingTop
                    - paddingBottom
                    - lp.topMargin
                    - lp.bottomMargin)

            val childWidthMeasureSpec = if (lockX) getChildMeasureSpec(
                widthMeasureSpec,
                getPaddingLeft() + getPaddingRight() + lp.leftMargin + lp.rightMargin,
                lp.width
            ) else MeasureSpec.makeMeasureSpec(
                max(parentSpaceX.toDouble(), childWidth.toDouble()).toInt(),
                MeasureSpec.EXACTLY
            )
            val childHeightMeasureSpec = if (lockY) getChildMeasureSpec(
                heightMeasureSpec,
                getPaddingTop() + getPaddingBottom() + lp.topMargin + lp.bottomMargin,
                lp.height
            ) else MeasureSpec.makeMeasureSpec(
                max(parentSpaceY.toDouble(), childHeight.toDouble()).toInt(),
                MeasureSpec.EXACTLY
            )
            child.measure(childWidthMeasureSpec, childHeightMeasureSpec)
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        // Let the focused view and/or our descendants get the key first
        return super.dispatchKeyEvent(event) || executeKeyEvent(event)
    }

    /**
     * You can call this function yourself to have the scroll view perform
     * scrolling from a key event, just as if the event had been dispatched to
     * it by the view hierarchy.
     *
     * @param event The key event to execute.
     * @return Return true if the event was handled, else false.
     */
    fun executeKeyEvent(event: KeyEvent): Boolean {
        mTempRect.setEmpty()

        if (!canScrollX() && !canScrollY()) {
            if (isFocused && event.keyCode != KeyEvent.KEYCODE_BACK) {
                var currentFocused = findFocus()
                if (currentFocused === this) currentFocused = null
                val nextFocused = FocusFinder.getInstance().findNextFocus(
                    this,
                    currentFocused, FOCUS_DOWN
                )
                return nextFocused != null && nextFocused !== this && nextFocused.requestFocus(FOCUS_DOWN)
            }
            return false
        }

        var handled = false
        if (event.action == KeyEvent.ACTION_DOWN) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_DPAD_LEFT -> handled = if (!event.isAltPressed) {
                    arrowScroll(FOCUS_LEFT)
                } else {
                    fullScroll(FOCUS_LEFT)
                }

                KeyEvent.KEYCODE_DPAD_UP -> handled = if (!event.isAltPressed) {
                    arrowScroll(FOCUS_UP)
                } else {
                    fullScroll(FOCUS_UP)
                }

                KeyEvent.KEYCODE_DPAD_RIGHT -> handled = if (!event.isAltPressed) {
                    arrowScroll(FOCUS_RIGHT)
                } else {
                    fullScroll(FOCUS_RIGHT)
                }

                KeyEvent.KEYCODE_DPAD_DOWN -> handled = if (!event.isAltPressed) {
                    arrowScroll(FOCUS_DOWN)
                } else {
                    fullScroll(FOCUS_DOWN)
                }

                KeyEvent.KEYCODE_SPACE -> pageScroll(if (event.isShiftPressed) FOCUS_UP else FOCUS_DOWN)
            }
        }

        return handled
    }

    private fun inChild(x: Int, y: Int): Boolean {
        if (childCount > 0) {
            val scrollX = scrollX
            val scrollY = scrollY
            val child = getChildAt(0)
            return !(y < child.top - scrollY || y >= child.bottom - scrollY || x < child.left - scrollX || x >= child.right - scrollX)
        }
        return false
    }

    private fun initOrResetVelocityTracker() {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain()
        } else {
            mVelocityTracker!!.clear()
        }
    }

    private fun initVelocityTrackerIfNotExists() {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain()
        }
    }

    private fun recycleVelocityTracker() {
        if (mVelocityTracker != null) {
            mVelocityTracker!!.recycle()
            mVelocityTracker = null
        }
    }

    override fun requestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
        if (disallowIntercept) {
            recycleVelocityTracker()
        }
        super.requestDisallowInterceptTouchEvent(disallowIntercept)
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        /*
         * This method JUST determines whether we want to intercept the motion.
         * If we return true, onMotionEvent will be called and we do the actual
         * scrolling there.
         */

        /*
         * Shortcut the most recurring case: the user is in the dragging
         * state and he is moving his finger.  We want to intercept this
         * motion.
         */

        val action = ev.action
        if ((action == MotionEvent.ACTION_MOVE) && mIsBeingDragged) {
            return true
        }

        when (action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_MOVE -> run label@{
                /*
                                * mIsBeingDragged == false, otherwise the shortcut would have caught it. Check
                                * whether the user has moved far enough from his original down touch.
                                */

                /*
                 * Locally do absolute value. mLastMotionY is set to the y value
                 * of the down event.
                 */
                val activePointerId = mActivePointerId
                if (activePointerId == INVALID_POINTER) {
                    // If we don't have a valid id, the touch down wasn't on content.
                    return@label
                }

                val pointerIndex = ev.findPointerIndex(activePointerId)
                if (pointerIndex == -1) {
                    Log.e(
                        TAG, ("Invalid pointerId=" + activePointerId
                                + " in onInterceptTouchEvent")
                    )
                    return@label
                }

                val x = ev.getX(pointerIndex).toInt()
                val y = ev.getY(pointerIndex).toInt()
                val xDiff = abs((x - mLastMotionX).toDouble()).toInt()
                val yDiff = abs((y - mLastMotionY).toDouble()).toInt()
                val doX = xDiff > mTouchSlop
                        && (nestedScrollAxes and ViewCompat.SCROLL_AXIS_HORIZONTAL) == 0 && !lockX
                val doY = yDiff > mTouchSlop
                        && (nestedScrollAxes and ViewCompat.SCROLL_AXIS_VERTICAL) == 0 && !lockY
                if (doX || doY) {
                    mIsBeingDragged = true
                    mLastMotionX = x
                    mLastMotionY = y
                    initVelocityTrackerIfNotExists()
                    mVelocityTracker!!.addMovement(ev)
                    mNestedXOffset = 0
                    mNestedYOffset = 0
                    val parent = parent
                    parent?.requestDisallowInterceptTouchEvent(true)
                }
            }

            MotionEvent.ACTION_DOWN -> run label@{
                val x = ev.x.toInt()
                val y = ev.y.toInt()
                if (!inChild(x, y)) {
                    mIsBeingDragged = false
                    recycleVelocityTracker()
                    return@label
                }

                /*
                 * Remember location of down touch.
                 * ACTION_DOWN always refers to pointer index 0.
                 */
                mLastMotionX = x
                mLastMotionY = y
                mActivePointerId = ev.getPointerId(0)

                initOrResetVelocityTracker()
                mVelocityTracker!!.addMovement(ev)
                /*
                 * If being flinged and user touches the screen, initiate drag;
                 * otherwise don't. mScroller.isFinished should be false when
                 * being flinged. We need to call computeScrollOffset() first so that
                 * isFinished() is correct.
                 */
                mScroller!!.computeScrollOffset()
                mIsBeingDragged = !mScroller!!.isFinished
                startNestedScroll(
                    ViewCompat.SCROLL_AXIS_HORIZONTAL or ViewCompat.SCROLL_AXIS_VERTICAL,
                    ViewCompat.TYPE_TOUCH
                )
            }

            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
                /* Release the drag */
                mIsBeingDragged = false
                mActivePointerId = INVALID_POINTER
                recycleVelocityTracker()
                if (mScroller!!.springBack(scrollX, scrollY, 0, scrollRangeX, 0, scrollRangeY)) {
                    ViewCompat.postInvalidateOnAnimation(this)
                }
                stopNestedScroll(ViewCompat.TYPE_TOUCH)
            }

            MotionEvent.ACTION_POINTER_UP -> onSecondaryPointerUp(ev)
        }

        /*
         * The only time we want to intercept motion events is if we are in the
         * drag mode.
         */
        return mIsBeingDragged
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        initVelocityTrackerIfNotExists()

        val actionMasked = ev.actionMasked

        if (actionMasked == MotionEvent.ACTION_DOWN) {
            mNestedXOffset = 0
            mNestedYOffset = 0
        }

        val vtev = MotionEvent.obtain(ev)
        vtev.offsetLocation(mNestedXOffset.toFloat(), mNestedYOffset.toFloat())

        when (actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (childCount == 0) {
                    return false
                }
                if (((!mScroller!!.isFinished).also { mIsBeingDragged = it })) {
                    val parent = parent
                    parent?.requestDisallowInterceptTouchEvent(true)
                }

                /*
                 * If being flinged and user touches, stop the fling. isFinished
                 * will be false if being flinged.
                 */
                if (!mScroller!!.isFinished) {
                    abortAnimatedScroll()
                }

                // Remember where the motion event started
                mLastMotionX = ev.x.toInt()
                mLastMotionY = ev.y.toInt()
                mActivePointerId = ev.getPointerId(0)
                startNestedScroll(
                    ViewCompat.SCROLL_AXIS_HORIZONTAL or ViewCompat.SCROLL_AXIS_VERTICAL,
                    ViewCompat.TYPE_TOUCH
                )
            }

            MotionEvent.ACTION_MOVE -> run label@{
                val activePointerIndex = ev.findPointerIndex(mActivePointerId)
                if (activePointerIndex == -1) {
                    Log.e(
                        TAG,
                        "Invalid pointerId=$mActivePointerId in onTouchEvent"
                    )
                    return@label
                }

                val x = ev.getX(activePointerIndex).toInt()
                val y = ev.getY(activePointerIndex).toInt()
                var deltaX = mLastMotionX - x
                var deltaY = mLastMotionY - y
                if (!mIsBeingDragged && (abs(deltaY.toDouble()) > mTouchSlop && !lockY || abs(deltaX.toDouble()) > mTouchSlop && !lockX)) {
                    val parent = parent
                    parent?.requestDisallowInterceptTouchEvent(true)
                    mIsBeingDragged = true
                    if (deltaX > 0) {
                        deltaX -= mTouchSlop
                    } else {
                        deltaX += mTouchSlop
                    }
                    if (deltaY > 0) {
                        deltaY -= mTouchSlop
                    } else {
                        deltaY += mTouchSlop
                    }
                }
                if (mIsBeingDragged) {
                    // Start with nested pre scrolling
                    if (dispatchNestedPreScroll(
                            deltaX, deltaY, mScrollConsumed, mScrollOffset,
                            ViewCompat.TYPE_TOUCH
                        )
                    ) {
                        deltaX -= mScrollConsumed[0]
                        deltaY -= mScrollConsumed[1]
                        mNestedXOffset += mScrollOffset[0]
                        mNestedYOffset += mScrollOffset[1]
                    }

                    // Scroll to follow the motion event
                    mLastMotionX = x - mScrollOffset[0]
                    mLastMotionY = y - mScrollOffset[1]

                    val oldX = scrollX
                    val oldY = scrollY
                    val rangeX = scrollRangeX
                    val rangeY = scrollRangeY
                    val overscrollMode = overScrollMode
                    val canOverscrollX = overscrollMode == OVER_SCROLL_ALWAYS
                            || (overscrollMode == OVER_SCROLL_IF_CONTENT_SCROLLS && rangeX > 0)
                    val canOverscrollY = overscrollMode == OVER_SCROLL_ALWAYS
                            || (overscrollMode == OVER_SCROLL_IF_CONTENT_SCROLLS && rangeY > 0)

                    // Calling overScrollByCompat will call onOverScrolled, which
                    // calls onScrollChanged if applicable.
                    if (overScrollByCompat(
                            deltaX, deltaY, scrollX, scrollY, rangeX, rangeY, 0,
                            0, true
                        ) && !hasNestedScrollingParent(ViewCompat.TYPE_TOUCH)
                    ) {
                        // Break our velocity if we hit a scroll barrier.
                        mVelocityTracker!!.clear()
                    }

                    val scrolledDeltaX = scrollX - oldX
                    val scrolledDeltaY = scrollY - oldY
                    val unconsumedX = deltaX - scrolledDeltaX
                    val unconsumedY = deltaY - scrolledDeltaY

                    mScrollConsumed[0] = 0
                    mScrollConsumed[1] = 0

                    dispatchNestedScroll(
                        scrolledDeltaX, scrolledDeltaY, unconsumedX, unconsumedY, mScrollOffset,
                        ViewCompat.TYPE_TOUCH, mScrollConsumed
                    )

                    mLastMotionX -= mScrollOffset[0]
                    mLastMotionY -= mScrollOffset[1]
                    mNestedXOffset += mScrollOffset[0]
                    mNestedYOffset += mScrollOffset[1]

                    if (canOverscrollX) {
                        deltaX -= mScrollConsumed[0]
                        ensureGlows()
                        val pulledToX = oldX + deltaX
                        if (pulledToX < 0) {
                            EdgeEffectCompat.onPull(
                                mEdgeGlowLeft!!, deltaX.toFloat() / width,
                                1f - ev.getY(activePointerIndex) / height
                            )
                            if (!mEdgeGlowRight!!.isFinished) {
                                mEdgeGlowRight!!.onRelease()
                            }
                        } else if (pulledToX > rangeX) {
                            EdgeEffectCompat.onPull(
                                mEdgeGlowRight!!, deltaX.toFloat() / width,
                                ev.getY(activePointerIndex)
                                        / height
                            )
                            if (!mEdgeGlowLeft!!.isFinished) {
                                mEdgeGlowLeft!!.onRelease()
                            }
                        }
                    }
                    if (canOverscrollY) {
                        deltaY -= mScrollConsumed[1]
                        ensureGlows()
                        val pulledToY = oldY + deltaY
                        if (pulledToY < 0) {
                            EdgeEffectCompat.onPull(
                                mEdgeGlowTop!!, deltaY.toFloat() / height,
                                ev.getX(activePointerIndex) / width
                            )
                            if (!mEdgeGlowBottom!!.isFinished) {
                                mEdgeGlowBottom!!.onRelease()
                            }
                        } else if (pulledToY > rangeY) {
                            EdgeEffectCompat.onPull(
                                mEdgeGlowBottom!!, deltaY.toFloat() / height,
                                1f - ev.getX(activePointerIndex)
                                        / width
                            )
                            if (!mEdgeGlowTop!!.isFinished) {
                                mEdgeGlowTop!!.onRelease()
                            }
                        }
                    }
                    if (canOverscrollX || canOverscrollY) {
                        if (mEdgeGlowTop != null
                            && (!mEdgeGlowLeft!!.isFinished || !mEdgeGlowTop!!.isFinished || !mEdgeGlowRight!!.isFinished || !mEdgeGlowBottom!!.isFinished)
                        ) {
                            ViewCompat.postInvalidateOnAnimation(this)
                        }
                    }
                }
            }

            MotionEvent.ACTION_UP -> {
                val velocityTracker = mVelocityTracker
                velocityTracker!!.computeCurrentVelocity(1000, mMaximumVelocity.toFloat())
                val initialVelocityX = velocityTracker.getXVelocity(mActivePointerId).toInt()
                val initialVelocityY = velocityTracker.getYVelocity(mActivePointerId).toInt()
                if ((abs(initialVelocityX.toDouble()) >= mMinimumVelocity) || (abs(initialVelocityY.toDouble()) >= mMinimumVelocity)) {
                    if (!dispatchNestedPreFling(-initialVelocityX.toFloat(), -initialVelocityY.toFloat())) {
                        dispatchNestedFling(-initialVelocityX.toFloat(), -initialVelocityY.toFloat(), true)
                        fling(-initialVelocityX, -initialVelocityY)
                    }
                } else if (mScroller!!.springBack(
                        scrollX, scrollY,
                        0, scrollRangeX,
                        0, scrollRangeY
                    )
                ) {
                    ViewCompat.postInvalidateOnAnimation(this)
                }
                mActivePointerId = INVALID_POINTER
                endDrag()
            }

            MotionEvent.ACTION_CANCEL -> {
                if (mIsBeingDragged && childCount > 0) {
                    if (mScroller!!.springBack(scrollX, scrollY, 0, scrollRangeX, 0, scrollRangeY)) {
                        ViewCompat.postInvalidateOnAnimation(this)
                    }
                }
                mActivePointerId = INVALID_POINTER
                endDrag()
            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                val index = ev.actionIndex
                mLastMotionX = ev.getX(index).toInt()
                mLastMotionY = ev.getY(index).toInt()
                mActivePointerId = ev.getPointerId(index)
            }

            MotionEvent.ACTION_POINTER_UP -> {
                onSecondaryPointerUp(ev)
                mLastMotionX = ev.getX(ev.findPointerIndex(mActivePointerId)).toInt()
                mLastMotionY = ev.getY(ev.findPointerIndex(mActivePointerId)).toInt()
            }
        }

        if (mVelocityTracker != null) {
            mVelocityTracker!!.addMovement(vtev)
        }
        vtev.recycle()

        return true
    }

    private fun onSecondaryPointerUp(ev: MotionEvent) {
        val pointerIndex = ev.actionIndex
        val pointerId = ev.getPointerId(pointerIndex)
        if (pointerId == mActivePointerId) {
            // This was our active pointer going up. Choose a new
            // active pointer and adjust accordingly.
            // TODO: Make this decision more intelligent.
            val newPointerIndex = if (pointerIndex == 0) 1 else 0
            mLastMotionX = ev.getX(newPointerIndex).toInt()
            mLastMotionY = ev.getY(newPointerIndex).toInt()
            mActivePointerId = ev.getPointerId(newPointerIndex)
            if (mVelocityTracker != null) {
                mVelocityTracker!!.clear()
            }
        }
    }

    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        if ((event.source and InputDeviceCompat.SOURCE_CLASS_POINTER) != 0) {
            when (event.action) {
                MotionEvent.ACTION_SCROLL -> {
                    if (!mIsBeingDragged) {
                        val hscroll = event.getAxisValue(MotionEvent.AXIS_HSCROLL)
                        val vscroll = event.getAxisValue(MotionEvent.AXIS_VSCROLL)
                        if (hscroll != 0f || vscroll != 0f) {
                            val deltaX = (hscroll * horizontalScrollFactorCompat).toInt()
                            val deltaY = (vscroll * verticalScrollFactorCompat).toInt()
                            val rangeX = scrollRangeX
                            val rangeY = scrollRangeY
                            val oldScrollX = scrollX
                            val oldScrollY = scrollY
                            var newScrollX = oldScrollX - deltaX
                            var newScrollY = oldScrollY - deltaY
                            if (newScrollX < 0) {
                                newScrollX = 0
                            } else if (newScrollX > rangeX) {
                                newScrollX = rangeX
                            }
                            if (newScrollY < 0) {
                                newScrollY = 0
                            } else if (newScrollY > rangeY) {
                                newScrollY = rangeY
                            }
                            if (newScrollX != oldScrollX || newScrollY != oldScrollY) {
                                super.scrollTo(newScrollX, newScrollY)
                                return true
                            }
                        }
                    }
                }
            }
        }
        return false
    }

    private val horizontalScrollFactorCompat: Float
        get() {
            if (mHorizontalScrollFactor == 0f) {
                val outValue = TypedValue()
                val context = context
                check(
                    context.theme.resolveAttribute(
                        R.attr.listPreferredItemHeight, outValue, true
                    )
                ) { "Expected theme to define listPreferredItemHeight." }
                mHorizontalScrollFactor = outValue.getDimension(
                    context.resources.displayMetrics
                )
            }
            return mHorizontalScrollFactor
        }

    private val verticalScrollFactorCompat: Float
        get() {
            if (mVerticalScrollFactor == 0f) {
                val outValue = TypedValue()
                val context = context
                check(
                    context.theme.resolveAttribute(
                        R.attr.listPreferredItemHeight, outValue, true
                    )
                ) { "Expected theme to define listPreferredItemHeight." }
                mVerticalScrollFactor = outValue.getDimension(
                    context.resources.displayMetrics
                )
            }
            return mVerticalScrollFactor
        }

    override fun onOverScrolled(
        scrollX: Int, scrollY: Int,
        clampedX: Boolean, clampedY: Boolean
    ) {
        super.scrollTo(scrollX, scrollY)
    }

    fun overScrollByCompat(
        deltaX: Int, deltaY: Int,
        scrollX: Int, scrollY: Int,
        scrollRangeX: Int, scrollRangeY: Int,
        maxOverScrollX: Int, maxOverScrollY: Int,
        isTouchEvent: Boolean
    ): Boolean {
        var maxOverScrollX = maxOverScrollX
        var maxOverScrollY = maxOverScrollY
        val overScrollMode = overScrollMode
        val canScrollHorizontal =
            computeHorizontalScrollRange() > computeHorizontalScrollExtent()
        val canScrollVertical =
            computeVerticalScrollRange() > computeVerticalScrollExtent()
        val overScrollHorizontal = overScrollMode == OVER_SCROLL_ALWAYS
                || (overScrollMode == OVER_SCROLL_IF_CONTENT_SCROLLS && canScrollHorizontal)
        val overScrollVertical = overScrollMode == OVER_SCROLL_ALWAYS
                || (overScrollMode == OVER_SCROLL_IF_CONTENT_SCROLLS && canScrollVertical)

        var newScrollX = scrollX + deltaX
        if (!overScrollHorizontal) {
            maxOverScrollX = 0
        }

        var newScrollY = scrollY + deltaY
        if (!overScrollVertical) {
            maxOverScrollY = 0
        }

        // Clamp values if at the limits and record
        val left = -maxOverScrollX
        val right = maxOverScrollX + scrollRangeX
        val top = -maxOverScrollY
        val bottom = maxOverScrollY + scrollRangeY

        var clampedX = false
        if (newScrollX > right) {
            newScrollX = right
            clampedX = true
        } else if (newScrollX < left) {
            newScrollX = left
            clampedX = true
        }

        var clampedY = false
        if (newScrollY > bottom) {
            newScrollY = bottom
            clampedY = true
        } else if (newScrollY < top) {
            newScrollY = top
            clampedY = true
        }

        if ((clampedX && clampedY) && !hasNestedScrollingParent(ViewCompat.TYPE_NON_TOUCH)) {
            mScroller!!.springBack(newScrollX, newScrollY, 0, this.scrollRangeX, 0, this.scrollRangeY)
        }

        onOverScrolled(newScrollX, newScrollY, clampedX, clampedY)

        return clampedX && clampedY
    }

    val scrollRangeX: Int
        get() {
            var scrollRange = 0
            if (childCount > 0) {
                val child = getChildAt(0)
                val lp = child.layoutParams as LayoutParams
                val childSize = child.width + lp.leftMargin + lp.rightMargin
                val parentSpace = width - paddingLeft - paddingRight
                scrollRange = max(0.0, (childSize - parentSpace).toDouble()).toInt()
            }
            return scrollRange
        }

    val scrollRangeY: Int
        get() {
            var scrollRange = 0
            if (childCount > 0) {
                val child = getChildAt(0)
                val lp = child.layoutParams as LayoutParams
                val childSize = child.height + lp.topMargin + lp.bottomMargin
                val parentSpace = height - paddingTop - paddingBottom
                scrollRange = max(0.0, (childSize - parentSpace).toDouble()).toInt()
            }
            return scrollRange
        }

    /**
     *
     *
     * Finds the next focusable component that fits in the specified bounds.
     *
     *
     * @param topFocus look for a candidate is the one at the top of the bounds
     * if topFocus is true, or at the bottom of the bounds if topFocus is
     * false
     * @param top the top offset of the bounds in which a focusable must be
     * found
     * @param bottom the bottom offset of the bounds in which a focusable must
     * be found
     * @return the next focusable component in the bounds or null if none can
     * be found
     */
    private fun findFocusableViewInBounds(topFocus: Boolean, top: Int, bottom: Int): View? {
        val focusables: List<View> = getFocusables(FOCUS_FORWARD)
        var focusCandidate: View? = null

        /*
         * A fully contained focusable is one where its top is below the bound's
         * top, and its bottom is above the bound's bottom. A partially
         * contained focusable is one where some part of it is within the
         * bounds, but it also has some part that is not within bounds.  A fully contained
         * focusable is preferred to a partially contained focusable.
         */
        var foundFullyContainedFocusable = false

        val count = focusables.size
        for (i in 0..<count) {
            val view = focusables[i]
            val viewTop = view.top
            val viewBottom = view.bottom

            if (top < viewBottom && viewTop < bottom) {
                /*
                 * the focusable is in the target area, it is a candidate for
                 * focusing
                 */

                val viewIsFullyContained = (top < viewTop) && (viewBottom < bottom)

                if (focusCandidate == null) {
                    /* No candidate, take this one */
                    focusCandidate = view
                    foundFullyContainedFocusable = viewIsFullyContained
                } else {
                    val viewIsCloserToBoundary =
                        (topFocus && viewTop < focusCandidate.top)
                                || (!topFocus && viewBottom > focusCandidate.bottom)

                    if (foundFullyContainedFocusable) {
                        if (viewIsFullyContained && viewIsCloserToBoundary) {
                            /*
                             * We're dealing with only fully contained views, so
                             * it has to be closer to the boundary to beat our
                             * candidate
                             */
                            focusCandidate = view
                        }
                    } else {
                        if (viewIsFullyContained) {
                            /* Any fully contained view beats a partially contained view */
                            focusCandidate = view
                            foundFullyContainedFocusable = true
                        } else if (viewIsCloserToBoundary) {
                            /*
                             * Partially contained view beats another partially
                             * contained view if it's closer
                             */
                            focusCandidate = view
                        }
                    }
                }
            }
        }

        return focusCandidate
    }

    /**
     *
     * Handles scrolling in response to a "page up/down" shortcut press. This
     * method will scroll the view by one page up or down and give the focus
     * to the topmost/bottommost component in the new visible area. If no
     * component is a good candidate for focus, this scrollview reclaims the
     * focus.
     *
     * @param direction the scroll direction: [android.view.View.FOCUS_UP]
     * to go one page up or
     * [android.view.View.FOCUS_DOWN] to go one page down
     * @return true if the key event is consumed by this method, false otherwise
     */
    fun pageScroll(direction: Int): Boolean {
        val down = direction == FOCUS_DOWN
        val height = height

        if (down) {
            mTempRect.top = scrollY + height
            val count = childCount
            if (count > 0) {
                val view = getChildAt(count - 1)
                val lp = view.layoutParams as LayoutParams
                val bottom = view.bottom + lp.bottomMargin + paddingBottom
                if (mTempRect.top + height > bottom) {
                    mTempRect.top = bottom - height
                }
            }
        } else {
            mTempRect.top = scrollY - height
            if (mTempRect.top < 0) {
                mTempRect.top = 0
            }
        }
        mTempRect.bottom = mTempRect.top + height

        return scrollAndFocus(direction, mTempRect.top, mTempRect.bottom)
    }

    /**
     *
     * Handles scrolling in response to a "home/end" shortcut press. This
     * method will scroll the view to the top or bottom and give the focus
     * to the topmost/bottommost component in the new visible area. If no
     * component is a good candidate for focus, this scrollview reclaims the
     * focus.
     *
     * @param direction the scroll direction: [android.view.View.FOCUS_UP]
     * to go the top of the view or
     * [android.view.View.FOCUS_DOWN] to go the bottom
     * @return true if the key event is consumed by this method, false otherwise
     */
    fun fullScroll(direction: Int): Boolean {
        val down = direction == FOCUS_DOWN
        val height = height

        mTempRect.top = 0
        mTempRect.bottom = height

        if (down) {
            val count = childCount
            if (count > 0) {
                val view = getChildAt(count - 1)
                val lp = view.layoutParams as LayoutParams
                mTempRect.bottom = view.bottom + lp.bottomMargin + paddingBottom
                mTempRect.top = mTempRect.bottom - height
            }
        }

        return scrollAndFocus(direction, mTempRect.top, mTempRect.bottom)
    }

    /**
     *
     * Scrolls the view to make the area defined by `top` and
     * `bottom` visible. This method attempts to give the focus
     * to a component visible in this area. If no component can be focused in
     * the new visible area, the focus is reclaimed by this ScrollView.
     *
     * @param direction the scroll direction: [android.view.View.FOCUS_UP]
     * to go upward, [android.view.View.FOCUS_DOWN] to downward
     * @param top the top offset of the new area to be made visible
     * @param bottom the bottom offset of the new area to be made visible
     * @return true if the key event is consumed by this method, false otherwise
     */
    private fun scrollAndFocus(direction: Int, top: Int, bottom: Int): Boolean {
        var handled = true

        val height = height
        val containerTop = scrollY
        val containerBottom = containerTop + height
        val up = direction == FOCUS_UP

        var newFocused = findFocusableViewInBounds(up, top, bottom)
        if (newFocused == null) {
            newFocused = this
        }

        if (top >= containerTop && bottom <= containerBottom) {
            handled = false
        } else {
            val deltaY = if (up) (top - containerTop) else (bottom - containerBottom)
            doScroll(0, deltaY)
        }

        if (newFocused !== findFocus()) newFocused.requestFocus(direction)

        return handled
    }

    /**
     * Handle scrolling in response to an arrow click.
     *
     * @param direction The direction corresponding to the arrow key that was
     * pressed
     * @return True if we consumed the event, false otherwise
     */
    fun arrowScroll(direction: Int): Boolean {
        var currentFocused = findFocus()
        if (currentFocused === this) currentFocused = null

        val nextFocused = FocusFinder.getInstance().findNextFocus(this, currentFocused, direction)

        val maxJumpX = maxScrollAmountX
        val maxJumpY = maxScrollAmountY

        if (nextFocused != null && isWithinDeltaOfPage(nextFocused, maxJumpX, width, maxJumpY, height)) {
            nextFocused.getDrawingRect(mTempRect)
            offsetDescendantRectToMyCoords(nextFocused, mTempRect)
            val scrollDeltaX = computeScrollDeltaToGetChildRectOnScreenX(mTempRect)
            val scrollDeltaY = computeScrollDeltaToGetChildRectOnScreenY(mTempRect)
            doScroll(scrollDeltaX, scrollDeltaY)
            nextFocused.requestFocus(direction)
        } else {
            // no new focus
            var scrollDeltaX = maxJumpX
            var scrollDeltaY = maxJumpY

            if (direction == FOCUS_UP && scrollY < scrollDeltaY) {
                scrollDeltaY = scrollY
            } else if (direction == FOCUS_DOWN) {
                if (childCount > 0) {
                    val child = getChildAt(0)
                    val lp = child.layoutParams as LayoutParams
                    val daBottom = child.bottom + lp.bottomMargin
                    val screenBottom = scrollY + height - paddingBottom
                    scrollDeltaY = min((daBottom - screenBottom).toDouble(), maxJumpY.toDouble()).toInt()
                }
            } else if (direction == FOCUS_LEFT && scrollX < scrollDeltaX) {
                scrollDeltaX = scrollX
            } else if (direction == FOCUS_RIGHT) {
                if (childCount > 0) {
                    val child = getChildAt(0)
                    val lp = child.layoutParams as LayoutParams
                    val daRight = child.right + lp.rightMargin
                    val screenRight = scrollX + width - paddingRight
                    scrollDeltaX = min((daRight - screenRight).toDouble(), maxJumpX.toDouble()).toInt()
                }
            }
            if (scrollDeltaX == 0 && scrollDeltaY == 0) {
                return false
            }
            if (direction == FOCUS_RIGHT) {
                doScroll(scrollDeltaX, 0)
            } else if (direction == FOCUS_LEFT) {
                doScroll(-scrollDeltaX, 0)
            } else if (direction == FOCUS_DOWN) {
                doScroll(0, scrollDeltaY)
            } else if (direction == FOCUS_UP) {
                doScroll(0, -scrollDeltaY)
            } else {
                return false
            }
        }

        if (currentFocused != null && currentFocused.isFocused
            && isOffPage(currentFocused)
        ) {
            // previously focused item still has focus and is off screen, give
            // it up (take it back to ourselves)
            // (also, need to temporarily force FOCUS_BEFORE_DESCENDANTS so we are
            // sure to
            // get it)
            val descendantFocusability = descendantFocusability // save
            setDescendantFocusability(FOCUS_BEFORE_DESCENDANTS)
            requestFocus()
            setDescendantFocusability(descendantFocusability) // restore
        }
        return true
    }

    /**
     * @return whether the descendant of this scroll view is scrolled off
     * screen.
     */
    private fun isOffPage(descendant: View): Boolean {
        return !isWithinDeltaOfPage(descendant, 0, width, 0, height)
    }

    /**
     * @return whether the descendant of this scroll view is within delta
     * pixels of being on the screen.
     */
    private fun isWithinDeltaOfPage(descendant: View, deltaX: Int, width: Int, deltaY: Int, height: Int): Boolean {
        descendant.getDrawingRect(mTempRect)
        offsetDescendantRectToMyCoords(descendant, mTempRect)

        return (mTempRect.bottom + deltaY) >= scrollY && (mTempRect.top - deltaY) <= (scrollY + height) && (mTempRect.right + deltaX) >= scrollX && (mTempRect.left - deltaX) <= (scrollX + width)
    }

    /**
     * Smooth scroll by a Y delta
     *
     * @param deltaX the number of pixels to scroll by on the X axis
     * @param deltaY the number of pixels to scroll by on the Y axis
     */
    private fun doScroll(deltaX: Int, deltaY: Int) {
        if (deltaX != 0 || deltaY != 0) {
            if (isSmoothScrollingEnabled) {
                smoothScrollBy(deltaX, deltaY)
            } else {
                scrollBy(deltaX, deltaY)
            }
        }
    }

    /**
     * Like [View.scrollBy], but scroll smoothly instead of immediately.
     *
     * @param dx the number of pixels to scroll by on the X axis
     * @param dy the number of pixels to scroll by on the Y axis
     */
    fun smoothScrollBy(dx: Int, dy: Int) {
        smoothScrollBy(dx, dy, DEFAULT_SMOOTH_SCROLL_DURATION, false)
    }

    /**
     * Like [View.scrollBy], but scroll smoothly instead of immediately.
     *
     * @param dx the number of pixels to scroll by on the X axis
     * @param dy the number of pixels to scroll by on the Y axis
     * @param scrollDurationMs the duration of the smooth scroll operation in milliseconds
     */
    fun smoothScrollBy(dx: Int, dy: Int, scrollDurationMs: Int) {
        smoothScrollBy(dx, dy, scrollDurationMs, false)
    }

    /**
     * Like [View.scrollBy], but scroll smoothly instead of immediately.
     *
     * @param dx the number of pixels to scroll by on the X axis
     * @param dy the number of pixels to scroll by on the Y axis
     * @param scrollDurationMs the duration of the smooth scroll operation in milliseconds
     * @param withNestedScrolling whether to include nested scrolling operations.
     */
    private fun smoothScrollBy(dx: Int, dy: Int, scrollDurationMs: Int, withNestedScrolling: Boolean) {
        var dx = dx
        var dy = dy
        if (childCount == 0) {
            // Nothing to do.
            return
        }
        val duration = AnimationUtils.currentAnimationTimeMillis() - mLastScroll
        if (duration > ANIMATED_SCROLL_GAP) {
            val child = getChildAt(0)
            val lp = child.layoutParams as LayoutParams
            val childWidth = child.width + lp.leftMargin + lp.rightMargin
            val childHeight = child.height + lp.topMargin + lp.bottomMargin
            val parentSpaceX = width - paddingLeft - paddingRight
            val parentSpaceY = height - paddingTop - paddingBottom
            val scrollX = scrollX
            val scrollY = scrollY
            val maxX = max(0.0, (childWidth - parentSpaceX).toDouble()).toInt()
            val maxY = max(0.0, (childHeight - parentSpaceY).toDouble()).toInt()
            dx = (max(0.0, min((scrollX + dx).toDouble(), maxX.toDouble())) - scrollX).toInt()
            dy = (max(0.0, min((scrollY + dy).toDouble(), maxY.toDouble())) - scrollY).toInt()
            mScroller!!.startScroll(scrollX, scrollY, dx, dy, scrollDurationMs)
            runAnimatedScroll(withNestedScrolling)
        } else {
            if (!mScroller!!.isFinished) {
                abortAnimatedScroll()
            }
            scrollBy(dx, dy)
        }
        mLastScroll = AnimationUtils.currentAnimationTimeMillis()
    }

    /**
     * Like [.scrollTo], but scroll smoothly instead of immediately.
     *
     * @param x the position where to scroll on the X axis
     * @param y the position where to scroll on the Y axis
     */
    fun smoothScrollTo(x: Int, y: Int) {
        smoothScrollTo(x, y, DEFAULT_SMOOTH_SCROLL_DURATION, false)
    }

    /**
     * Like [.scrollTo], but scroll smoothly instead of immediately.
     *
     * @param x the position where to scroll on the X axis
     * @param y the position where to scroll on the Y axis
     * @param scrollDurationMs the duration of the smooth scroll operation in milliseconds
     */
    fun smoothScrollTo(x: Int, y: Int, scrollDurationMs: Int) {
        smoothScrollTo(x, y, scrollDurationMs, false)
    }

    /**
     * Like [.scrollTo], but scroll smoothly instead of immediately.
     *
     * @param x the position where to scroll on the X axis
     * @param y the position where to scroll on the Y axis
     * @param withNestedScrolling whether to include nested scrolling operations.
     */
    // This should be considered private, it is package private to avoid a synthetic ancestor.
    fun smoothScrollTo(x: Int, y: Int, withNestedScrolling: Boolean) {
        smoothScrollTo(x, y, DEFAULT_SMOOTH_SCROLL_DURATION, withNestedScrolling)
    }

    /**
     * Like [.scrollTo], but scroll smoothly instead of immediately.
     *
     * @param x the position where to scroll on the X axis
     * @param y the position where to scroll on the Y axis
     * @param scrollDurationMs the duration of the smooth scroll operation in milliseconds
     * @param withNestedScrolling whether to include nested scrolling operations.
     */
    // This should be considered private, it is package private to avoid a synthetic ancestor.
    fun smoothScrollTo(x: Int, y: Int, scrollDurationMs: Int, withNestedScrolling: Boolean) {
        smoothScrollBy(x - scrollX, y - scrollY, scrollDurationMs, withNestedScrolling)
    }

    /**
     *
     * The scroll range of a scroll view is the overall height of all of its
     * children.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    override fun computeVerticalScrollRange(): Int {
        val count = childCount
        val parentSpace = height - paddingBottom - paddingTop
        if (count == 0) {
            return parentSpace
        }

        val child = getChildAt(0)
        val lp = child.layoutParams as LayoutParams
        var scrollRange = child.bottom + lp.bottomMargin
        val scrollY = scrollY
        val overscrollBottom = max(0.0, (scrollRange - parentSpace).toDouble()).toInt()
        if (scrollY < 0) {
            scrollRange -= scrollY
        } else if (scrollY > overscrollBottom) {
            scrollRange += scrollY - overscrollBottom
        }

        return scrollRange
    }

    /** @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    override fun computeVerticalScrollOffset(): Int {
        return max(0.0, super.computeVerticalScrollOffset().toDouble()).toInt()
    }

    /** @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    override fun computeVerticalScrollExtent(): Int {
        return super.computeVerticalScrollExtent()
    }

    /**
     *
     * The scroll range of a scroll view is the overall height of all of its
     * children.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    override fun computeHorizontalScrollRange(): Int {
        val count = childCount
        val parentSpace = width - paddingRight - paddingLeft
        if (count == 0) {
            return parentSpace
        }

        val child = getChildAt(0)
        val lp = child.layoutParams as LayoutParams
        var scrollRange = child.right + lp.rightMargin
        val scrollX = scrollX
        val overscrollRight = max(0.0, (scrollRange - parentSpace).toDouble()).toInt()
        if (scrollX < 0) {
            scrollRange -= scrollX
        } else if (scrollX > overscrollRight) {
            scrollRange += scrollX - overscrollRight
        }

        return scrollRange
    }

    /** @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    override fun computeHorizontalScrollOffset(): Int {
        return max(0.0, super.computeHorizontalScrollOffset().toDouble()).toInt()
    }

    /** @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    override fun computeHorizontalScrollExtent(): Int {
        return super.computeHorizontalScrollExtent()
    }

    override fun measureChild(
        child: View, parentWidthMeasureSpec: Int,
        parentHeightMeasureSpec: Int
    ) {
        val lp = child.layoutParams
        child.measure(
            if(lockX)
                getChildMeasureSpec(parentWidthMeasureSpec, paddingLeft + paddingRight, lp.width)
            else MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
            if(lockY)
                getChildMeasureSpec(parentHeightMeasureSpec, paddingTop + paddingBottom, lp.height)
            else MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
        )
    }

    override fun measureChildWithMargins(
        child: View, parentWidthMeasureSpec: Int, widthUsed: Int,
        parentHeightMeasureSpec: Int, heightUsed: Int
    ) {
        val lp = child.layoutParams as MarginLayoutParams
        child.measure(
            if(lockX)
                getChildMeasureSpec(parentWidthMeasureSpec, paddingLeft + paddingRight + lp.leftMargin + lp.rightMargin + widthUsed, lp.width)
            else MeasureSpec.makeMeasureSpec(lp.leftMargin + lp.rightMargin, MeasureSpec.UNSPECIFIED),
            if(lockY)
                getChildMeasureSpec(parentHeightMeasureSpec, paddingTop + paddingBottom + lp.topMargin + lp.bottomMargin + heightUsed, lp.height)
            else MeasureSpec.makeMeasureSpec(lp.topMargin + lp.bottomMargin, MeasureSpec.UNSPECIFIED)
        )
    }

    override fun computeScroll() {
        if (mScroller!!.isFinished) {
            return
        }

        mScroller!!.computeScrollOffset()
        val x = mScroller!!.currX
        val y = mScroller!!.currY
        var unconsumedX = x - mLastScrollerX
        var unconsumedY = y - mLastScrollerY
        mLastScrollerX = x
        mLastScrollerY = y

        // Nested Scrolling Pre Pass
        mScrollConsumed[0] = 0
        mScrollConsumed[1] = 0
        dispatchNestedPreScroll(
            unconsumedX, unconsumedY, mScrollConsumed, null,
            ViewCompat.TYPE_NON_TOUCH
        )
        unconsumedX -= mScrollConsumed[0]
        unconsumedY -= mScrollConsumed[1]

        val rangeX = scrollRangeX
        val rangeY = scrollRangeY

        if (unconsumedX != 0 || unconsumedY != 0) {
            // Internal Scroll
            val oldScrollX = scrollX
            val oldScrollY = scrollY
            overScrollByCompat(unconsumedX, unconsumedY, scrollX, oldScrollY, rangeX, rangeY, 0, 0, false)
            val scrolledByMeX = scrollX - oldScrollX
            val scrolledByMeY = scrollY - oldScrollY
            unconsumedX -= scrolledByMeX
            unconsumedY -= scrolledByMeY

            // Nested Scrolling Post Pass
            mScrollConsumed[0] = 0
            mScrollConsumed[1] = 0
            dispatchNestedScroll(
                scrolledByMeX, scrolledByMeY, unconsumedX, unconsumedY, mScrollOffset,
                ViewCompat.TYPE_NON_TOUCH, mScrollConsumed
            )
            unconsumedX -= mScrollConsumed[0]
            unconsumedY -= mScrollConsumed[1]
        }

        if (unconsumedX != 0 && unconsumedY != 0) {
            val mode = overScrollMode
            val canOverscrollX = mode == OVER_SCROLL_ALWAYS
                    || (mode == OVER_SCROLL_IF_CONTENT_SCROLLS && rangeX > 0)
            val canOverscrollY = mode == OVER_SCROLL_ALWAYS
                    || (mode == OVER_SCROLL_IF_CONTENT_SCROLLS && rangeY > 0)
            if (canOverscrollX) {
                ensureGlows()
                if (unconsumedX < 0) {
                    if (mEdgeGlowLeft!!.isFinished) {
                        mEdgeGlowLeft!!.onAbsorb(mScroller!!.currVelocity.toInt())
                    }
                } else {
                    if (mEdgeGlowRight!!.isFinished) {
                        mEdgeGlowRight!!.onAbsorb(mScroller!!.currVelocity.toInt())
                    }
                }
            }
            if (canOverscrollY) {
                ensureGlows()
                if (unconsumedY < 0) {
                    if (mEdgeGlowTop!!.isFinished) {
                        mEdgeGlowTop!!.onAbsorb(mScroller!!.currVelocity.toInt())
                    }
                } else {
                    if (mEdgeGlowBottom!!.isFinished) {
                        mEdgeGlowBottom!!.onAbsorb(mScroller!!.currVelocity.toInt())
                    }
                }
            }
            abortAnimatedScroll()
        }

        if (!mScroller!!.isFinished) {
            ViewCompat.postInvalidateOnAnimation(this)
        } else {
            stopNestedScroll(ViewCompat.TYPE_NON_TOUCH)
        }
    }

    private fun runAnimatedScroll(participateInNestedScrolling: Boolean) {
        if (participateInNestedScrolling) {
            startNestedScroll(
                ViewCompat.SCROLL_AXIS_HORIZONTAL or ViewCompat.SCROLL_AXIS_VERTICAL,
                ViewCompat.TYPE_NON_TOUCH
            )
        } else {
            stopNestedScroll(ViewCompat.TYPE_NON_TOUCH)
        }
        mLastScrollerX = scrollX
        mLastScrollerY = scrollY
        ViewCompat.postInvalidateOnAnimation(this)
    }

    private fun abortAnimatedScroll() {
        mScroller!!.abortAnimation()
        stopNestedScroll(ViewCompat.TYPE_NON_TOUCH)
    }

    /**
     * Scrolls the view to the given child.
     *
     * @param child the View to scroll to
     */
    private fun scrollToChild(child: View) {
        child.getDrawingRect(mTempRect)

        /* Offset from child's local coordinates to ScrollView coordinates */
        offsetDescendantRectToMyCoords(child, mTempRect)

        val scrollDeltaX = computeScrollDeltaToGetChildRectOnScreenX(mTempRect)
        val scrollDeltaY = computeScrollDeltaToGetChildRectOnScreenY(mTempRect)

        if (scrollDeltaX != 0 || scrollDeltaY != 0) {
            scrollBy(scrollDeltaX, scrollDeltaY)
        }
    }

    /**
     * If rect is off screen, scroll just enough to get it (or at least the
     * first screen size chunk of it) on screen.
     *
     * @param rect The rectangle.
     * @param immediate True to scroll immediately without animation
     * @return true if scrolling was performed
     */
    private fun scrollToChildRect(rect: Rect, immediate: Boolean): Boolean {
        val deltaX = computeScrollDeltaToGetChildRectOnScreenX(rect)
        val deltaY = computeScrollDeltaToGetChildRectOnScreenY(rect)
        val scrollX = deltaX != 0
        val scrollY = deltaY != 0
        if (scrollX || scrollY) {
            if (immediate) {
                scrollBy(deltaX, deltaY)
            } else {
                smoothScrollBy(deltaX, deltaY)
            }
        }
        return scrollX || scrollY
    }

    /**
     * Compute the amount to scroll in the X direction in order to get
     * a rectangle completely on the screen (or, if wider than the screen,
     * at least the first screen size chunk of it).
     *
     * @param rect The rect.
     * @return The scroll delta.
     */
    protected fun computeScrollDeltaToGetChildRectOnScreenX(rect: Rect): Int {
        if (childCount == 0) return 0

        val width = width
        var screenLeft = scrollX
        var screenRight = screenLeft + width
        val actualScreenRight = screenRight

        val fadingEdge = horizontalFadingEdgeLength

        // TODO: screenTop should be incremented by fadingEdge * getTopFadingEdgeStrength (but for
        // the target scroll distance).
        // leave room for top fading edge as long as rect isn't at very top
        if (rect.left > 0) {
            screenLeft += fadingEdge
        }

        // TODO: screenBottom should be decremented by fadingEdge * getBottomFadingEdgeStrength (but
        // for the target scroll distance).
        // leave room for bottom fading edge as long as rect isn't at very bottom
        val child = getChildAt(0)
        val lp = child.layoutParams as LayoutParams
        if (rect.right < child.width + lp.leftMargin + lp.rightMargin) {
            screenRight -= fadingEdge
        }

        var scrollXDelta = 0

        if (rect.right > screenRight && rect.left > screenLeft) {
            // need to move down to get it in view: move down just enough so
            // that the entire rectangle is in view (or at least the first
            // screen size chunk).

            scrollXDelta += if (rect.width() > width) {
                // just enough to get screen size chunk on
                (rect.left - screenLeft)
            } else {
                // get entire rect at bottom of screen
                (rect.right - screenRight)
            }

            // make sure we aren't scrolling beyond the end of our content
            val right = child.right + lp.rightMargin
            val distanceToRight = right - actualScreenRight
            scrollXDelta = min(scrollXDelta.toDouble(), distanceToRight.toDouble()).toInt()
        } else if (rect.left < screenLeft && rect.right < screenRight) {
            // need to move up to get it in view: move up just enough so that
            // entire rectangle is in view (or at least the first screen
            // size chunk of it).

            scrollXDelta -= if (rect.width() > width) {
                // screen size chunk
                (screenRight - rect.right)
            } else {
                // entire rect at top
                (screenLeft - rect.left)
            }

            // make sure we aren't scrolling any further than the top our content
            scrollXDelta = max(scrollXDelta.toDouble(), -scrollX.toDouble()).toInt()
        }
        return scrollXDelta
    }

    /**
     * Compute the amount to scroll in the Y direction in order to get
     * a rectangle completely on the screen (or, if taller than the screen,
     * at least the first screen size chunk of it).
     *
     * @param rect The rect.
     * @return The scroll delta.
     */
    protected fun computeScrollDeltaToGetChildRectOnScreenY(rect: Rect): Int {
        if (childCount == 0) return 0

        val height = height
        var screenTop = scrollY
        var screenBottom = screenTop + height
        val actualScreenBottom = screenBottom

        val fadingEdge = verticalFadingEdgeLength

        // TODO: screenTop should be incremented by fadingEdge * getTopFadingEdgeStrength (but for
        // the target scroll distance).
        // leave room for top fading edge as long as rect isn't at very top
        if (rect.top > 0) {
            screenTop += fadingEdge
        }

        // TODO: screenBottom should be decremented by fadingEdge * getBottomFadingEdgeStrength (but
        // for the target scroll distance).
        // leave room for bottom fading edge as long as rect isn't at very bottom
        val child = getChildAt(0)
        val lp = child.layoutParams as LayoutParams
        if (rect.bottom < child.height + lp.topMargin + lp.bottomMargin) {
            screenBottom -= fadingEdge
        }

        var scrollYDelta = 0

        if (rect.bottom > screenBottom && rect.top > screenTop) {
            // need to move down to get it in view: move down just enough so
            // that the entire rectangle is in view (or at least the first
            // screen size chunk).

            scrollYDelta += if (rect.height() > height) {
                // just enough to get screen size chunk on
                (rect.top - screenTop)
            } else {
                // get entire rect at bottom of screen
                (rect.bottom - screenBottom)
            }

            // make sure we aren't scrolling beyond the end of our content
            val bottom = child.bottom + lp.bottomMargin
            val distanceToBottom = bottom - actualScreenBottom
            scrollYDelta = min(scrollYDelta.toDouble(), distanceToBottom.toDouble()).toInt()
        } else if (rect.top < screenTop && rect.bottom < screenBottom) {
            // need to move up to get it in view: move up just enough so that
            // entire rectangle is in view (or at least the first screen
            // size chunk of it).

            scrollYDelta -= if (rect.height() > height) {
                // screen size chunk
                (screenBottom - rect.bottom)
            } else {
                // entire rect at top
                (screenTop - rect.top)
            }

            // make sure we aren't scrolling any further than the top our content
            scrollYDelta = max(scrollYDelta.toDouble(), -scrollY.toDouble()).toInt()
        }
        return scrollYDelta
    }

    override fun requestChildFocus(child: View, focused: View) {
        if (!mIsLayoutDirty) {
            scrollToChild(focused)
        } else {
            // The child may not be laid out yet, we can't compute the scroll yet
            mChildToScrollTo = focused
        }
        super.requestChildFocus(child, focused)
    }


    /**
     * When looking for focus in children of a scroll view, need to be a little
     * more careful not to give focus to something that is scrolled off screen.
     *
     *
     * This is more expensive than the default [android.view.ViewGroup]
     * implementation, otherwise this behavior might have been made the default.
     */
    override fun onRequestFocusInDescendants(
        direction: Int,
        previouslyFocusedRect: Rect?
    ): Boolean {
        // convert from forward / backward notation to up / down / left / right
        // (ugh).

        var direction = direction
        if (direction == FOCUS_FORWARD) {
            direction = FOCUS_DOWN
        } else if (direction == FOCUS_BACKWARD) {
            direction = FOCUS_UP
        }

        val nextFocus = if (previouslyFocusedRect == null)
            FocusFinder.getInstance().findNextFocus(this, null, direction)
        else
            FocusFinder.getInstance().findNextFocusFromRect(
                this, previouslyFocusedRect, direction
            )

        if (nextFocus == null) {
            return false
        }

        if (isOffPage(nextFocus)) {
            return false
        }

        return nextFocus.requestFocus(direction, previouslyFocusedRect)
    }

    override fun requestChildRectangleOnScreen(
        child: View, rectangle: Rect,
        immediate: Boolean
    ): Boolean {
        // offset into coordinate space of this scroll view
        rectangle.offset(
            child.left - child.scrollX,
            child.top - child.scrollY
        )

        return scrollToChildRect(rectangle, immediate)
    }

    override fun requestLayout() {
        mIsLayoutDirty = true
        super.requestLayout()
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)
        mIsLayoutDirty = false
        // Give a child focus if it needs it
        if (mChildToScrollTo != null && isViewDescendantOf(mChildToScrollTo!!, this)) {
            scrollToChild(mChildToScrollTo!!)
        }
        mChildToScrollTo = null

        if (!mIsLaidOut) {
            // If there is a saved state, scroll to the position saved in that state.
            if (mSavedState != null) {
                scrollTo(mSavedState!!.scrollPositionX, mSavedState!!.scrollPositionY)
                mSavedState = null
            } // mScrollY default value is "0"


            // Make sure current scrollY position falls into the scroll range.  If it doesn't,
            // scroll such that it does.
            var childWidth = 0
            var childHeight = 0
            if (childCount > 0) {
                val child = getChildAt(0)
                val lp = child.layoutParams as LayoutParams
                childWidth = child.measuredWidth + lp.leftMargin + lp.rightMargin
                childHeight = child.measuredHeight + lp.topMargin + lp.bottomMargin
            }
            val parentSpaceX = r - l - paddingLeft - paddingRight
            val parentSpaceY = b - t - paddingTop - paddingBottom
            val currentScrollX = scrollX
            val currentScrollY = scrollY
            val newScrollX = clamp(currentScrollX, parentSpaceX, childWidth)
            val newScrollY = clamp(currentScrollY, parentSpaceY, childHeight)
            if (newScrollX != currentScrollX || newScrollY != currentScrollY) {
                scrollTo(newScrollX, newScrollY)
            }
        }

        // Calling this with the present values causes it to re-claim them
        scrollTo(scrollX, scrollY)
        mIsLaidOut = true
    }

    public override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        mIsLaidOut = false
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        val currentFocused = findFocus()
        if (null == currentFocused || this === currentFocused) {
            return
        }

        // If the currently-focused view was visible on the screen when the
        // screen was at the old height, then scroll the screen to make that
        // view visible with the new screen height.
        if (isWithinDeltaOfPage(currentFocused, 0, oldw, 0, oldh)) {
            currentFocused.getDrawingRect(mTempRect)
            offsetDescendantRectToMyCoords(currentFocused, mTempRect)
            val scrollDeltaX = computeScrollDeltaToGetChildRectOnScreenX(mTempRect)
            val scrollDeltaY = computeScrollDeltaToGetChildRectOnScreenY(mTempRect)
            doScroll(scrollDeltaX, scrollDeltaY)
        }
    }

    /**
     * Fling the scroll view
     *
     * @param velocityY The initial velocity in the Y direction. Positive
     * numbers mean that the finger/cursor is moving down the screen,
     * which means we want to scroll towards the top.
     */
    fun fling(velocityX: Int, velocityY: Int) {
        if (childCount > 0) {
            mScroller!!.fling(
                scrollX, scrollY,  // start
                velocityX, velocityY,  // velocities
                Int.MIN_VALUE, Int.MAX_VALUE,  // x
                Int.MIN_VALUE, Int.MAX_VALUE,  // y
                0, 0
            ) // overscroll
            runAnimatedScroll(true)
        }
    }

    private fun endDrag() {
        mIsBeingDragged = false

        recycleVelocityTracker()
        stopNestedScroll(ViewCompat.TYPE_TOUCH)

        if (mEdgeGlowTop != null) {
            mEdgeGlowLeft!!.onRelease()
            mEdgeGlowTop!!.onRelease()
            mEdgeGlowRight!!.onRelease()
            mEdgeGlowBottom!!.onRelease()
        }
    }

    /**
     * {@inheritDoc}
     *
     *
     * This version also clamps the scrolling to the bounds of our child.
     */
    override fun scrollTo(x: Int, y: Int) {
        // we rely on the fact the View.scrollBy calls scrollTo.
        var x = x
        var y = y
        if (childCount > 0) {
            val child = getChildAt(0)
            val lp = child.layoutParams as LayoutParams
            val parentSpaceHorizontal = width - paddingLeft - paddingRight
            val childSizeHorizontal = child.width + lp.leftMargin + lp.rightMargin
            val parentSpaceVertical = height - paddingTop - paddingBottom
            val childSizeVertical = child.height + lp.topMargin + lp.bottomMargin
            x = clamp(x, parentSpaceHorizontal, childSizeHorizontal)
            y = clamp(y, parentSpaceVertical, childSizeVertical)
            if (x != scrollX || y != scrollY) {

                super.scrollTo(x, y)
            }
        }
    }
    fun scrollToIgnoringClamp(x: Int, y: Int) {
        super.scrollTo(x, y)
        mScroller?.finalX
    }

    private fun ensureGlows() {
        if (overScrollMode != OVER_SCROLL_NEVER) {
            if (mEdgeGlowTop == null) {
                val context = context
                mEdgeGlowLeft = EdgeEffect(context)
                mEdgeGlowTop = EdgeEffect(context)
                mEdgeGlowRight = EdgeEffect(context)
                mEdgeGlowBottom = EdgeEffect(context)
            }
        } else {
            mEdgeGlowTop = null
            mEdgeGlowBottom = null
        }
    }

    private fun maxNeumorphicShadowExtent(group: ViewGroup): Float {
        var max = 0f
        for (i in 0 until group.childCount) {
            val child = group.getChildAt(i) ?: continue
            if (child.visibility == GONE) continue
            val bg = child.background as? com.lightningkite.kiteui.views.NeumorphicDrawable
            if (bg != null) max = maxOf(max, bg.shadowExtent)
            if (child is ViewGroup) max = maxOf(max, maxNeumorphicShadowExtent(child))
        }
        return max
    }

    override fun dispatchDraw(canvas: Canvas) {
        val shadowExtentInt = maxNeumorphicShadowExtent(this).toInt()
        val saveCount = canvas.save()
        canvas.clipRect(
            Int.MIN_VALUE,
            scrollY + paddingTop - shadowExtentInt,
            Int.MAX_VALUE,
            scrollY + height - paddingBottom + shadowExtentInt
        )
        for (i in 0 until childCount) {
            val child = getChildAt(i) ?: continue
            if (child.visibility == GONE) continue
            val bg = child.background as? com.lightningkite.kiteui.views.NeumorphicDrawable ?: continue
            bg.drawOuterShadowsFromParent(canvas, child.left, child.top)
        }
        super.dispatchDraw(canvas)
        canvas.restoreToCount(saveCount)
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        if (mEdgeGlowTop != null) {
            val scrollX = scrollX
            val scrollY = scrollY
            if (!mEdgeGlowLeft!!.isFinished) {
                val restoreCount = canvas.save()
                var width = width
                var height = height
                var xTranslation = min(0.0, scrollX.toDouble()).toInt()
                var yTranslation = scrollY
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP || clipToPadding) {
                    width -= paddingLeft + paddingRight
                    xTranslation += paddingLeft
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && clipToPadding) {
                    height -= paddingTop + paddingBottom
                    yTranslation += paddingTop
                }
                canvas.translate(xTranslation.toFloat(), yTranslation.toFloat())
                canvas.rotate(-90f, height / 2f, height / 2f)
                mEdgeGlowLeft!!.setSize(height, width)
                if (mEdgeGlowLeft!!.draw(canvas)) {
                    ViewCompat.postInvalidateOnAnimation(this)
                }
                canvas.restoreToCount(restoreCount)
            }
            if (!mEdgeGlowTop!!.isFinished) {
                val restoreCount = canvas.save()
                var width = width
                var height = height
                var xTranslation = scrollX
                var yTranslation = min(0.0, scrollY.toDouble()).toInt()
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP || clipToPadding) {
                    width -= paddingLeft + paddingRight
                    xTranslation += paddingLeft
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && clipToPadding) {
                    height -= paddingTop + paddingBottom
                    yTranslation += paddingTop
                }
                canvas.translate(xTranslation.toFloat(), yTranslation.toFloat())
                mEdgeGlowTop!!.setSize(width, height)
                if (mEdgeGlowTop!!.draw(canvas)) {
                    ViewCompat.postInvalidateOnAnimation(this)
                }
                canvas.restoreToCount(restoreCount)
            }
            if (!mEdgeGlowRight!!.isFinished) {
                val restoreCount = canvas.save()
                var width = width
                var height = height
                var xTranslation = (max(scrollRangeX.toDouble(), scrollX.toDouble()) + width).toInt()
                var yTranslation = scrollY
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP || clipToPadding) {
                    width -= paddingLeft + paddingRight
                    xTranslation += paddingLeft
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && clipToPadding) {
                    height -= paddingTop + paddingBottom
                    yTranslation -= paddingBottom
                }
                canvas.translate((xTranslation - width).toFloat(), yTranslation.toFloat())
                canvas.rotate(90f, width / 2f, width / 2f)
                mEdgeGlowRight!!.setSize(height, width)
                if (mEdgeGlowRight!!.draw(canvas)) {
                    ViewCompat.postInvalidateOnAnimation(this)
                }
                canvas.restoreToCount(restoreCount)
            }
            if (!mEdgeGlowBottom!!.isFinished) {
                val restoreCount = canvas.save()
                var width = width
                var height = height
                var xTranslation = scrollX
                var yTranslation = (max(scrollRangeY.toDouble(), scrollY.toDouble()) + height).toInt()
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP || clipToPadding) {
                    width -= paddingLeft + paddingRight
                    xTranslation += paddingLeft
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && clipToPadding) {
                    height -= paddingTop + paddingBottom
                    yTranslation -= paddingBottom
                }
                canvas.translate((xTranslation - width).toFloat(), yTranslation.toFloat())
                canvas.rotate(180f, width.toFloat(), 0f)
                mEdgeGlowBottom!!.setSize(width, height)
                if (mEdgeGlowBottom!!.draw(canvas)) {
                    ViewCompat.postInvalidateOnAnimation(this)
                }
                canvas.restoreToCount(restoreCount)
            }
        }
    }

    override fun onRestoreInstanceState(state: Parcelable) {
        if (state !is SavedState) {
            super.onRestoreInstanceState(state)
            return
        }

        val ss = state
        super.onRestoreInstanceState(ss.superState)
        mSavedState = ss
        requestLayout()
    }

    override fun onSaveInstanceState(): Parcelable? {
        val superState = super.onSaveInstanceState()
        val ss = SavedState(superState)
        ss.scrollPositionX = scrollX
        ss.scrollPositionY = scrollY
        return ss
    }

    internal class SavedState : BaseSavedState {
        var scrollPositionX: Int = 0
        var scrollPositionY: Int = 0

        constructor(superState: Parcelable?) : super(superState)

        constructor(source: Parcel) : super(source) {
            scrollPositionX = source.readInt()
            scrollPositionY = source.readInt()
        }

        override fun writeToParcel(dest: Parcel, flags: Int) {
            super.writeToParcel(dest, flags)
            dest.writeInt(scrollPositionX)
            dest.writeInt(scrollPositionY)
        }

        override fun toString(): String {
            return ("HorizontalScrollView.SavedState{"
                    + Integer.toHexString(System.identityHashCode(this))
                    + " scrollPositionX=" + scrollPositionX + "}"
                    + " scrollPositionY=" + scrollPositionY + "}")
        }

        companion object {
            @JvmField
            val CREATOR: Parcelable.Creator<SavedState?> = object : Parcelable.Creator<SavedState?> {
                override fun createFromParcel(`in`: Parcel): SavedState? {
                    return SavedState(`in`)
                }

                override fun newArray(size: Int): Array<SavedState?> {
                    return arrayOfNulls(size)
                }
            }
        }
    }

    internal class AccessibilityDelegate : AccessibilityDelegateCompat() {
        override fun performAccessibilityAction(host: View, action: Int, arguments: Bundle?): Boolean {
            if (super.performAccessibilityAction(host, action, arguments)) {
                return true
            }
            val nsvHost = host as TwoWayNestedScrollView
            if (!nsvHost.isEnabled) {
                return false
            }
            when (action) {
                AccessibilityNodeInfoCompat.ACTION_SCROLL_FORWARD, R.id.accessibilityActionScrollDown -> {
                    run {
                        val viewportHeight = (nsvHost.height - nsvHost.paddingBottom
                                - nsvHost.paddingTop)
                        val targetScrollY = min(
                            (nsvHost.scrollY + viewportHeight).toDouble(),
                            nsvHost.scrollRangeY.toDouble()
                        ).toInt()
                        if (targetScrollY != nsvHost.scrollY) {
                            nsvHost.smoothScrollTo(0, targetScrollY, true)
                            return true
                        }
                    }
                    return false
                }

                AccessibilityNodeInfoCompat.ACTION_SCROLL_BACKWARD, R.id.accessibilityActionScrollUp -> {
                    run {
                        val viewportHeight = (nsvHost.height - nsvHost.paddingBottom
                                - nsvHost.paddingTop)
                        val targetScrollY = max((nsvHost.scrollY - viewportHeight).toDouble(), 0.0).toInt()
                        if (targetScrollY != nsvHost.scrollY) {
                            nsvHost.smoothScrollTo(0, targetScrollY, true)
                            return true
                        }
                    }
                    return false
                }
            }
            return false
        }

        override fun onInitializeAccessibilityNodeInfo(host: View, info: AccessibilityNodeInfoCompat) {
            super.onInitializeAccessibilityNodeInfo(host, info)
            val nsvHost = host as TwoWayNestedScrollView
            info.className = ScrollView::class.java.name
            if (nsvHost.isEnabled) {
                val scrollRange = nsvHost.scrollRangeY
                if (scrollRange > 0) {
                    info.isScrollable = true
                    if (nsvHost.scrollY > 0) {
                        info.addAction(
                            AccessibilityNodeInfoCompat.AccessibilityActionCompat
                                .ACTION_SCROLL_BACKWARD
                        )
                        info.addAction(
                            AccessibilityNodeInfoCompat.AccessibilityActionCompat
                                .ACTION_SCROLL_UP
                        )
                    }
                    if (nsvHost.scrollY < scrollRange) {
                        info.addAction(
                            AccessibilityNodeInfoCompat.AccessibilityActionCompat
                                .ACTION_SCROLL_FORWARD
                        )
                        info.addAction(
                            AccessibilityNodeInfoCompat.AccessibilityActionCompat
                                .ACTION_SCROLL_DOWN
                        )
                    }
                }
            }
        }

        override fun onInitializeAccessibilityEvent(host: View, event: AccessibilityEvent) {
            super.onInitializeAccessibilityEvent(host, event)
            val nsvHost = host as TwoWayNestedScrollView
            event.className = ScrollView::class.java.name
            val scrollable = nsvHost.scrollRangeY > 0
            event.isScrollable = scrollable
            event.scrollX = nsvHost.scrollX
            event.scrollY = nsvHost.scrollY
            AccessibilityRecordCompat.setMaxScrollX(event, nsvHost.scrollRangeX)
            AccessibilityRecordCompat.setMaxScrollY(event, nsvHost.scrollRangeY)
        }
    }

    companion object {
        const val ANIMATED_SCROLL_GAP: Int = 250

        const val MAX_SCROLL_FACTOR: Float = 0.5f

        private const val TAG = "TwoWayNestedScrollView"
        private const val DEFAULT_SMOOTH_SCROLL_DURATION = 250

        /**
         * Sentinel value for no current active pointer.
         * Used by [.mActivePointerId].
         */
        private const val INVALID_POINTER = -1

        private val ACCESSIBILITY_DELEGATE = AccessibilityDelegate()

        private val SCROLLVIEW_STYLEABLE = intArrayOf(
            R.attr.fillViewport
        )

        /**
         * Return true if child is a descendant of parent, (or equal to the parent).
         */
        private fun isViewDescendantOf(child: View, parent: View): Boolean {
            if (child === parent) {
                return true
            }

            val theParent = child.parent
            return (theParent is ViewGroup) && isViewDescendantOf(theParent as View, parent)
        }

        private fun clamp(n: Int, my: Int, child: Int): Int {
            if (my >= child || n < 0) {
                /* my >= child is this case:
             *                    |--------------- me ---------------|
             *     |------ child ------|
             * or
             *     |--------------- me ---------------|
             *            |------ child ------|
             * or
             *     |--------------- me ---------------|
             *                                  |------ child ------|
             *
             * n < 0 is this case:
             *     |------ me ------|
             *                    |-------- child --------|
             *     |-- mScrollX --|
             */
                return 0
            }
            if ((my + n) > child) {
                /* this case:
             *                    |------ me ------|
             *     |------ child ------|
             *     |-- mScrollX --|
             */
                return child - my
            }
            return n
        }
    }
}
