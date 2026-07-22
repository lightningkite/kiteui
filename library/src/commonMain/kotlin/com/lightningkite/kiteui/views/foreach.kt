package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.InternalKiteUi
import com.lightningkite.kiteui.OverrideOnly
import com.lightningkite.kiteui.afterTimeout
import com.lightningkite.kiteui.views.direct.RowOrCol
import com.lightningkite.kiteui.views.direct.asListItem
import com.lightningkite.kiteui.views.direct.setupAsListContainer
import com.lightningkite.kiteui.views.direct.shownWhen
import com.lightningkite.reactive.context.reactive
import com.lightningkite.reactive.core.LateInitSignal
import com.lightningkite.reactive.core.Reactive
import com.lightningkite.reactive.core.Signal
import com.lightningkite.reactive.extensions.value
import kotlin.math.min

/**
 * **INTERNAL API:** Renders [items] using positional slot reuse with optional placeholder loading states.
 *
 * Views are created once and reused by position as the list changes. This is efficient
 * for lists where items can change but order/count is relatively stable.
 *
 * **Do not call directly.** Use [colOf]/[rowOf] for common cases, or [renderListIn] for custom containers:
 * ```kotlin
 * renderListIn(ElementWriter::yourContainer, items, placeholders, poolCap, beforeModifier, render)
 * ```
 *
 * @param placeholders Number of placeholder items to show while data loads (0 = no placeholders)
 * @param poolCap Maximum number of hidden views to retain beyond current list size (default 32)
 */
@PublishedApi
internal fun <T> ContainerElement.renderList(
    items: Reactive<List<T>>,
    placeholders: Int,
    poolCap: Int = 32,
    beforeModifier: ViewWriter.() -> ElementWriter.CanAddListElementModifier,
    render: ElementWriter.CanAddListElementModifier.(Reactive<T>) -> Unit
) {
    renderListPositional(items, placeholders, poolCap, beforeModifier, render)
}

/**
 * **INTERNAL API:** Renders [items] using keyed ID diffing with optional animation.
 *
 * Each item is tracked by [id]. When the list changes, items are matched by ID:
 * matched views have their data updated in-place; new IDs trigger insertion;
 * removed IDs trigger deletion. This is the most efficient strategy when items
 * have stable identities and can be reordered, inserted, or removed.
 *
 * **Do not call directly.** Use [colOf]/[rowOf] for common cases, or [renderListIn] for custom containers:
 * ```kotlin
 * renderListIn(ElementWriter::yourContainer, items, id, animate, beforeModifier, render)
 * ```
 *
 * @param animate If true, uses [shownWhen] for enter/exit transitions (default).
 *                When animated, render receives [CanAddSizing]. When not animated,
 *                render receives [CanAddListElementModifier] directly.
 */
@PublishedApi
internal fun <T, ID> ContainerElement.renderListKeyed(
    items: Reactive<List<T>>,
    id: (T) -> ID,
    animate: Boolean = true,
    beforeModifier: ViewWriter.() -> ElementWriter.CanAddListElementModifier = { this },
    render: ElementWriter.CanAddSizing.(Reactive<T>) -> Unit
) {
    if (animate) {
        renderListKeyedAnimated(items, id, beforeModifier, render)
    } else {
        renderListKeyedNoAnimation(items, id, beforeModifier, render)
    }
}

/**
 * **INTERNAL API:** Renders [items] with full rebuild on every change, optionally animated.
 *
 * **PERFORMANCE WARNING:** This clears and recreates all views whenever the list changes.
 * Use this only as a last resort when keyed or positional strategies don't apply.
 *
 * **Do not call directly.** Use [colOfExpensive]/[rowOfExpensive] for common cases, or [renderListInExpensive] for custom containers:
 * ```kotlin
 * renderListInExpensive(ElementWriter::yourContainer, items, animate, beforeModifier, render)
 * ```
 *
 * When animated, uses object equality (==) to match items for enter/exit transitions.
 * Works best with data classes. Note that [beforeModifier] receives the item value T.
 *
 * @param animate If true, animates entry/exit using object equality matching
 */
@PublishedApi
internal fun <T> ContainerElement.renderListExpensive(
    items: Reactive<List<T>>,
    animate: Boolean,
    beforeModifier: ViewWriter.(T) -> ElementWriter.CanAddListElementModifier = { this },
    render: ElementWriter.CanAddSizing.(T) -> Unit
) {
    if (animate) renderListExpensiveAnimating(items, beforeModifier, render)
    else renderListExpensive(items, beforeModifier, render)
}



// ---------------------------------------------------------------------------
// Internal implementations (factored bodies of the old functions)
// ---------------------------------------------------------------------------

