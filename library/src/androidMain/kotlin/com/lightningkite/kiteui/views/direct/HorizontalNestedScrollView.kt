package com.lightningkite.kiteui.views.direct

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.*
import android.widget.EdgeEffect
import android.widget.FrameLayout
import android.widget.OverScroller
import androidx.core.view.*
import androidx.core.widget.EdgeEffectCompat
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Apache - https://github.com/Tans5/HorizontalNestedScrollView/blob/master/LICENSE
 * author: pengcheng.tan
 * date: 2020/3/23
 */

class HorizontalNestedScrollView : FrameLayout, NestedScrollingChild3, NestedScrollingParent3 {

    private val viewConfiguration: ViewConfiguration by lazy { ViewConfiguration.get(context) }
    private val touchSlop: Int by lazy { viewConfiguration.scaledTouchSlop }
    private val minVelocity: Int by lazy { viewConfiguration.scaledMinimumFlingVelocity }
    private val maxVelocity: Int by lazy { viewConfiguration.scaledMaximumFlingVelocity }

    private val scroller: OverScroller by lazy { OverScroller(context) }
    private var velocityTracker: VelocityTracker? = null

    private val edgeGlowStart: EdgeEffect by lazy { EdgeEffect(context) }
    private val edgeGlowEnd: EdgeEffect by lazy { EdgeEffect(context) }
    // TODO: adjust over scroll enable.
    private val overScrollEnable: Boolean = true

    private val parentHelper: NestedScrollingParentHelper by lazy { NestedScrollingParentHelper(this).apply { isNestedScrollingEnabled = true } }
    private val childHelper: NestedScrollingChildHelper by lazy { NestedScrollingChildHelper(this).apply { isNestedScrollingEnabled = true } }

    constructor(context: Context) : super(context) {
        initAttrs()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        initAttrs()
    }

    constructor(context: Context, attrs: AttributeSet, style: Int) : super(context, attrs, style) {
        initAttrs()
    }

    private fun initAttrs() {
        isClickable = true
        isFocusable = true
        setWillNotDraw(false)
    }

