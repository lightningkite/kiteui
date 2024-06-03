package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.models.ScreenTransition
import com.lightningkite.kiteui.reactive.reactiveScope
import com.lightningkite.kiteui.views.*
import kotlin.time.measureTime


expect class SwapView(context: RContext) : RView {

    fun swap(transition: ScreenTransition = ScreenTransition.Fade, createNewView: ViewWriter.() -> Unit): Unit

}

inline fun <T> SwapView.swapping(
    crossinline transition: (T) -> ScreenTransition = { ScreenTransition.Fade },
    crossinline current: suspend () -> T,
    crossinline views: ViewWriter.(T) -> Unit
): Unit {
    val queue = ArrayList<T>()
    var alreadySwapping = false
    reactiveScope {
        val c = current()
        queue.add(c)
        if (alreadySwapping) {
            return@reactiveScope
        }
        alreadySwapping = true
        while (queue.isNotEmpty()) {
            val next = queue.removeAt(0)
            try {
                measureTime {
                    swap(transition(next)) { views(next) }
                }.also { println("Took ${it.inWholeMilliseconds}ms to swap") }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        alreadySwapping = false
    }
}