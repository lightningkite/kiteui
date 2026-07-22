package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.InternalKiteUi
import com.lightningkite.kiteui.OverrideOnly
import com.lightningkite.kiteui.afterTimeout
import com.lightningkite.kiteui.views.direct.RowOrCol
import com.lightningkite.kiteui.views.direct.asListItem
import com.lightningkite.kiteui.views.direct.frame
import com.lightningkite.kiteui.views.direct.setupAsListContainer
import com.lightningkite.kiteui.views.direct.shownWhen
import com.lightningkite.kiteui.views.l2.RecyclerViewRenderer
import com.lightningkite.kiteui.views.l2.RecyclerViewRendererSet
import com.lightningkite.reactive.context.reactive
import com.lightningkite.reactive.core.Constant
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
 * **INTERNAL API:** Renders [items] using keyed ID diffing with support for multiple renderer types per item.
 *
 * Similar to [renderListKeyed] but allows different items to use different renderers. Each item's
 * renderer and ID are provided by [rendererSet]. When animated, items matched by ID are shown/hidden
 * with transitions. When not animated, uses positional slot reuse for maximum efficiency.
 *
 * **Do not call directly.** Use the public API instead:
 * ```kotlin
 * renderListIn(ElementWriter::yourContainer, items, rendererSet, animate, poolCap, placeholders)
 * ```
 *
 * @param items Reactive list of items to render
 * @param rendererSet Provides ID and renderer for each item
 * @param animate If true (default), uses keyed diffing with animated transitions.
 *                If false, uses positional slot reuse (no reorder support).
 * @param poolCap Maximum hidden views to retain beyond list size (positional mode only)
 * @param placeHoldersWhileLoading Renderers to show as placeholders while loading
 */