    private var lastTouchX: Int = -1
    private var activePointerId: Int = -1
    private var isBeingDragged = false
    private var lastScrollerX = 0
    private var nestedXOffset = 0

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {

        val actionMasked = ev?.actionMasked
        if (actionMasked == MotionEvent.ACTION_MOVE && isBeingDragged) {
            return true
        }
        return when (actionMasked) {

            MotionEvent.ACTION_DOWN -> {
                val x = (ev.x + 0.5).toInt()
                if (!inChild(x, (ev.y + 0.5).toInt())) {
                    isBeingDragged = false
                    recycleVelocityTracker()
                    false
                } else {
                    lastTouchX = x
                    activePointerId = ev.getPointerId(0)
                    initOrResetVelocityTracker()
                    velocityTracker?.addMovement(ev)
                    scroller.computeScrollOffset()
                    isBeingDragged = !scroller.isFinished
                    startNestedScroll(ViewCompat.SCROLL_AXIS_HORIZONTAL, ViewCompat.TYPE_TOUCH)
                    isBeingDragged
                }
            }

            MotionEvent.ACTION_MOVE -> {
                val pointerIndex = ev.findPointerIndex(activePointerId)
                val x = (ev.getX(pointerIndex) + 0.5).toInt()
                val dx = abs(lastTouchX - x)
                if (dx > touchSlop && (nestedScrollAxes and ViewCompat.SCROLL_AXIS_HORIZONTAL) == 0) {
                    isBeingDragged = true
                    lastTouchX = x
                    initVelocityTrackerIfNotExists()
                    velocityTracker?.addMovement(ev)
                    nestedXOffset = 0
                    parent?.requestDisallowInterceptTouchEvent(true)
                    true
                } else {
                    false
                }
            }

            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
                isBeingDragged = false
                activePointerId = -1
                recycleVelocityTracker()
                if (scroller.springBack(scrollX, scrollY, 0, getScrollRangeX(), 0, 0)) {
                    ViewCompat.postInvalidateOnAnimation(this)
                }
                stopNestedScroll(ViewCompat.TYPE_TOUCH)
                false
            }

            MotionEvent.ACTION_POINTER_UP -> {
                val actionIndex = ev.actionIndex
                val currentId = ev.getPointerId(actionIndex)
                if (currentId == activePointerId) {
                    val newIndex = if (actionIndex == 0) 1 else 0
                    activePointerId = ev.getPointerId(newIndex)
                    lastTouchX = (ev.getX(newIndex) + 0.5).toInt()
                }
                false
            }
            else -> false
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        initVelocityTrackerIfNotExists()

        var result = true
        if (event != null) {
            val velocityEvent = MotionEvent.obtain(event)

            velocityEvent.offsetLocation(if (MotionEvent.ACTION_DOWN == event.actionMasked) 0f else nestedXOffset.toFloat(), 0f)

            val actionIndex = event.actionIndex
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {

                    nestedXOffset = 0

                    activePointerId = event.getPointerId(actionIndex)
                    lastTouchX = (event.getX(event.findPointerIndex(activePointerId)) + 0.5).toInt()

                    if (!scroller.isFinished) {
                        parent?.requestDisallowInterceptTouchEvent(true)
                        abortAnimatedScroll()
                    }

                    if (childCount == 0) {
                        result = false
                    }
                    startNestedScroll(ViewCompat.SCROLL_AXIS_HORIZONTAL, ViewCompat.TYPE_TOUCH)
                }

                MotionEvent.ACTION_MOVE -> {
                    val activeActionIndex = event.findPointerIndex(activePointerId)
                    val x = (event.getX(activeActionIndex) + 0.5).toInt()
                    var dx = lastTouchX - x
                    if (!isBeingDragged && abs(dx) > touchSlop) {
                        parent?.requestDisallowInterceptTouchEvent(true)
                        isBeingDragged = true
                        if (dx > 0) {
                            dx -= touchSlop
                        } else {
                            dx += touchSlop
                        }
                    }

                    if (isBeingDragged) {
                        val scrollConsumed = IntArray(2) { 0 }
                        val scrollOffset = IntArray(2) { 0 }
                        if (dispatchNestedPreScroll(dx, 0, scrollConsumed, scrollOffset, ViewCompat.TYPE_TOUCH)) {
                            dx -= scrollConsumed[0]
                            nestedXOffset += scrollOffset[0]
                        }
                        lastTouchX = x - scrollOffset[0]

                        val oldScrollX = scrollX
                        if (overScrollX(dx, scrollX, getScrollRangeX())) {
                            velocityTracker?.clear()
                        }

                        val scrolledDeltaX = scrollX - oldScrollX
                        val unconsumedX = dx - scrolledDeltaX
                        scrollConsumed[0] = 0
                        dispatchNestedScroll(scrolledDeltaX, 0, unconsumedX, 0, scrollOffset,
                            ViewCompat.TYPE_TOUCH, scrollConsumed)
                        lastTouchX -= scrollOffset[0]
                        nestedXOffset += scrollOffset[0]

                        if (overScrollEnable) {
                            val pulledToX = oldScrollX + dx
                            if (pulledToX < 0) {
                                EdgeEffectCompat.onPull(edgeGlowStart, dx.toFloat() / width.toFloat(),
                                    1f - event.getY(activeActionIndex) / height.toFloat())
                                if (!edgeGlowEnd.isFinished) {
                                    edgeGlowEnd.onRelease()
                                }
                            } else if (pulledToX > getScrollRangeX()) {
                                EdgeEffectCompat.onPull(edgeGlowEnd, dx.toFloat() / width.toFloat(),
                                    event.getY(activeActionIndex) / height.toFloat())
                                if (!edgeGlowStart.isFinished) {
                                    edgeGlowStart.onRelease()
                                }
                            }

                            if (!edgeGlowEnd.isFinished || !edgeGlowStart.isFinished) {
                                ViewCompat.postInvalidateOnAnimation(this)
                            }
                        }

                    }
                }

                MotionEvent.ACTION_UP -> {
                    val velocityTracker = this.velocityTracker
                    velocityTracker?.computeCurrentVelocity(1000, maxVelocity.toFloat())
                    val velocityX = velocityTracker?.getXVelocity(activePointerId)?.toInt() ?: 0
                    if (abs(velocityX) > minVelocity) {
                        if (!dispatchNestedPreFling(-velocityX.toFloat(), 0f)) {
                            dispatchNestedFling(-velocityX.toFloat(), 0f, true)
                            fling(-velocityX)
                        }
                    } else if (scroller.springBack(scrollX, scrollY, 0, getScrollRangeX(), 0, 0)) {
                        ViewCompat.postInvalidateOnAnimation(this)
                    }

                    endDrag()
                }

                MotionEvent.ACTION_CANCEL -> {

                    if (isBeingDragged && childCount > 0) {
                        if (scroller.springBack(scrollX, scrollY, 0, getScrollRangeX(), 0, 0)) {
                            ViewCompat.postInvalidateOnAnimation(this)
                        }
                    }

                    endDrag()
                }

                MotionEvent.ACTION_POINTER_DOWN -> {
                    activePointerId = event.getPointerId(actionIndex)
                    lastTouchX = (event.getX(event.findPointerIndex(activePointerId)) + 0.5).toInt()
                }

                MotionEvent.ACTION_POINTER_UP -> {
                    val currentId = event.getPointerId(actionIndex)
                    if (currentId == activePointerId) {
                        val newIndex = if (actionIndex == 0) 1 else 0
                        activePointerId = event.getPointerId(newIndex)
                        lastTouchX = (event.getX(newIndex) + 0.5).toInt()
                    }
                }
            }
            velocityTracker?.addMovement(velocityEvent)
            velocityEvent.recycle()
        }

