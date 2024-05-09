package com.lightningkite.kiteui.views.direct

import ViewWriter
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.transition.TransitionManager
import androidx.transition.TransitionSet
import com.lightningkite.kiteui.models.ScreenTransition
import com.lightningkite.kiteui.views.*


actual class SwapView actual constructor(context: RContext): RView(context) {
    override val native = FrameLayout(context.activity).apply {
        isClickable = true
    }
    actual fun swap(
        transition: ScreenTransition,
        createNewView: ViewWriter.() -> Unit,
    ) {
        val oldView = this.children.firstOrNull()
        var newView: RView? = null
        val writer = object: ViewWriter() {
            override val context: RContext
                get() = this@SwapView.context

            override fun addChild(view: RView) {
                newView = view
            }
        }
        animationsEnabled = false
        try {
            writer.createNewView()
        } finally {
            animationsEnabled = true
        }
        newView?.native?.layoutParams = newView?.native?.layoutParams?.also {
            it.width = ViewGroup.LayoutParams.MATCH_PARENT
            it.height = ViewGroup.LayoutParams.MATCH_PARENT
        } ?: FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        TransitionManager.beginDelayedTransition(native, TransitionSet().apply {
            newView?.let { transition.enter?.setDuration(theme.transitionDuration.inWholeMilliseconds)?.addTarget(it.native) }
            oldView?.let { transition.exit?.setDuration(theme.transitionDuration.inWholeMilliseconds)?.addTarget(it.native) }
            transition.exit?.let { addTransition(it) }
            transition.enter?.let { addTransition(it) }
        })
        oldView?.let { oldNN -> removeChild(oldNN) }
        newView?.let { addChild(it) }
        if(newView == null) native.visibility = View.GONE
        else native.visibility = View.VISIBLE
    }
}
