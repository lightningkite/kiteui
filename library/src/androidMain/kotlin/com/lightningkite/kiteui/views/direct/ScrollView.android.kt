package com.lightningkite.kiteui.views.direct

import android.annotation.SuppressLint
import android.hardware.SensorManager
import android.os.Build
import android.view.*
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import androidx.core.view.ViewConfigurationCompat.getScaledMaximumFlingVelocity
import androidx.core.view.children
import androidx.core.widget.NestedScrollView
import com.lightningkite.kiteui.afterTimeout
import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.Rect
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView
import com.lightningkite.kiteui.views.RViewWrapper
import kotlin.math.*

class ScrollView constructor(
    context: RContext,
    override val horizontal: Boolean,
    override val vertical: Boolean
) : RViewWrapper(context), ScrollingBehaviors {
    private val scrollChanged = BasicListenable()
    private val vx = VelocityTracker.obtain()
    private val vy = VelocityTracker.obtain()
    private val horizontalScrollView: HorizontalScrollView? =
        if (horizontal) HorizontalScrollView(context.activity).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                setOnScrollChangeListener { _, _, _, _, _ ->
                    scrollChanged.invokeAll()
                }
            } else {
                val l: ViewTreeObserver.OnScrollChangedListener = ViewTreeObserver.OnScrollChangedListener {
                    scrollChanged.invokeAll()
                }
                viewTreeObserver.addOnScrollChangedListener(l)
                onRemove { viewTreeObserver.removeOnScrollChangedListener(l) }
            }
            val touches = HashSet<Int>()
            setOnTouchListener { v, event ->
                vx.addMovement(event)
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        touches.add(event.actionIndex)
                        _directlyInteractingWithScroller.value = true
                    }

                    MotionEvent.ACTION_MOVE -> {
                        touches.add(event.actionIndex)
                        _directlyInteractingWithScroller.value = true
                    }

                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        touches.remove(event.actionIndex)
                        _directlyInteractingWithScroller.value = touches.size != 0
                        if (touches.size == 0) afterTimeout(16) { snaps() }
                    }
                }
                println("TOUCH IS WORKING horizontalScrollView ${touches} (action = ${event.actionMasked})")
                false
            }
        } else null

    @SuppressLint("ClickableViewAccessibility")
    private val verticalScrollView: NestedScrollView? = if (vertical) NestedScrollView(context.activity).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            setOnScrollChangeListener { _, _, _, _, _ ->
                scrollChanged.invokeAll()
            }
        } else {
            val l: ViewTreeObserver.OnScrollChangedListener = ViewTreeObserver.OnScrollChangedListener {
                scrollChanged.invokeAll()
            }
            viewTreeObserver.addOnScrollChangedListener(l)
            onRemove { viewTreeObserver.removeOnScrollChangedListener(l) }
        }
        val touches = HashSet<Int>()
        setOnTouchListener { v, event ->
            vy.addMovement(event)
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    touches.add(event.actionIndex)
                    _directlyInteractingWithScroller.value = true
                }

                MotionEvent.ACTION_MOVE -> {
                    touches.add(event.actionIndex)
                    _directlyInteractingWithScroller.value = true
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    touches.remove(event.actionIndex)
                    _directlyInteractingWithScroller.value = touches.size != 0
                    if (touches.size == 0) afterTimeout(16) { snaps() }
                }
            }
            println("TOUCH IS WORKING verticalScrollView ${touches} (action = ${event.actionMasked})")
            false
        }
        isFillViewport = true
    } else null