        return result
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        if (!edgeGlowStart.isFinished) {
            val restoreCount = canvas.save()
            val width = width.toFloat()
            val height = height.toFloat()
            canvas.rotate(270f)
            canvas.translate(-height, min(0f, scrollX.toFloat()))
            edgeGlowStart.setSize(height.toInt(), width.toInt())
            if (edgeGlowStart.draw(canvas)) {
                ViewCompat.postInvalidateOnAnimation(this)
            }
            canvas.restoreToCount(restoreCount)
        }

        if (!edgeGlowEnd.isFinished) {
            val restoreCount = canvas.save()
            val width = width.toFloat()
            val height = height.toFloat()
            canvas.rotate(90f)
            canvas.translate(0f, -(max(getScrollRangeX().toFloat(), scrollX.toFloat()) + width))
            edgeGlowEnd.setSize(height.toInt(), width.toInt())
            if (edgeGlowEnd.draw(canvas)) {
                ViewCompat.postInvalidateOnAnimation(this)
            }
            canvas.restoreToCount(restoreCount)
        }
    }

    override fun computeScroll() {
        if (scroller.isFinished) {
            return
        }
        scroller.computeScrollOffset()
        val x = scroller.currX
        val dx = x - lastScrollerX
        lastScrollerX = x
        var unconsumed = dx
        val scrollConsumed = IntArray(2) { 0 }
        if (dispatchNestedPreScroll(unconsumed, 0, scrollConsumed, null, ViewCompat.TYPE_NON_TOUCH)) {
            unconsumed -= scrollConsumed[0]
        }

        if (unconsumed != 0) {
            val scrollXBefore = scrollX
            overScrollX(dx, scrollXBefore, getScrollRangeX())
            val scrollAfter = scrollX
            val scrollByMe = scrollAfter - scrollXBefore
            unconsumed -= scrollByMe
            scrollConsumed[0] = 0
            dispatchNestedScroll(scrollByMe, 0, unconsumed, 0, null,
                ViewCompat.TYPE_NON_TOUCH, scrollConsumed)
            unconsumed -= scrollConsumed[0]
        }
        if (unconsumed < 0) {
            if (edgeGlowStart.isFinished) {
                edgeGlowStart.onAbsorb(scroller.currVelocity.toInt())
            }
            abortAnimatedScroll()
        }
        if (unconsumed > 0) {
            if (edgeGlowEnd.isFinished) {
                edgeGlowEnd.onAbsorb(scroller.currVelocity.toInt())
            }
            abortAnimatedScroll()
        }

        if (!scroller.isFinished) {
            ViewCompat.postInvalidateOnAnimation(this)
        } else {
            stopNestedScroll(ViewCompat.TYPE_NON_TOUCH)
        }

    }

    override fun computeHorizontalScrollRange(): Int {
        if (childCount > 1) {
            error("HorizontalNestedScrollView only support one child.")
        }
        val parentSpace = width - paddingStart - paddingEnd
        if (childCount == 0) {
            return parentSpace
        }
        val child = getChildAt(0)
        val lp = child.layoutParams as? MarginLayoutParams
        var range = child.right + ((lp?.marginEnd) ?: 0)
        val overScrollRange = max(0, range - parentSpace)
        val currentScrollX = scrollX
        if (currentScrollX < 0) {
            range -= currentScrollX
        } else if (currentScrollX > overScrollRange) {
            range += currentScrollX - overScrollRange
        }

        return range
    }

    fun getScrollRangeX(): Int {
        val child = getChildAt(0)
        val lp = child.layoutParams as MarginLayoutParams
        val childSize = child.width + lp.leftMargin + lp.rightMargin
        val parentSpace = width - paddingLeft - paddingRight
        return max(0, childSize - parentSpace)
    }

    fun overScrollX(deltaX: Int, scrollX: Int, maxRange: Int): Boolean {
        val newScrollX = deltaX + scrollX
        return when {
            maxRange < newScrollX -> {
                scrollTo(maxRange, 0)
                true
            }
            newScrollX < 0 -> {
                scrollTo(0, 0)
                true
            }
            else -> {
                scrollTo(newScrollX, 0)
                false
            }
        }
    }

    fun fling(velocityX: Int) {
        if (childCount > 0) {
            scroller.fling(
                scrollX, scrollY,
                velocityX, 0,
                Int.MIN_VALUE, Int.MAX_VALUE,
                0, 0,
                0, 0
            )
            runAnimatedScroll(true)
        }
    }

    private fun runAnimatedScroll(nestedScrolling: Boolean) {
        if (nestedScrolling) {
            startNestedScroll(ViewCompat.SCROLL_AXIS_HORIZONTAL, ViewCompat.TYPE_NON_TOUCH)
        } else {
            stopNestedScroll(ViewCompat.TYPE_NON_TOUCH)
        }
        lastScrollerX = scrollX
        ViewCompat.postInvalidateOnAnimation(this)
    }

    private fun endDrag() {
        lastTouchX = -1
        activePointerId = -1
        isBeingDragged = false
        recycleVelocityTracker()
        edgeGlowStart.onRelease()
        edgeGlowEnd.onRelease()
        stopNestedScroll(ViewCompat.TYPE_TOUCH)
    }

    private fun initOrResetVelocityTracker() {
        val velocityTracker = this.velocityTracker
        if (velocityTracker == null) {
            this.velocityTracker = VelocityTracker.obtain()
        } else {
            velocityTracker.clear()
        }
    }

    private fun initVelocityTrackerIfNotExists() {
        if (velocityTracker == null) {
            this.velocityTracker = VelocityTracker.obtain()
        }
    }

    private fun recycleVelocityTracker() {
        val velocityTracker = this.velocityTracker
        velocityTracker?.recycle()
        this.velocityTracker = null
    }

    private fun abortAnimatedScroll() {
        scroller.abortAnimation()
    }

    private fun inChild(x: Int, y: Int): Boolean {
        return if (childCount > 0) {
            val scrollX = scrollX
            val child = getChildAt(0)
            return !(y < child.top || y >= child.bottom || x < child.left - scrollX || x >= child.right - scrollX)
        } else {
            false
        }
    }

    override fun generateLayoutParams(lp: ViewGroup.LayoutParams?): ViewGroup.LayoutParams {
        return MarginLayoutParams(lp)
    }

    override fun measureChild(
        child: View?,
        parentWidthMeasureSpec: Int,
        parentHeightMeasureSpec: Int
    ) {
        if (child != null) {
            val lp = child.layoutParams as MarginLayoutParams
            val childWidthSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
            val childHeightSpec = ViewGroup.getChildMeasureSpec(parentHeightMeasureSpec, paddingTop + paddingBottom, lp.height)
            child.measure(childWidthSpec, childHeightSpec)
        }
    }

    override fun measureChildWithMargins(
        child: View?,
        parentWidthMeasureSpec: Int,
        widthUsed: Int,
        parentHeightMeasureSpec: Int,
        heightUsed: Int
    ) {
        if (child != null) {
            val lp = child.layoutParams as MarginLayoutParams
            val childWidthSpec = MeasureSpec.makeMeasureSpec(lp.leftMargin + lp.bottomMargin, MeasureSpec.UNSPECIFIED)
            val childHeightSpec = ViewGroup.getChildMeasureSpec(parentHeightMeasureSpec,
                paddingTop + paddingBottom + lp.topMargin + lp.bottomMargin + heightUsed,
                lp.height)
            child.measure(childWidthSpec, childHeightSpec)
        }
    }

    override fun onNestedScrollAccepted(child: View, target: View, axes: Int, type: Int) {
        parentHelper.onNestedScrollAccepted(child, target, axes, type)
        startNestedScroll(ViewCompat.SCROLL_AXIS_HORIZONTAL, type)
    }

    override fun onNestedPreScroll(target: View, dx: Int, dy: Int, consumed: IntArray, type: Int) {
        dispatchNestedPreScroll(dx, dy, consumed, null, type)
    }

    override fun onStartNestedScroll(child: View, target: View, axes: Int, type: Int): Boolean {
        return (axes and ViewCompat.SCROLL_AXIS_HORIZONTAL) != 0
    }

    override fun onNestedScroll(
        target: View,
        dxConsumed: Int,
        dyConsumed: Int,
        dxUnconsumed: Int,
        dyUnconsumed: Int,
        type: Int,
        consumed: IntArray
    ) {
        onNestedScrollInternal(dxUnconsumed, type, consumed)
    }

    override fun onNestedScroll(
        target: View,
        dxConsumed: Int,
        dyConsumed: Int,
        dxUnconsumed: Int,
        dyUnconsumed: Int,
        type: Int
    ) {
        onNestedScrollInternal(dxUnconsumed, type, null)
    }

    private fun onNestedScrollInternal(dxUnconsumed: Int, type: Int, consumed: IntArray?) {
        val oldScrollX = scrollX
        // scrollBy(dxUnconsumed, 0)
        overScrollX(dxUnconsumed, scrollX, getScrollRangeX())
        val myConsumedX = scrollX - oldScrollX
        if (consumed != null) {
            consumed[0] += myConsumedX
        }
        val myUnconsumedX = dxUnconsumed - myConsumedX
        childHelper.dispatchNestedScroll(myConsumedX, 0, myUnconsumedX, 0, null, type, consumed)
    }

    override fun onStopNestedScroll(target: View, type: Int) {
        parentHelper.onStopNestedScroll(target, type)
        stopNestedScroll(type)
    }

    override fun dispatchNestedScroll(
        dxConsumed: Int,
        dyConsumed: Int,
        dxUnconsumed: Int,
        dyUnconsumed: Int,
        offsetInWindow: IntArray?,
        type: Int,
        consumed: IntArray
    ) {
        childHelper.dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, offsetInWindow, type, consumed)
    }

    override fun dispatchNestedScroll(
        dxConsumed: Int,
        dyConsumed: Int,
        dxUnconsumed: Int,
        dyUnconsumed: Int,
        offsetInWindow: IntArray?,
        type: Int
    ): Boolean {
        return childHelper.dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, offsetInWindow, type)
    }

    override fun dispatchNestedPreScroll(
        dx: Int,
        dy: Int,
        consumed: IntArray?,
        offsetInWindow: IntArray?,
        type: Int
    ): Boolean {
        return childHelper.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow, type)
    }

    override fun dispatchNestedPreFling(velocityX: Float, velocityY: Float): Boolean {
        return childHelper.dispatchNestedPreFling(velocityX, velocityY)
    }

    override fun onNestedPreFling(target: View, velocityX: Float, velocityY: Float): Boolean {
        return dispatchNestedPreFling(velocityX, velocityY)
    }

    override fun dispatchNestedFling(
        velocityX: Float,
        velocityY: Float,
        consumed: Boolean
    ): Boolean {
        return childHelper.dispatchNestedFling(velocityX, velocityY, consumed)
    }

    override fun onNestedFling(
        target: View,
        velocityX: Float,
        velocityY: Float,
        consumed: Boolean
    ): Boolean {
        return if (!consumed) {
            dispatchNestedFling(velocityX, velocityY, consumed)
            fling(-velocityX.toInt())
            true
        } else {
            false
        }
    }

    private var fuckedUpByX: Int = 0
    private var fuckedUpByY: Int = 0
    override fun scrollBy(x: Int, y: Int) {
        super.scrollBy(x, y)
        fuckedUpByX += x
        fuckedUpByY += y
    }

    override fun stopNestedScroll(type: Int) {
        childHelper.stopNestedScroll(type)
    }

    override fun hasNestedScrollingParent(type: Int): Boolean {
        return childHelper.hasNestedScrollingParent(type)
    }

    override fun startNestedScroll(axes: Int, type: Int): Boolean {
        return childHelper.startNestedScroll(axes, type)
    }

    override fun getNestedScrollAxes(): Int {
        return parentHelper.nestedScrollAxes
    }


}