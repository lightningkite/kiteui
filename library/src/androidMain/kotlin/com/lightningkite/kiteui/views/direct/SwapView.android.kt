package com.lightningkite.kiteui.views.direct

import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.lightningkite.kiteui.OverrideOnly
import com.lightningkite.kiteui.PerformanceInfo
import com.lightningkite.kiteui.models.ScreenTransition
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.reactive.context.*
import kotlinx.coroutines.CoroutineScope


public actual class SwapView actual constructor(context: ElementContext) : NativeContainerElement(context) {
    actual override val underlyingNativeElement: SwapView get() = this

    override val native: FrameLayout = FrameLayout(context.activity)

    private var currentView: Element? = this.children.firstOrNull()

    public companion object {
        internal val swapTimeMakeViewPerformance: PerformanceInfo = PerformanceInfo("swapTimeMakeView")
        internal val swapTimeAddViewsPerformance: PerformanceInfo = PerformanceInfo("swapTimeAddViews")
    }

    public actual fun swap(
        transition: ScreenTransition,
        createNewView: ViewWriter.() -> Unit,
    ) {
        native.visibility = View.VISIBLE
        val oldView = this.currentView
        var newViewHolder: Element? = null
        val writer = object : ViewWriter, CoroutineScope by this {
            override val context: ElementContext
                get() = this@SwapView.context

            @OverrideOnly
            override fun willAddChild(element: Element) {
                element.underlyingNativeElement.parent = this@SwapView
            }

            @OverrideOnly
            override fun addChild(element: Element) {
                newViewHolder = element
            }
        }
        animationsEnabled = false
        try {
            swapTimeMakeViewPerformance {
                writer.createNewView()
            }
        } finally {
            animationsEnabled = true
        }
        val newView = newViewHolder
        currentView = newView
        swapTimeAddViewsPerformance {
            newView?.native?.let { native ->
                native.layoutParams = native.layoutParams?.also {
                    it.width = ViewGroup.LayoutParams.MATCH_PARENT
                    it.height = ViewGroup.LayoutParams.MATCH_PARENT
                } ?: FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            }
            oldView?.let { old ->
                old.animateOut(transition) {
                    removeChild(old)
                    if (currentView == null) native.visibility = View.GONE
                }
            }
            newView?.let { addChild(it) }
            newView?.animateIn(transition) {}
        }
    }
}

