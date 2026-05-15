@file:OptIn(InternalKiteUi::class)

package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.InternalKiteUi
import com.lightningkite.kiteui.OverrideOnly
import com.lightningkite.kiteui.models.ScreenTransition
import com.lightningkite.kiteui.report
import com.lightningkite.kiteui.views.ElementWriter
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.*
import com.lightningkite.reactive.context.ReactiveContext
import com.lightningkite.reactive.context.reactive
import com.lightningkite.reactive.core.Reactive
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract


fun <T, ID> ElementWriter.col(
    items: Reactive<List<T>>,
    id: (T) -> ID,
    preHidingModifiers: ViewWriter.(ID) -> ElementWriter.CanAddShownWhen = { this },
    render: ElementWriter.CanAddTheme.(Reactive<T>) -> Unit
) = col {
    forEachById(items, id, preHidingModifiers, render)
}
fun <T, ID> ElementWriter.row(
    items: Reactive<List<T>>,
    id: (T) -> ID,
    preHidingModifiers: ViewWriter.(ID) -> ElementWriter.CanAddShownWhen = { this },
    render: ElementWriter.CanAddTheme.(Reactive<T>) -> Unit
) = row {
    forEachById(items, id, preHidingModifiers, render)
}

fun <T, ID> ElementWriter.colWithoutAnimations(
    items: Reactive<List<T>>,
    id: (T) -> ID,
    render: ElementWriter.CanAddTheme.(Reactive<T>) -> Unit
) = col {
    forEachByIdWithoutAnimation(items, id, render)
}
fun <T, ID> ElementWriter.rowWithoutAnimations(
    items: Reactive<List<T>>,
    id: (T) -> ID,
    render: ElementWriter.CanAddTheme.(Reactive<T>) -> Unit
) = row {
    forEachByIdWithoutAnimation(items, id, render)
}

fun <T> ElementWriter.col(
    items: Reactive<List<T>>,
    render: ElementWriter.CanAddTheme.(T) -> Unit
) = col {
    forEach(items, render)
}
fun <T> ElementWriter.row(
    items: Reactive<List<T>>,
    render: ElementWriter.CanAddTheme.(T) -> Unit
) = row {
    forEach(items, render)
}

inline fun <T> ElementWriter.swapping(
    crossinline transition: (T) -> ScreenTransition = { ScreenTransition.Fade },
    crossinline current: ReactiveContext.() -> T,
    crossinline views: ViewWriter.(T) -> Unit
): SwapView {
    return swapView {
        swapping(transition, current, views)
    }
}


fun ContainerElement.atIndex(index: Int): ViewWriter {
    val writer = object : ViewWriter by this {
        override val context: ElementContext
            get() = this@atIndex.context
        @OptIn(OverrideOnly::class)
        override fun willAddChild(element: Element) {
            element.underlyingNativeElement.parent = this@atIndex
        }
        @OptIn(OverrideOnly::class)
        override fun addChild(element: Element) {
            this@atIndex.addChild(index, element)
        }
    }
    return writer
}