/** ViewWriter wrapper that inserts children at a specific index instead of appending. */
private fun ContainerElement.atIndex(index: Int): ViewWriter =
    object : ViewWriter by this {
        @OverrideOnly
        override fun addChild(element: Element) {
            this@atIndex.addChild(index, element)
        }
    }

/**
 * **INTERNAL API:** Renders [items] with full rebuild on every change, no animation.
 *
 * **PERFORMANCE WARNING:** This clears and recreates all views whenever the list changes.
 * Use this only as a last resort when keyed or positional strategies don't apply.
 *
 * **Do not call directly.** Use [colOfExpensive]/[rowOfExpensive] for common cases, or [renderListInExpensive] for custom containers:
 * ```kotlin
 * renderListInExpensive(ElementWriter::yourContainer, items, beforeModifier, render)
 * ```
 *
 * Note that [beforeModifier] receives the item value T, allowing item-specific modifiers.
 */
@PublishedApi
internal fun <T> ContainerElement.renderListExpensive(
    items: Reactive<List<T>>,
    beforeModifier: ViewWriter.(T) -> ElementWriter.CanAddListElementModifier,
    render: ElementWriter.CanAddListElementModifier.(T) -> Unit
) {
    setupAsListContainer()
    reactive {
        clearChildren()
        items().forEach { item ->
            beforeModifier(item).asListItem.render(item)
        }
    }
}

/**
 * Full rebuild with animated entry/exit using object equality for item matching.
 *
 * Clears and recreates views on each change. Uses object equality (==) to match items
 * between old and new lists to animate removals. Items are identified by their value;
 * works best with data classes. Uses [shownWhen] for transitions.
 */
