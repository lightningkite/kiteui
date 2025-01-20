package com.lightningkite.kiteui.views.direct

import android.annotation.SuppressLint
import android.os.Build
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import androidx.core.view.children
import androidx.core.widget.NestedScrollView
import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.Rect
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView
import com.lightningkite.kiteui.views.RViewWrapper
import kotlin.math.roundToInt

class ScrollView constructor(
    context: RContext,
    override val horizontal: Boolean,
    override val vertical: Boolean
) : RViewWrapper(context), ScrollingBehaviors {
    private val scrollChanged = BasicListenable()
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
        var down = 0
        setOnTouchListener { v, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    down++
                    _directlyInteractingWithScroller.value = down != 0
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    down--
                    _directlyInteractingWithScroller.value = down != 0
                }
            }
            true
        }
        isFillViewport = true
    } else null
    private val innermostView: ViewGroup =
        horizontalScrollView ?: verticalScrollView ?: error("ScrollView requires at least one axis.")
    private val outermostView: ViewGroup =
        verticalScrollView ?: horizontalScrollView ?: error("ScrollView requires at least one axis.")
    override val native: View = outermostView

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