@PublishedApi
internal fun <T, ID : Any> ContainerElement.renderHeterogeneousList(
    items: Reactive<List<T>>,
    rendererSet: RecyclerViewRendererSet<T, ID>,
    animate: Boolean = true,
    poolCap: Int = 32,
    placeHoldersWhileLoading: List<RecyclerViewRenderer<T>> = emptyList(),
) {
    if (animate) {
        renderHeterogeneousListAnimated(items, rendererSet, placeHoldersWhileLoading)
    }
    else {
        renderHeterogeneousListPositional(items, rendererSet, poolCap, placeHoldersWhileLoading)
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

private data class Cell<ID, DATA>(
    var oldIndex: Int,
    val id: ID,
    val data: DATA,
    val cells: ArrayList<Cell<ID, DATA>>,
    val container: ContainerElement,
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
        container.afterTimeout(view.theme.transitionDuration.inWholeMilliseconds + 100) {
            if (n == livenessIter) {
                container.removeChild(view)
                cells.remove(this)
            }
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

    val old = ArrayList<Cell<Nothing?, T>>()

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
                    oldPos, Cell(
                        oldIndex = index,
                        id = null,
                        data = toRender,
                        container = this@renderListExpensiveAnimating,
                        cells = old,
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

    val old = ArrayList<Cell<ID, Signal<T>>>()

    reactive {
        val new = items()
        var oldPos = 0
        new.forEachIndexed { index, toRender ->
            var matchIndex = -1
            for (checkIndex in oldPos..<old.size) {
                if (old[checkIndex].id == id(toRender)) {
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
                    oldPos, Cell(
                        oldIndex = index,
                        id = id(toRender),
                        data = data,
                        view = result,
                        shown = shown,
                        container = this@renderListKeyedAnimated,
                        cells = old
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

    val old = ArrayList<Cell<ID, Signal<T>>>()

    reactive {
        withoutAnimation {
            val new = items()
            var oldPos = 0
            new.forEachIndexed { index, toRender ->
                var matchIndex = -1
                for (checkIndex in oldPos..<old.size) {
                    if (old[checkIndex].id == id(toRender)) {
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
                        oldPos, Cell(
                            oldIndex = index,
                            id = id(toRender),
                            data = data,
                            view = result,
                            shown = shown,
                            container = this@renderListKeyedNoAnimation,
                            cells = old
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

/**
 * Renders [items] using keyed ID diffing with multiple renderers and animated transitions.
 *
 * Similar to [renderListKeyedAnimated] but supports different renderer types per item.
 * Uses a three-tier matching strategy:
 * 1. **Exact ID match**: Reuses cell with matching ID (preserves state during reorders)
 * 2. **Renderer change**: If renderer changed for matched ID, hides old cell and creates new
 * 3. **Create new**: Creates new cell when no existing cell matches
 *
 * Hidden cells are removed after their exit animation completes. Loading placeholders
 * are shown before data arrives and automatically hidden when real data is available.
 * Uses [shownWhen] for animated entry/exit transitions.
 *
 * @param items Reactive list of items to render
 * @param rendererSet Provides ID and renderer for each item
 * @param loadingRenderers Renderers to show as placeholders while [items] is loading
 */
private fun <T, ID : Any> ContainerElement.renderHeterogeneousListAnimated(
    items: Reactive<List<T>>,
    rendererSet: RecyclerViewRendererSet<T, ID>,
    loadingRenderers: List<RecyclerViewRenderer<T>> = emptyList()
) {
    setupAsListContainer()

    data class RendererData(
        val renderer: RecyclerViewRenderer<T>,
        val indexSignal: Signal<Int>,
        val data: LateInitSignal<T>
    )

    // Use nullable ID to allow placeholder cells with null IDs
    val cells = ArrayList<Cell<ID?, RendererData>>()

    /** Creates a cell with shownWhen animation support */
    fun createCell(
        index: Int,
        itemId: ID?,
        item: T?,
        renderer: RecyclerViewRenderer<T>,
        insertAt: Int
    ): Cell<ID?, RendererData> {
        val data = LateInitSignal<T>()
        if (item != null) data.value = item
        val indexSignal = Signal(index)
        val shown = Signal(false)
        val view = this@renderHeterogeneousListAnimated.atIndex(insertAt).produceExactlyOneView {
            // Wrap renderer output in a frame with shownWhen for animation
            asListItem.shownWhen { shown() }.frame {
                renderer.render(this, data, indexSignal)
            }
        }
        return Cell(
            oldIndex = index,
            id = itemId,
            data = RendererData(renderer, indexSignal, data),
            view = view,
            shown = shown,
            container = this@renderHeterogeneousListAnimated,
            cells = cells
        )
    }

    reactive(onLoad = {
        // Show loading placeholders before data arrives
        if (loadingRenderers.isEmpty()) return@reactive
        this@renderHeterogeneousListAnimated.withoutAnimation {
            loadingRenderers.forEachIndexed { idx, renderer ->
                val cell = createCell(idx, null, null, renderer, idx)
                cells.add(cell)
                // Show placeholder immediately (no animation on initial load)
                cell.shown.value = true
            }
        }
    }) {
        val newList = items()
        var cellPos = 0

        // Process each item in the new list
        newList.forEachIndexed { newIndex, item ->
            val itemId = rendererSet.id(item)
            val itemRenderer = rendererSet.renderer(item)

            // Search for existing cell with matching ID (only non-null IDs match)
            var matchIndex = -1
            for (checkIndex in cellPos until cells.size) {
                val cellId = cells[checkIndex].id
                if (cellId != null && cellId == itemId) {
                    matchIndex = checkIndex
                    break
                }
            }

            if (matchIndex != -1) {
                // Found exact ID match - hide cells before it (including any placeholders)
                for (i in cellPos until matchIndex) {
                    cells[i].hide()
                }
                cellPos = matchIndex + 1

                val cell = cells[matchIndex]

                // Check if renderer changed
                if (cell.data.renderer != itemRenderer) {
                    // Renderer changed - hide old cell and create new at same position
                    cell.hide()
                    // Insert new view at the matched position (old view animates out in place)
                    val newCell = createCell(newIndex, itemId, item, itemRenderer, matchIndex)
                    cells[matchIndex] = newCell
                    afterTimeout(1) { newCell.show() }
                } else {
                    // Same renderer - update data in place
                    cell.data.data.value = item
                    cell.data.indexSignal.value = newIndex
                    cell.oldIndex = newIndex
                    cell.show()
                }
            } else {
                // No ID match - create new cell at correct position
                val newCell = createCell(newIndex, itemId, item, itemRenderer, cellPos)
                cells.add(cellPos, newCell)
                afterTimeout(1) { newCell.show() }
                cellPos++
            }
        }

        // Hide remaining cells that weren't matched (including any remaining placeholders)
        for (i in cellPos until cells.size) {
            cells[i].hide()
        }
    }
}

private class PositionalCell<T, ID : Any>(
    container: ContainerElement,
    private val rendererSet: RecyclerViewRendererSet<T, ID>,
    val data: LateInitSignal<T>,
    id: ID?,
    renderer: RecyclerViewRenderer<T>,
    index: Int
) {
    constructor(
        container: ContainerElement,
        set: RecyclerViewRendererSet<T, ID>,
        data: T,
        index: Int
    ) : this(
        container,
        set,
        LateInitSignal<T>().apply { value = data },
        set.id(data),
        set.renderer(data),
        index
    )

    var id: ID? = id
        private set

    var renderer: RecyclerViewRenderer<T> = renderer
        private set

    val index: Constant<Int> = Constant(index)

    val view = container.asListItem.frame {
        renderer.render(this, this@PositionalCell.data, this@PositionalCell.index)
    }

    private var watchingLoad = false
    fun watchBackgroundProcess(readable: Reactive<*>) {
        if (watchingLoad) return
        watchingLoad = true
        view.underlyingNativeElement.watchBackgroundProcess(readable)
    }

    fun hide() {
        view.withoutAnimation {
            view.shown = false
        }
        data.unset()
    }

    fun show() {
        view.shown = true
    }

    /**
     * Updates this cell with new data, recreating the view only if the renderer changes.
     */
    fun newData(value: T) {
        view.shown = true
        val newId = rendererSet.id(value)
        val newRenderer = rendererSet.renderer(value)

        if (newRenderer == renderer) {
            // Same renderer - just update data and ID
            data.value = value
            id = newId
        } else {
            // Renderer changed - must recreate view
            renderer = newRenderer
            id = newId
            view.withoutAnimation {
                view.clearChildren()
                data.value = value
                newRenderer.render(view, this@PositionalCell.data, this@PositionalCell.index)
            }
        }
    }
}

/**
 * Renders [items] using positional slot reuse with multiple renderers.
 *
 * **Most efficient variant** - reuses cells by position rather than by ID.
 * Each position in the list has a dedicated cell that updates its data in place.
 * Views are only recreated when the renderer type changes for a position.
 *
 * **Trade-offs:**
 * - Pro: Maximum efficiency - no DOM reordering, minimal view recreation
 * - Pro: Simple pooling - excess cells are hidden and reused when list grows
 * - Con: No support for item reordering animations (items at position N always use cell N)
 * - Con: State in cells (e.g., scroll position) doesn't follow items when they move
 *
 * **Best for:**
 * - Lists where items change frequently but order is stable
 * - Server-driven lists where the whole list is replaced
 * - Situations where you need maximum rendering performance
 *
 * @param items Reactive list of items to render
 * @param rendererSet Provides ID and renderer for each item
 * @param poolCap Maximum hidden cells to retain beyond current list size (default 32)
 * @param loadingRenderers Renderers to show as placeholders while [items] is loading
 */
private fun <T, ID : Any> ContainerElement.renderHeterogeneousListPositional(
    items: Reactive<List<T>>,
    rendererSet: RecyclerViewRendererSet<T, ID>,
    poolCap: Int = 32,
    loadingRenderers: List<RecyclerViewRenderer<T>> = emptyList()
) {
    setupAsListContainer()

    val cells = ArrayList<PositionalCell<T, ID>>()

    reactive(onLoad = {
        // Show loading placeholders before data arrives
        if (loadingRenderers.isEmpty()) return@reactive
        this@renderHeterogeneousListPositional.withoutAnimation {
            loadingRenderers.forEachIndexed { idx, renderer ->
                val cell = PositionalCell(
                    this@renderHeterogeneousListPositional,
                    rendererSet,
                    LateInitSignal(),
                    null,
                    renderer,
                    idx
                )
                cells.add(cell)
                cell.show()
            }
        }
    }) {
        val list = items()

        this@renderHeterogeneousListPositional.withoutAnimation {
            // Update existing cells or create new ones as needed
            for ((idx, item) in list.withIndex()) {
                val existingCell = cells.getOrNull(idx)
                if (existingCell != null) {
                    existingCell.newData(item)
                } else {
                    cells.add(PositionalCell(this@renderHeterogeneousListPositional, rendererSet, item, idx))
                }
            }

            // Hide excess cells beyond current list size
            for (i in list.size until cells.size) {
                cells[i].hide()
            }

            // Evict cells beyond poolCap (iterate in reverse to maintain stable indices)
            val maxCells = list.size + poolCap
            if (cells.size > maxCells) {
                for (i in cells.lastIndex downTo maxCells) {
                    removeChild(i)
                    cells.removeAt(i)
                }
            }
        }

        // Watch background processes for visible cells
        for (i in 0 until min(list.size, cells.size)) {
            cells[i].watchBackgroundProcess(this)
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