private fun <T> ContainerElement.renderListExpensiveAnimating(
    items: Reactive<List<T>>,
    beforeModifier: ViewWriter.(T) -> ElementWriter.CanAddListElementModifier = { this },
    render: ElementWriter.CanAddSizing.(T) -> Unit
) {
    setupAsListContainer()
    val oldEarly = ArrayList<Any>()

    data class OldViewInfo(
        var oldIndex: Int,
        val data: T,
        val view: Element,
        val shown: Signal<Boolean>
    ) {
        var livenessIter = 0
        fun show() {
            livenessIter++
            shown.value = true
        }
        fun hide() {
            val n = ++livenessIter
            shown.value = false
            afterTimeout(view.theme.transitionDuration.inWholeMilliseconds + 100) {
                if (n == livenessIter) {
                    removeChild(view)
                    oldEarly.remove(this)
                }
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    val old = oldEarly as ArrayList<OldViewInfo>
    reactive {
        val new = items()
        var oldPos = 0
        new.forEachIndexed { index, toRender ->
            var matchIndex = -1
            for (checkIndex in oldPos..<old.size) {
                if (old[checkIndex].data == toRender) {
                    old[checkIndex].show()
                    matchIndex = checkIndex
                    break
                }
            }
            if (matchIndex != -1) {
                for (i in oldPos until matchIndex) {
                    old[i].hide()
                }
                oldPos = matchIndex + 1
            } else {
                val shown = Signal(false)
                val result: Element = this@renderListExpensiveAnimating.atIndex(oldPos).produceExactlyOneView {
                    beforeModifier(toRender).asListItem.shownWhen { shown() }.render(toRender)
                }
                old.add(
                    oldPos, OldViewInfo(
                        oldIndex = index,
                        data = toRender,
                        view = result,
                        shown = shown
                    )
                )
                shown.value = true
                oldPos++
            }
        }
        old.subList(oldPos, old.size).forEach { it.hide() }
    }
}

/**
 * Positional slot reuse with view pooling and placeholder support.
 *
 * Creates view slots once and updates them in-place as items change. Efficient when
 * list order is stable and items change by position. Shows [placeholders] slots as
 * loading state. Evicts hidden slots beyond [poolCap] to limit memory usage.
 */
private fun <T> ContainerElement.renderListPositional(
    items: Reactive<List<T>>,
    placeholders: Int,
    poolCap: Int,
    beforeModifier: ViewWriter.() -> ElementWriter.CanAddListElementModifier,
    render: ElementWriter.CanAddListElementModifier.(Reactive<T>) -> Unit
) {
    setupAsListContainer()
    val currentViews = ArrayList<LateInitSignal<T>>()
    val currentView = this
    reactive(onLoad = {
        currentView.withoutAnimation {
            if (placeholders <= 0) return@reactive
            if (currentViews.size < placeholders) {
                repeat(placeholders - currentViews.size) {
                    val newProp = LateInitSignal<T>()
                    beforeModifier().asListItem.render(newProp)
                    currentViews.add(newProp)
                }
            }
            val ch = currentView.children
            for (index in 0 until placeholders) {
                ch[index].shown = true
                currentViews[index].unset()
            }
            for (index in placeholders..<currentViews.size) {
                ch[index].shown = false
            }
        }
    }) {
        val itemList = items()
        currentView.withoutAnimation {
            val oldSize = currentViews.size
            // Grow the slot pool as needed.
            if (currentViews.size < itemList.size) {
                repeat(itemList.size - currentViews.size) {
                    val newProp = LateInitSignal<T>()
                    newProp.value = itemList[currentViews.size]
                    beforeModifier().asListItem.render(newProp)
                    currentViews.add(newProp)
                }
            }

            // Update in-view slots.
            val ch = currentView.children
            for (index in 0..<min(oldSize, itemList.size)) {
                ch[index].shown = true
                currentViews[index].value = itemList[index]
            }
            // Hide excess slots.
            for (index in itemList.size..<currentViews.size) {
                ch[index].shown = false
            }

            // Evict tail slots when the hidden pool exceeds poolCap.
            val maxSlots = itemList.size + poolCap
            if (currentViews.size > maxSlots) {
                // Remove from the tail downward to keep indices stable.
                for (index in currentViews.indices.reversed()) {
                    if (currentViews.size <= maxSlots) break
                    if (index >= itemList.size) {
                        // This slot is hidden — evict it.
                        currentView.removeChild(index)
                        currentViews.removeAt(index)
                    }
                }
            }
        }
    }
}

/**
 * Keyed diffing with animated entry/exit transitions.
 *
 * Tracks items by [id] and diffs the list on each change. Matched IDs update their
 * views in-place; new IDs animate in; removed IDs animate out. Most efficient for
 * lists with stable item identities that reorder, insert, or remove items.
 *
 * Uses [shownWhen] to drive transitions, so render receives [CanAddSizing]
 * (the type returned after shownWhen consumes the CanAddShownWhen stage).
 */
private fun <T, ID> ContainerElement.renderListKeyedAnimated(
    items: Reactive<List<T>>,
    id: (T) -> ID,
    beforeModifier: ViewWriter.() -> ElementWriter.CanAddListElementModifier,
    render: ElementWriter.CanAddSizing.(Reactive<T>) -> Unit
) {
    setupAsListContainer()
    val oldEarly = ArrayList<Any>()

    data class OldViewInfo(
        var oldIndex: Int,
        val oldId: ID,
        val data: Signal<T>,
        val view: Element,
        val shown: Signal<Boolean>
    ) {
        var livenessIter = 0
        fun show() {
            livenessIter++
            shown.value = true
        }
        fun hide() {
            val n = ++livenessIter
            shown.value = false
            afterTimeout(view.theme.transitionDuration.inWholeMilliseconds + 100) {
                if (n == livenessIter) {
                    removeChild(view)
                    oldEarly.remove(this)
                }
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    val old = oldEarly as ArrayList<OldViewInfo>
    reactive {
        val new = items()
        var oldPos = 0
        new.forEachIndexed { index, toRender ->
            var matchIndex = -1
            for (checkIndex in oldPos..<old.size) {
                if (old[checkIndex].oldId == id(toRender)) {
                    matchIndex = checkIndex
                    break
                }
            }
            if (matchIndex != -1) {
                for (i in oldPos until matchIndex) {
                    old[i].hide()
                }
                oldPos = matchIndex + 1
                old[matchIndex].let {
                    it.data.value = toRender
                    it.show()
                }
            } else {
                val shown = Signal(false)
                val data = Signal(toRender)
                val result: Element = this@renderListKeyedAnimated.atIndex(oldPos).produceExactlyOneView {
                    beforeModifier().asListItem.shownWhen { shown() }.render(data)
                }
                old.add(
                    oldPos, OldViewInfo(
                        oldIndex = index,
                        oldId = id(toRender),
                        data = data,
                        view = result,
                        shown = shown
                    )
                )
                afterTimeout(1) { shown.value = true }
                oldPos++
            }
        }
        old.subList(oldPos, old.size).forEach { it.hide() }
    }
}

/**
 * Keyed diffing without animation.
 *
 * Tracks items by [id] and diffs the list on each change. Matched IDs update their
 * views in-place; new IDs insert instantly; removed IDs remove instantly. Most efficient
 * for lists with stable item identities that reorder, insert, or remove items.
 *
 * No wrapper elements are added. Render receives [CanAddListElementModifier] directly.
 */
private fun <T, ID> ContainerElement.renderListKeyedNoAnimation(
    items: Reactive<List<T>>,
    id: (T) -> ID,
    beforeModifier: ViewWriter.() -> ElementWriter.CanAddListElementModifier,
    render: ElementWriter.CanAddListElementModifier.(Reactive<T>) -> Unit
) {
    setupAsListContainer()
    val oldEarly = ArrayList<Any>()

    data class OldViewInfo(
        var oldIndex: Int,
        val oldId: ID,
        val data: Signal<T>,
        val view: Element,
        val shown: Signal<Boolean>
    ) {
        var livenessIter = 0
        fun show() {
            livenessIter++
            shown.value = true
        }
        fun hide() {
            val n = ++livenessIter
            shown.value = false
            afterTimeout(view.theme.transitionDuration.inWholeMilliseconds + 100) {
                if (n == livenessIter) {
                    removeChild(view)
                    oldEarly.remove(this)
                }
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    val old = oldEarly as ArrayList<OldViewInfo>
    reactive {
        withoutAnimation {
            val new = items()
            var oldPos = 0
            new.forEachIndexed { index, toRender ->
                var matchIndex = -1
                for (checkIndex in oldPos..<old.size) {
                    if (old[checkIndex].oldId == id(toRender)) {
                        matchIndex = checkIndex
                        break
                    }
                }
                if (matchIndex != -1) {
                    for (i in oldPos until matchIndex) {
                        old[i].hide()
                    }
                    oldPos = matchIndex + 1
                    old[matchIndex].let {
                        it.data.value = toRender
                        it.show()
                    }
                } else {
                    val shown = Signal(false)
                    val data = Signal(toRender)
                    val result: Element = this@renderListKeyedNoAnimation.atIndex(oldPos).produceExactlyOneView {
                        beforeModifier().asListItem.render(data)
                    }
                    old.add(
                        oldPos, OldViewInfo(
                            oldIndex = index,
                            oldId = id(toRender),
                            data = data,
                            view = result,
                            shown = shown
                        )
                    )
                    afterTimeout(1) { shown.value = true }
                    oldPos++
                }
            }
            old.subList(oldPos, old.size).forEach { it.hide() }
        }
    }
}


// ---------------------------------------------------------------------------
// Deprecated wrappers — kept for source compatibility, delegating to renderList
// ---------------------------------------------------------------------------

@InternalKiteUi
@Deprecated("Use renderListSlowIn instead")
fun <T> ContainerElement.forEach(
    items: Reactive<List<T>>,
    beforeListModifier: ViewWriter.(T) -> ElementWriter.CanAddListElementModifier = { this },
    render: ElementWriter.CanAddListElementModifier.(T) -> Unit
) = renderListExpensive(items, beforeListModifier, render)

@InternalKiteUi
@Deprecated("Use renderListIn instead")
fun <T> ContainerElement.forEachUpdating(
    items: Reactive<List<T>>,
    placeholdersWhileLoading: Int = 5,
    beforeListModifier: ViewWriter.() -> ElementWriter.CanAddListElementModifier = { this },
    render: ElementWriter.CanAddListElementModifier.(Reactive<T>) -> Unit
) = renderListPositional(items, placeholdersWhileLoading, 32, beforeListModifier, render)

@InternalKiteUi
@Deprecated("Use renderListIn instead")
fun <T, ID> RowOrCol.forEachById(
    items: Reactive<List<T>>,
    id: (T) -> ID,
    preHidingModifiers: ViewWriter.(ID) -> ElementWriter.CanAddListElementModifier = { this },
    render: ElementWriter.CanAddTheme.(Reactive<T>) -> Unit
) = renderListKeyedAnimated(items, id, { this }) { render(it) }

@InternalKiteUi
@Deprecated("Use renderListIn instead")
fun <T, ID> RowOrCol.forEachByIdWithoutAnimation(
    items: Reactive<List<T>>,
    id: (T) -> ID,
    beforeListModifier: ViewWriter.() -> ElementWriter.CanAddListElementModifier = { this },
    render: ElementWriter.CanAddListElementModifier.(Reactive<T>) -> Unit
) = renderListKeyedNoAnimation(items, id, beforeListModifier, render)

@InternalKiteUi
@Deprecated("Use renderListSlowIn instead")
fun <T> RowOrCol.forEachAnimated(
    items: Reactive<List<T>>,
    preHidingModifiers: ViewWriter.(T) -> ElementWriter.CanAddListElementModifier = { this },
    render: ElementWriter.CanAddTheme.(T) -> Unit
) = renderListExpensiveAnimating(items, preHidingModifiers, render)