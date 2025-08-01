package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.ScreenTransition
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.report
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*


public expect class SwapView(context: RContext) : RView {
    public fun swap(transition: ScreenTransition = ScreenTransition.Fade, createNewView: ViewWriter.() -> ViewModifiable?): Unit
}

public inline fun <T> SwapView.swapping(
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