package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.models.ScreenTransition
import com.lightningkite.kiteui.report
import com.lightningkite.kiteui.views.*
import com.lightningkite.signal.ReactiveContext
import com.lightningkite.signal.reactiveScope


expect class SwapView(context: RContext) : RView {
    fun swap(transition: ScreenTransition = ScreenTransition.Fade, createNewView: ViewWriter.() -> ViewModifiable?): Unit
}

inline fun <T> SwapView.swapping(
    crossinline transition: (T) -> ScreenTransition = { ScreenTransition.Fade },
    crossinline current: ReactiveContext.() -> T,
    crossinline views: ViewWriter.(T) -> ViewModifiable?
): Unit {
    val queue = ArrayList<T>()
    var alreadySwapping = false
    reactiveScope {
        val c  = current(this)
        queue.add(c)
        if (alreadySwapping) {
            return@reactiveScope
        }
        alreadySwapping = true
        while (queue.isNotEmpty()) {
            val next = queue.removeAt(0)
            try {
                swap(transition(next)) { views(next) }
            } catch (e: Exception) {
                Exception("Failed to render $next", e).report("SwapView.swapping")
            }
        }
        alreadySwapping = false
    }
}