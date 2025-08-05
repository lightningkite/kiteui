package com.lightningkite.kiteui.views.direct

import android.annotation.SuppressLint
import android.hardware.SensorManager
import android.os.Build
import android.view.*
import android.view.ViewTreeObserver.OnPreDrawListener
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.OverScroller
import androidx.core.view.children
import androidx.core.widget.NestedScrollView
import com.lightningkite.kiteui.WeakReference
import com.lightningkite.kiteui.afterTimeout
import com.lightningkite.kiteui.checkLeakAfterDelay
import com.lightningkite.kiteui.debugPrint
import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.Rect
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.viewDebugTarget
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView
import com.lightningkite.kiteui.views.RViewWrapper
import com.lightningkite.kiteui.views.debugPrint
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*
import java.lang.reflect.Modifier
import kotlin.math.*

public class ScrollView constructor(
    context: RContext,
    public override val horizontal: Boolean,
    public override val vertical: Boolean
) : RViewWrapper(context), ScrollingBehaviors {
    private val scrollChanged = BasicListenable()
    private var vx = VelocityTracker.obtain()
    private var vy = VelocityTracker.obtain()
    public override val native: TwoWayNestedScrollView = TwoWayNestedScrollView(context.activity).apply {
        lockX = !horizontal
        lockY = !vertical
        isFillViewport = true
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
                    vx.recycle()
                    vx = VelocityTracker.obtain()
                    vy.recycle()
                    vy = VelocityTracker.obtain()
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
            false
        }
    }

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
        val configuration = ViewConfiguration.get(native.context)
        val mTouchSlop = configuration.scaledTouchSlop
        val mMinimumVelocity = configuration.scaledMinimumFlingVelocity
        val mMaximumVelocity = configuration.scaledMaximumFlingVelocity
        vx.computeCurrentVelocity(1000, mMaximumVelocity.toFloat())
        vy.computeCurrentVelocity(1000, mMaximumVelocity.toFloat())
        val cx = native.scrollX
        val fx = native.let {
            val vel = vx.xVelocity.roundToInt()
            it.scrollX + getSplineFlingDistance(vel).roundToInt() * -vel.sign
        }
        val fw = native.width
        val cy = native.scrollY
        val fy = native.let {
            val vel = vy.yVelocity.roundToInt()
            it.scrollY + getSplineFlingDistance(vel).roundToInt() * -vel.sign
        }
        val fh = native.height
        val r = android.graphics.Rect(0, 0, 0, 0)
        var hOffset = Int.MAX_VALUE
        var vOffset = Int.MAX_VALUE
        if (scrollSnapStop) {
            val candidates = native.children.flatMap { (it as? ViewGroup)?.children ?: sequenceOf() }
                .map {
                    r.set(0, 0, it.width, it.height)
                    native.offsetDescendantRectToMyCoords(it, r)
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
                .also { if (it.isEmpty()) return }
                .asSequence()
            val lowerXCandidate =
                candidates.filter { it.cx < 0 }.minByOrNull { abs(it.cx) } ?: candidates.minBy { it.cx }
            val upperXCandidate =
                candidates.filter { it.cx > 0 }.minByOrNull { abs(it.cx) } ?: candidates.maxBy { it.cx }
            val lowerYCandidate =
                candidates.filter { it.cy < 0 }.minByOrNull { abs(it.cy) } ?: candidates.minBy { it.cy }
            val upperYCandidate =
                candidates.filter { it.cy > 0 }.minByOrNull { abs(it.cy) } ?: candidates.maxBy { it.cy }
            hOffset = sequenceOf(lowerXCandidate, upperXCandidate).minBy { abs(it.fx) }.cx
            vOffset = sequenceOf(lowerYCandidate, upperYCandidate).minBy { abs(it.fy) }.cy
        } else {
            var hFOffset = Int.MAX_VALUE
            var vFOffset = Int.MAX_VALUE
            native.children.flatMap { (it as? ViewGroup)?.children ?: sequenceOf() }
                .forEach {
                    r.set(0, 0, it.width, it.height)
                    native.offsetDescendantRectToMyCoords(it, r)
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
        }
        if (hOffset != Int.MAX_VALUE && abs(hOffset) > 1)
            native.smoothScrollBy(hOffset, 0)
        if (vOffset != Int.MAX_VALUE && abs(vOffset) > 1)
            native.smoothScrollBy(0, vOffset)
    }

    public override fun defaultLayoutParams(): ViewGroup.LayoutParams =
        FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

    public override var showScrollBars: Boolean = true
        set(value) {
            field = value
            native.isHorizontalScrollBarEnabled = value
            native.isVerticalScrollBarEnabled = value
        }
    public override val viewport: Reactive<Rect> = object : Reactive<Rect>, Listenable by scrollChanged {
        override val state: ReactiveState<Rect>
            get() {
                debugPrint { "Reading actual viewport, got ${native.scrollX}, ${native.scrollY}" }
                return ReactiveState(
                    Rect.fromSize(
                        (native.scrollX ?: 0).toDouble(),
                        (native.scrollY ?: 0).toDouble(),
                        native.width.toDouble(),
                        native.height.toDouble(),
                    )
                )
            }
    }
    public override val content: Reactive<Rect> = object : Reactive<Rect>, BaseListenable() {
        override val state: ReactiveState<Rect>
            get() = ReactiveState(
                Rect.fromSize(
                    0.0,
                    0.0,
                    native.children.firstOrNull()?.width?.toDouble() ?: 0.0,
                    native.children.firstOrNull()?.height?.toDouble() ?: 0.0,
                )
            )

        var l: ViewTreeObserver.OnGlobalLayoutListener? = null
        override fun activate() {
            l = ViewTreeObserver.OnGlobalLayoutListener { invokeAllListeners() }
            native.viewTreeObserver.addOnGlobalLayoutListener(l)
        }

        override fun deactivate() {
            native.viewTreeObserver.removeOnGlobalLayoutListener(l)
        }
    }

    public override var snapToElements: Pair<Align?, Align?> = null to null
    public override var scrollSnapStop: Boolean = false
    private val _directlyInteractingWithScroller = Signal(false)
    public override val directlyInteractingWithScroller: Reactive<Boolean> get() = _directlyInteractingWithScroller

    public override fun scrollTo(left: Double, top: Double, animated: Boolean) {
        if (animated) {
            native.smoothScrollTo(left.roundToInt(), top.roundToInt())
        } else {
            native.scrollTo(left.roundToInt(), top.roundToInt())
        }
    }

    public override fun scrollTo(element: RView, horizontal: Align, vertical: Align, animated: Boolean) {
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

    init {
        // We use a weak reference because for some reason, the onremove cleanup doesn't get rid of the listener.
        val weak = WeakReference(this)
        val l = OnPreDrawListener {
            val self = weak.get() ?: return@OnPreDrawListener true
            var x = self.queuedJumpX
            var y = self.queuedJumpY
            if (self.queuedJumpX == -1.0 && self.queuedJumpY == -1.0) return@OnPreDrawListener true
            self.queuedJumpX = -1.0
            self.queuedJumpY = -1.0
            self.native.scrollTo(x.roundToInt(), y.roundToInt())
            true
        }
        native.viewTreeObserver.addOnPreDrawListener(l)
        onRemove {
            native.viewTreeObserver.removeOnPreDrawListener(l)
        }
    }

    public var queuedJumpX: Double = -1.0
    public var queuedJumpY: Double = -1.0
    public override fun scrollToKeepAnimations(x: Double, y: Double) {
//        native.mScroller?.abortAnimation()
        queuedJumpX = x
        queuedJumpY = y
        native.children.firstOrNull()?.debugPrint { "Scrolling queued $queuedJumpX $queuedJumpY" }
    }
}