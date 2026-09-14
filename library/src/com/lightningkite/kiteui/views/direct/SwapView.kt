package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.ScreenTransition
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.navigation.render
import com.lightningkite.kiteui.report
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.context.reactive
import com.lightningkite.reactive.core.Reactive

public expect class SwapView(context: ElementContext) : ElementWithChildren, NativeElement {
    override val underlyingNativeElement: SwapView
    override val children: List<Element>

    public fun swap(
        transition: ScreenTransition = ScreenTransition.Fade,
        createNewView: ViewWriter.() -> Unit
    )
}

public inline fun <T> SwapView.swapping(
    crossinline transition: (T) -> ScreenTransition = { ScreenTransition.Fade },
    crossinline current: ReactiveContext.() -> T,
    crossinline views: ViewWriter.(T) -> Unit
) {
    val queue = ArrayList<T>()
    var alreadySwapping = false
    // A "redirect page" (show a spinner in render(), then swap itself out from a coroutine
    // launched in that same render()) navigates re-entrantly whenever the coroutine doesn't
    // actually suspend: the navigator's stack mutates while this very calculation is still
    // running, which TypedReactiveContext treats as the calculation triggering itself. A
    // reentrancyLimit of 0 (the default) reports that as a mistake and drops the update; a
    // positive limit lets it settle instead, rerunning this block so the queue/alreadySwapping
    // loop below picks up the new page - see ReactiveContext's "Self-Triggering Calculations" doc.
    // A handful of settle passes comfortably covers chained redirects (an auth gate redirecting
    // into an onboarding gate, say); a real redirect loop past that surfaces as a
    // ReactiveReentrancyException instead of silently freezing the screen.
    reactive(reentrancyLimit = 8) {
        val c = current(this)
        queue.add(c)
        if (alreadySwapping) {
            return@reactive
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