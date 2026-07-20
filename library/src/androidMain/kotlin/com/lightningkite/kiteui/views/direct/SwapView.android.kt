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


public actual class SwapView actual constructor(context: ElementContext) : NativeContainerElement(context) {
    actual override val underlyingNativeElement: SwapView get() = this

    override val native = FrameLayout(context.activity)

    public companion object {
        public val swapTimeMakeViewPerformance = PerformanceInfo("swapTimeMakeView")
        public val swapTimeAddViewsPerformance = PerformanceInfo("swapTimeAddViews")
    }

    public actual fun swap(
        transition: ScreenTransition,
        createNewView: ViewWriter.() -> Unit,
    ) {
        native.visibility = View.VISIBLE
        val oldView = this.children.firstOrNull()
        var newViewHolder: Element? = null
        val writer = object : ViewWriter, CalculationContext by this {
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
        swapTimeAddViewsPerformance {
            newView?.native?.layoutParams = newView?.native?.layoutParams?.also {
                it.width = ViewGroup.LayoutParams.MATCH_PARENT
                it.height = ViewGroup.LayoutParams.MATCH_PARENT
            } ?: FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            oldView?.let { old ->
                old.animateOut(transition) {
                    removeChild(old)
                    if (newView == null) native.visibility = View.GONE
                }
            }
            newView?.let { addChild(it) }
            newView?.animateIn(transition) {}
        }
    }
}