//    private var dx = IntArray(4)
//    private var dy = IntArray(4)
//    private var dt = LongArray(4)
//    init {
//        var lastX = 0
//        var lastY = 0
//        var i = 0
//        scrollChanged.addListener {
//            val newX = horizontalScrollView?.scrollX ?: 0
//            val newY = verticalScrollView?.scrollY ?: 0
//            dt[i % dt.size] = System.currentTimeMillis()
//            dx[i % dt.size] += newX - lastX
//            dy[i % dt.size] += newY - lastY
//            i++
//            lastX = horizontalScrollView?.scrollX ?: 0
//            lastY = verticalScrollView?.scrollY ?: 0
//        }
//    }
//    private val vx get() =

    private val innermostView: ViewGroup =
        horizontalScrollView ?: verticalScrollView ?: error("ScrollView requires at least one axis.")
    private val outermostView: ViewGroup =
        verticalScrollView ?: horizontalScrollView ?: error("ScrollView requires at least one axis.")
    override val native: View = outermostView

    /**
     * Copied from OverScroller, this returns the distance that a fling with the given velocity
     * will go.
     * @param velocity The velocity of the fling
     * @return The distance that will be traveled by a fling of the given velocity.
     */
    private fun getSplineFlingDistance(velocity: Int): Float {
        /**
         * The following are copied from OverScroller to determine how far a fling will go.
         */
        val SCROLL_FRICTION = 0.015f
        val INFLEXION = 0.35f // Tension lines cross at (INFLEXION, 1)
        val DECELERATION_RATE = (ln(0.78) / ln(0.9)).toFloat()
        val ppi = context.activity.resources.displayMetrics.density * 160.0f
        val mPhysicalCoeff = (SensorManager.GRAVITY_EARTH // g (m/s^2)
                * 39.37f // inch/meter
                * ppi
                * 0.84f);
        val l =
            ln((INFLEXION * abs(velocity.toDouble()) / (SCROLL_FRICTION * mPhysicalCoeff)).toDouble())
        val decelMinusOne = DECELERATION_RATE - 1.0
        return ((SCROLL_FRICTION * mPhysicalCoeff
                * exp(DECELERATION_RATE / decelMinusOne * l))).toFloat()
    }

    private data class SnapCandidate(val view: View, val fx: Int, val fy: Int, val cx: Int, val cy: Int)
    private fun snaps() {
        println("snaps - $snapToElements")
        val configuration = ViewConfiguration.get(native.context)
        val mTouchSlop = configuration.scaledTouchSlop
        val mMinimumVelocity = configuration.scaledMinimumFlingVelocity
        val mMaximumVelocity = configuration.scaledMaximumFlingVelocity
        vx.computeCurrentVelocity(1000, mMaximumVelocity.toFloat())
        vy.computeCurrentVelocity(1000, mMaximumVelocity.toFloat())
        val cx = horizontalScrollView?.scrollX ?: 0
        val fx = horizontalScrollView?.let {
            val vel = vx.xVelocity.roundToInt()
            it.scrollX + getSplineFlingDistance(vel).roundToInt() * -vel.sign
        } ?: 0
        val fw = horizontalScrollView?.width ?: 0
        val cy = verticalScrollView?.scrollY ?: 0
        val fy = verticalScrollView?.let {
            val vel = vy.yVelocity.roundToInt()
            it.scrollY + getSplineFlingDistance(vel).roundToInt() * -vel.sign
        } ?: 0
        val fh = verticalScrollView?.height ?: 0
        val r = android.graphics.Rect(0, 0, 0, 0)
        var hOffset = Int.MAX_VALUE
        var vOffset = Int.MAX_VALUE
        println("Determining snap...")
        if(scrollSnapStop) {
            val candidates = innermostView.children.flatMap { (it as? ViewGroup)?.children ?: sequenceOf() }
                .map {
                    r.set(0, 0, it.width, it.height)
                    outermostView.offsetDescendantRectToMyCoords(it, r)
                    val offFX = when (snapToElements.first) {
                        Align.Start -> r.left - fx
                        Align.End -> r.right - (fx + fw)
                        Align.Center, Align.Stretch -> r.centerX() - (fx + fw / 2)
                        null -> Int.MAX_VALUE
                    }
                    val offFY = when (snapToElements.second) {
                        Align.Start -> r.top - fy
                        Align.End -> r.bottom - (fy + fh)
                        Align.Center, Align.Stretch -> r.centerY() - (fy + fh / 2)
                        null -> Int.MAX_VALUE
                    }
                    val offCX = when (snapToElements.first) {
                        Align.Start -> r.left - cx
                        Align.End -> r.right - (cx + fw)
                        Align.Center, Align.Stretch -> r.centerX() - (cx + fw / 2)
                        null -> Int.MAX_VALUE
                    }
                    val offCY = when (snapToElements.second) {
                        Align.Start -> r.top - cy
                        Align.End -> r.bottom - (cy + fh)
                        Align.Center, Align.Stretch -> r.centerY() - (cy + fh / 2)
                        null -> Int.MAX_VALUE
                    }
                    SnapCandidate(it, offFX, offFY, offCX, offCY)
                }
                .toList()
                .also { if(it.isEmpty()) return }
                .asSequence()
            val lowerXCandidate = candidates.filter { it.cx < 0 }.minByOrNull { abs(it.cx) } ?: candidates.minBy { it.cx }
            val upperXCandidate = candidates.filter { it.cx > 0 }.minByOrNull { abs(it.cx) } ?: candidates.maxBy { it.cx }
            val lowerYCandidate = candidates.filter { it.cy < 0 }.minByOrNull { abs(it.cy) } ?: candidates.minBy { it.cy }
            val upperYCandidate = candidates.filter { it.cy > 0 }.minByOrNull { abs(it.cy) } ?: candidates.maxBy { it.cy }
            hOffset = sequenceOf(lowerXCandidate, upperXCandidate).minBy { abs(it.fx) }.cx
            vOffset = sequenceOf(lowerYCandidate, upperYCandidate).minBy { abs(it.fy) }.cy
            println("Snaps!  $cx $cy ->| $fx $fy, $hOffset $vOffset")
        } else {
            var hFOffset = Int.MAX_VALUE
            var vFOffset = Int.MAX_VALUE
            innermostView.children.flatMap { (it as? ViewGroup)?.children ?: sequenceOf() }
                .forEach {
                    r.set(0, 0, it.width, it.height)
                    outermostView.offsetDescendantRectToMyCoords(it, r)
                    val offX = when (snapToElements.first) {
                        Align.Start -> r.left - fx
                        Align.End -> r.right - (fx + fw)
                        Align.Center, Align.Stretch -> r.centerX() - (fx + fw / 2)
                        null -> Int.MAX_VALUE
                    }
                    val offY = when (snapToElements.second) {
                        Align.Start -> r.top - fy
                        Align.End -> r.bottom - (fy + fh)
                        Align.Center, Align.Stretch -> r.centerY() - (fy + fh / 2)
                        null -> Int.MAX_VALUE
                    }
                    if (abs(offX) < abs(hFOffset)) {
                        hFOffset = offX
                        hOffset = when (snapToElements.first) {
                            Align.Start -> r.left - cx
                            Align.End -> r.right - (cx + fw)
                            Align.Center, Align.Stretch -> r.centerX() - (cx + fw / 2)
                            null -> Int.MAX_VALUE
                        }
                    }
                    if (abs(offY) < abs(vFOffset)) {
                        vFOffset = offY
                        vOffset = when (snapToElements.second) {
                            Align.Start -> r.top - cy
                            Align.End -> r.bottom - (cy + fh)
                            Align.Center, Align.Stretch -> r.centerY() - (cy + fh / 2)
                            null -> Int.MAX_VALUE
                        }
                    }
                }
            println("Snaps!  $cx $cy -> $fx $fy, $hOffset $vOffset")
        }
        if (hOffset != Int.MAX_VALUE && abs(hOffset) > 1)
            horizontalScrollView?.smoothScrollBy(hOffset, 0)
        if (vOffset != Int.MAX_VALUE && abs(vOffset) > 1)
            verticalScrollView?.smoothScrollBy(0, vOffset)
    }

    override fun internalAddChild(index: Int, view: RView) {
        (innermostView as ViewGroup).addView(view.native, index)
        if ((innermostView as ViewGroup).childCount != children.size) throw IllegalStateException("Native child count ${(innermostView as ViewGroup).childCount} != RView count ${children.size} on ${this::class.qualifiedName}")
    }

    override fun internalRemoveChild(index: Int) {
        if ((innermostView as ViewGroup).childCount != children.size) throw IllegalStateException("Native child count ${(innermostView as ViewGroup).childCount} != RView count ${children.size} on ${this::class.qualifiedName}")
        (innermostView as ViewGroup).removeViewAt(index)
    }

    override fun internalClearChildren() {
        if ((innermostView as ViewGroup).childCount != children.size) throw IllegalStateException("Native child count ${(innermostView as ViewGroup).childCount} != RView count ${children.size} on ${this::class.qualifiedName}")
        (innermostView as ViewGroup).removeAllViews()
    }

    override fun defaultLayoutParams(): ViewGroup.LayoutParams =
        FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

    override var showScrollBars: Boolean = true
        set(value) {
            field = value
            horizontalScrollView?.isHorizontalScrollBarEnabled = value
            verticalScrollView?.isVerticalScrollBarEnabled = value
        }
    override val viewport: Readable<Rect> = object : Readable<Rect>, Listenable by scrollChanged {
        override val state: ReadableState<Rect>
            get() = ReadableState(
                Rect.fromSize(
                    (horizontalScrollView?.scrollX ?: 0).toDouble(),
                    (verticalScrollView?.scrollY ?: 0).toDouble(),
                    native.width.toDouble(),
                    native.height.toDouble(),
                )
            )
    }
    override val content: Readable<Rect> = object : Readable<Rect>, BaseListenable() {
        override val state: ReadableState<Rect>
            get() = ReadableState(
                Rect.fromSize(
                    0.0,
                    0.0,
                    innermostView.children.firstOrNull()?.width?.toDouble() ?: 0.0,
                    innermostView.children.firstOrNull()?.height?.toDouble() ?: 0.0,
                )
            )

        var l: ViewTreeObserver.OnGlobalLayoutListener? = null
        override fun activate() {
            l = ViewTreeObserver.OnGlobalLayoutListener { invokeAllListeners() }
            innermostView.viewTreeObserver.addOnGlobalLayoutListener(l)
        }

        override fun deactivate() {
            innermostView.viewTreeObserver.removeOnGlobalLayoutListener(l)
        }
    }

    override var snapToElements: Pair<Align?, Align?> = null to null
    override var scrollSnapStop: Boolean = false
    private val _directlyInteractingWithScroller = Property(false)
    override val directlyInteractingWithScroller: Readable<Boolean> get() = _directlyInteractingWithScroller

    override fun scrollTo(left: Double, top: Double, animated: Boolean) {
        if (animated) {
            horizontalScrollView?.smoothScrollTo(left.roundToInt(), 0)
            verticalScrollView?.smoothScrollTo(0, top.roundToInt())
        } else {
            horizontalScrollView?.scrollTo(left.roundToInt(), 0)
            verticalScrollView?.scrollTo(0, top.roundToInt())
        }
    }

    override fun scrollTo(element: RView, horizontal: Align, vertical: Align, animated: Boolean) {
        scrollTo(
            left = when (horizontal) {
                Align.Start -> element.native.left
                Align.Center -> (element.native.right + element.native.left) / 2 - native.width / 2
                Align.End -> element.native.right - native.width
                Align.Stretch -> (element.native.right + element.native.left) / 2 - native.width / 2
            }.toDouble(),
            top = when (vertical) {
                Align.Start -> element.native.top
                Align.Center -> (element.native.bottom + element.native.top) / 2 - native.height / 2
                Align.End -> element.native.bottom - native.width
                Align.Stretch -> (element.native.bottom + element.native.top) / 2 - native.height / 2
            }.toDouble(),
            animated = animated
        )
    }

    override fun offset(x: Double, y: Double) {
        println("$horizontalScrollView?.scrollBy($x.roundToInt(), 0)")
        println("$verticalScrollView?.scrollBy(0, $y.roundToInt())")
        horizontalScrollView?.scrollBy(x.roundToInt(), 0)
        verticalScrollView?.scrollBy(0, y.roundToInt())
    }
}