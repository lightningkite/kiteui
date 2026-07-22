@file:OptIn(InternalKiteUi::class)

package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.InternalKiteUi
import com.lightningkite.kiteui.views.ElementWriter
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.*
import com.lightningkite.reactive.core.Reactive

inline fun <T, ID, C : ContainerElement> ElementWriter.renderListIn(
    container: ElementWriter.(C.() -> Unit) -> C,
    items: Reactive<List<T>>,
    noinline id: (T) -> ID,
    animate: Boolean = true,
    noinline beforeModifier: ViewWriter.() -> ElementWriter.CanAddListElementModifier = { this },
    noinline render: ElementWriter.CanAddSizing.(Reactive<T>) -> Unit
) = container {
    renderListKeyed(items, id, animate, beforeModifier, render)
}

inline fun <T, C : ContainerElement> ElementWriter.renderListIn(
    container: ElementWriter.(C.() -> Unit) -> C,
    items: Reactive<List<T>>,
    placeholdersWhileLoading: Int = 5,
    poolCap: Int = 32,
    noinline beforeModifier: ViewWriter.() -> ElementWriter.CanAddListElementModifier = { this },
    noinline render: ElementWriter.CanAddListElementModifier.(Reactive<T>) -> Unit
) = container {
    renderList(items, placeholdersWhileLoading, poolCap, beforeModifier, render)
}

inline fun <T, C : ContainerElement> ElementWriter.renderListInExpensive(
    container: ElementWriter.(C.() -> Unit) -> C,
    items: Reactive<List<T>>,
    animate: Boolean,
    noinline beforeModifier: ViewWriter.(T) -> ElementWriter.CanAddListElementModifier = { this },
    noinline render: ElementWriter.CanAddSizing.(T) -> Unit
) = container {
    renderListExpensive(items, animate, beforeModifier, render)
}

inline fun <T, C : ContainerElement> ElementWriter.renderListInExpensive(
    container: ElementWriter.(C.() -> Unit) -> C,
    items: Reactive<List<T>>,
    noinline beforeModifier: ViewWriter.(T) -> ElementWriter.CanAddListElementModifier = { this },
    noinline render: ElementWriter.CanAddListElementModifier.(T) -> Unit
) = container {
    renderListExpensive(items, beforeModifier, render)
}



fun <T, ID> ElementWriter.colOf(
    items: Reactive<List<T>>,
    id: (T) -> ID,
    animate: Boolean = true,
    preHidingModifiers: ViewWriter.() -> ElementWriter.CanAddListElementModifier = { this },
    render: ElementWriter.CanAddSizing.(Reactive<T>) -> Unit
): RowOrCol =
    renderListIn(ElementWriter::col, items, id, animate, preHidingModifiers, render)

fun <T> ElementWriter.colOf(
    items: Reactive<List<T>>,
    placeholdersWhileLoading: Int = 5,
    poolCap: Int = 32,
    beforeModifier: ViewWriter.() -> ElementWriter.CanAddListElementModifier = { this },
    render: ElementWriter.CanAddListElementModifier.(Reactive<T>) -> Unit
): RowOrCol =
    renderListIn(ElementWriter::col, items, placeholdersWhileLoading, poolCap, beforeModifier, render)

fun <T> ElementWriter.colOfExpensive(
    items: Reactive<List<T>>,
    animate: Boolean,
    beforeModifier: ViewWriter.(T) -> ElementWriter.CanAddListElementModifier = { this },
    render: ElementWriter.CanAddSizing.(T) -> Unit
): RowOrCol =
    renderListInExpensive(ElementWriter::col, items, animate, beforeModifier, render)

fun <T> ElementWriter.colOfExpensive(
    items: Reactive<List<T>>,
    beforeModifier: ViewWriter.(T) -> ElementWriter.CanAddListElementModifier = { this },
    render: ElementWriter.CanAddListElementModifier.(T) -> Unit
): RowOrCol =
    renderListInExpensive(ElementWriter::col, items, beforeModifier, render)


fun <T, ID> ElementWriter.rowOf(
    items: Reactive<List<T>>,
    id: (T) -> ID,
    animate: Boolean = true,
    preHidingModifiers: ViewWriter.() -> ElementWriter.CanAddListElementModifier = { this },
    render: ElementWriter.CanAddSizing.(Reactive<T>) -> Unit
): RowOrCol =
    renderListIn(ElementWriter::row, items, id, animate, preHidingModifiers, render)

fun <T> ElementWriter.rowOf(
    items: Reactive<List<T>>,
    placeholdersWhileLoading: Int = 5,
    poolCap: Int = 32,
    beforeModifier: ViewWriter.() -> ElementWriter.CanAddListElementModifier = { this },
    render: ElementWriter.CanAddListElementModifier.(Reactive<T>) -> Unit
): RowOrCol =
    renderListIn(ElementWriter::row, items, placeholdersWhileLoading, poolCap, beforeModifier, render)

fun <T> ElementWriter.rowOfExpensive(
    items: Reactive<List<T>>,
    animate: Boolean,
    beforeModifier: ViewWriter.(T) -> ElementWriter.CanAddListElementModifier = { this },
    render: ElementWriter.CanAddSizing.(T) -> Unit
): RowOrCol =
    renderListInExpensive(ElementWriter::row, items, animate, beforeModifier, render)

fun <T> ElementWriter.rowOfExpensive(
    items: Reactive<List<T>>,
    beforeModifier: ViewWriter.(T) -> ElementWriter.CanAddListElementModifier = { this },
    render: ElementWriter.CanAddListElementModifier.(T) -> Unit
): RowOrCol =
    renderListInExpensive(ElementWriter::row, items, beforeModifier, render)