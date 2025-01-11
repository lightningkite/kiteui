package com.lightningkite.kiteui.views.direct

import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import androidx.core.view.children
import androidx.core.widget.NestedScrollView
import com.lightningkite.kiteui.models.Rect
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView
import kotlin.math.roundToInt

actual class ScrollView actual constructor(
    context: RContext,
    actual val horizontal: Boolean,
    actual val vertical: Boolean
) : RView(context) {
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

    actual var showScrollBars: Boolean = true
        set(value) {
            field = value
            horizontalScrollView?.isHorizontalScrollBarEnabled = value
            verticalScrollView?.isVerticalScrollBarEnabled = value
        }
    actual val scrollReason: Readable<ScrollReason>
        get() = TODO("Not yet implemented")
    actual val viewport: Readable<Rect> = object : Readable<Rect>, Listenable by scrollChanged {
        override val state: ReadableState<Rect>
            get() = ReadableState(
                Rect(
                    (horizontalScrollView?.scrollX ?: 0).toDouble(),
                    (verticalScrollView?.scrollY ?: 0).toDouble(),
                    native.width.toDouble(),
                    native.height.toDouble(),
                )
            )
    }
    actual val content: Readable<Rect> = object : Readable<Rect>, BaseListenable() {
        override val state: ReadableState<Rect>
            get() = ReadableState(
                Rect(
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

    actual fun scrollTo(left: Double, top: Double, animated: Boolean) {
        if (animated) {
            horizontalScrollView?.smoothScrollTo(left.roundToInt(), 0)
            verticalScrollView?.smoothScrollTo(0, top.roundToInt())
        } else {
            horizontalScrollView?.scrollTo(left.roundToInt(), 0)
            verticalScrollView?.scrollTo(0, top.roundToInt())
        }
    }

    actual fun offset(x: Double, y: Double) {
        horizontalScrollView?.scrollBy(x.roundToInt(), 0)
        verticalScrollView?.scrollBy(0, y.roundToInt())
    }
}