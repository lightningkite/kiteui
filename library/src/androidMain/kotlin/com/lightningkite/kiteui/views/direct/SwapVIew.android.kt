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
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.measureTime

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual class NSwapView(context: Context) : SlightlyModifiedFrameLayout(context) {
    lateinit var viewWriter: ViewWriter
    var duration = 0.15.seconds
}

@ViewDsl
actual inline fun ViewWriter.swapViewActual(crossinline setup: SwapView.() -> Unit) {
    return viewElement(factory = ::NSwapView, wrapper = ::SwapView, setup = {
        val theme = currentTheme
        reactiveScope {
            native.duration = theme().transitionDuration
        }
        native.viewWriter = newViews()
        setup(this)
    })
}

@ViewDsl
actual inline fun ViewWriter.swapViewDialogActual(crossinline setup: SwapView.() -> Unit) {
    return viewElement(factory = ::NSwapView, wrapper = ::SwapView, setup = {
        val theme = currentTheme
        reactiveScope {
            native.duration = theme().transitionDuration
        }
        native.viewWriter = newViews()
        native.visibility = View.GONE
        setup(this)
    })
}

actual fun SwapView.swap(
    transition: ScreenTransition,
    createNewView: ViewWriter.() -> Unit,
) {
    measureTime {
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
            newView?.let { transition.enter?.setDuration(native.duration.inWholeMilliseconds)?.addTarget(it) }
            oldView?.let { transition.exit?.setDuration(native.duration.inWholeMilliseconds)?.addTarget(it) }
            transition.exit?.let { addTransition(it) }
            transition.enter?.let { addTransition(it) }
        })
        oldView?.let { oldNN -> native.removeView(oldNN); oldNN.shutdown() }
        newView?.let { native.addView(it) }
        if (newView == null) native.visibility = View.GONE
        else native.visibility = View.VISIBLE
    }.also { println("Took ${it.inWholeMilliseconds}ms to swap") }
}