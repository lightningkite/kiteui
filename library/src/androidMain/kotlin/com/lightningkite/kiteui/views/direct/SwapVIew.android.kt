package com.lightningkite.kiteui.views.direct

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.transition.TransitionManager
import androidx.transition.TransitionSet
import com.lightningkite.kiteui.models.ScreenTransition
import com.lightningkite.kiteui.views.*
import java.util.WeakHashMap

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual class NSwapView(context: Context): SlightlyModifiedFrameLayout(context) {
    lateinit var viewWriter: ViewWriter
}

@ViewDsl
actual inline fun ViewWriter.swapViewActual(crossinline setup: SwapView.() -> Unit) {
    return viewElement(factory = ::NSwapView, wrapper = ::SwapView, setup = {
        native.viewWriter = newViews()
        setup(this)
    })
}

@ViewDsl
actual inline fun ViewWriter.swapViewDialogActual(crossinline setup: SwapView.() -> Unit) {
    return viewElement(factory = ::NSwapView, wrapper = ::SwapView, setup = {
        native.viewWriter = newViews()
        native.visibility = View.GONE
        setup(this)
    })
}

actual fun SwapView.swap(
    transition: ScreenTransition,
    createNewView: ViewWriter.() -> Unit,
) {
    val oldView = this.native.getChildAt(0)
    native.viewWriter.rootCreated = null
    animationsEnabled = false
    try {
        native.viewWriter.createNewView()
    } finally {
        animationsEnabled = true
    }
    val newView = native.viewWriter.rootCreated
    newView?.layoutParams = newView?.layoutParams?.also {
        it.width = ViewGroup.LayoutParams.MATCH_PARENT
        it.height = ViewGroup.LayoutParams.MATCH_PARENT
    } ?: FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    TransitionManager.beginDelayedTransition(native, TransitionSet().apply {
        oldView?.let { transition.exit?.addTarget(it) }
        newView?.let { transition.enter?.addTarget(it) }
        transition.enter?.let { addTransition(it) }
        transition.exit?.let { addTransition(it) }
    })
    oldView?.let { oldNN -> native.removeView(oldNN); oldNN.shutdown() }
    newView?.let { native.addView(it) }
    if(newView == null) native.visibility = View.GONE
    else native.visibility = View.VISIBLE
}