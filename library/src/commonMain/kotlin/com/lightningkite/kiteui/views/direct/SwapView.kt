package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.ScreenTransition
import com.lightningkite.kiteui.views.*
import kotlin.jvm.JvmInline
import kotlin.contracts.*
import kotlin.time.measureTime

expect class NSwapView : NView

@JvmInline
value class SwapView(override val native: NSwapView) : RView<NSwapView>

@ViewDsl
expect fun ViewWriter.swapViewActual(setup: SwapView.() -> Unit = {}): Unit
@OptIn(ExperimentalContracts::class)
@ViewDsl
inline fun ViewWriter.swapView(noinline setup: SwapView.() -> Unit = {}) {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }; swapViewActual(setup)
}

@ViewDsl
expect fun ViewWriter.swapViewDialogActual(setup: SwapView.() -> Unit = {}): Unit
@OptIn(ExperimentalContracts::class)
@ViewDsl
inline fun ViewWriter.swapViewDialog(noinline setup: SwapView.() -> Unit = {}) {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }; swapViewDialogActual(setup)
}

expect fun SwapView.swap(
    transition: ScreenTransition = ScreenTransition.Fade,
    createNewView: ViewWriter.() -> Unit
): Unit

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