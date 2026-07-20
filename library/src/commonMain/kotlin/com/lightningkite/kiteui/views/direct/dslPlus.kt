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


public fun <T, ID> ElementWriter.colOf(
    items: Reactive<List<T>>,
    id: (T) -> ID,
    preHidingModifiers: ViewWriter.(ID) -> ElementWriter.CanAddListElementModifier = { this },
    render: ElementWriter.CanAddTheme.(Reactive<T>) -> Unit
) = col {
    forEachById(items, id, preHidingModifiers, render = render)
}
public fun <T, ID> ElementWriter.rowOf(
    items: Reactive<List<T>>,
    id: (T) -> ID,
    preHidingModifiers: ViewWriter.(ID) -> ElementWriter.CanAddListElementModifier = { this },
    render: ElementWriter.CanAddTheme.(Reactive<T>) -> Unit
) = row {
    forEachById(items, id, preHidingModifiers, render = render)
}

public fun <T> ElementWriter.colOf(
    items: Reactive<List<T>>,
    placeholdersWhileLoading: Int = 5,
    render: ElementWriter.CanAddListElementModifier.(Reactive<T>) -> Unit
) = col {
    forEachUpdating(items, placeholdersWhileLoading, render = render)
}
public fun <T> ElementWriter.rowOf(
    items: Reactive<List<T>>,
    placeholdersWhileLoading: Int = 5,
    render: ElementWriter.CanAddListElementModifier.(Reactive<T>) -> Unit
) = row {
    forEachUpdating(items, placeholdersWhileLoading, render = render)
}
public fun <T> ElementWriter.rowWrappingOf(
    items: Reactive<List<T>>,
    placeholdersWhileLoading: Int = 5,
    render: ElementWriter.CanAddListElementModifier.(Reactive<T>) -> Unit
) = rowWrapping {
    forEachUpdating(items, placeholdersWhileLoading, render = render)
}

public inline fun <T> ElementWriter.swapping(
    crossinline transition: (T) -> ScreenTransition = { ScreenTransition.Fade },
    crossinline current: ReactiveContext.() -> T,
    crossinline views: ViewWriter.(T) -> Unit
): SwapView {
    return swapView {
        swapping(transition, current, views)
    }
}


public fun ContainerElement.atIndex(index: Int): ViewWriter {
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
