package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.InternalKiteUi
import com.lightningkite.kiteui.afterTimeout
import com.lightningkite.kiteui.views.direct.RowOrCol
import com.lightningkite.kiteui.views.direct.asListItem
import com.lightningkite.kiteui.views.direct.atIndex
import com.lightningkite.kiteui.views.direct.setupAsListContainer
import com.lightningkite.kiteui.views.direct.shownWhen
import com.lightningkite.reactive.context.reactive
import com.lightningkite.reactive.core.LateInitSignal
import com.lightningkite.reactive.core.Reactive
import com.lightningkite.reactive.core.Signal
import com.lightningkite.reactive.extensions.value
import kotlin.math.min


/** Maximum number of pooled (hidden) views retained beyond the current list size. */
const val DEFAULT_LIST_POOL_CAP = 32

// ---------------------------------------------------------------------------
// Primary API
// ---------------------------------------------------------------------------

/**
 * Renders [items] using keyed ID diffing with optional animation.
 *
 * Each item is tracked by [id]. When the list changes, items are matched by ID:
 * matched views have their [Reactive] updated in-place; missing IDs trigger
 * animated insertion; IDs no longer in the list trigger animated removal.
 * Set [animate] to false to suppress enter/exit animations (identical to the
 * old forEachByIdWithoutAnimation behavior).
 *
 * The animated path uses [shownWhen] to drive entry/exit transitions, which
 * places a wrapper container around each item.  Because [shownWhen] consumes
 * the [CanAddListElementModifier] stage and returns [CanAddSizing], the render
 * lambda receives [CanAddTheme] as its receiver when animate=true.  When
 * animate=false, no wrapper is inserted and the render lambda receives
 * [CanAddListElementModifier] directly.
 */
@InternalKiteUi
fun <T, ID> ContainerElement.renderList(
    items: Reactive<List<T>>,
    id: (T) -> ID,
    animate: Boolean = true,
    beforeModifier: ViewWriter.() -> ElementWriter.CanAddListElementModifier = { this },
    render: ElementWriter.CanAddTheme.(Reactive<T>) -> Unit
) {
    if (animate) {
        renderListKeyedAnimated(items, id, beforeModifier, render)
    } else {
        renderListKeyedNoAnimation(items, id, beforeModifier, render)
    }
}

/**
 * Renders [items] using positional slot reuse (unkeyed).
 *
 * When [placeholders] is 0 and no slot reuse is needed, the list is fully
 * rebuilt on every change (simplest path, identical to old forEach behavior).
 *
 * When [placeholders] > 0 or positional reuse is desired, slots are created
 * on demand and hidden rather than destroyed on shrink. A pool cap is enforced:
 * when the number of hidden views exceeds [poolCap] beyond the current list
 * size, excess tail views are removed and destroyed to bound memory growth.
 *
 * The [render] lambda receives a [Reactive] that updates in-place when the
 * list changes (no view rebuild on item update).
 */
@InternalKiteUi
fun <T> ContainerElement.renderList(
    items: Reactive<List<T>>,
    animate: Boolean = false,
    placeholders: Int = 0,
    poolCap: Int = DEFAULT_LIST_POOL_CAP,
    beforeModifier: ViewWriter.() -> ElementWriter.CanAddListElementModifier = { this },
    render: ElementWriter.CanAddListElementModifier.(Reactive<T>) -> Unit
) {
    if (placeholders == 0 && poolCap == DEFAULT_LIST_POOL_CAP) {
        // Simple path: full rebuild on every change.  Wrap each item in a Signal so
        // callers that invoke it() inside render get the value; since clearChildren()
        // is called before each rebuild, the signal is a one-shot value holder.
        renderListSimple(items, beforeModifier) { item ->
            val signal = Signal(item)
            render(signal)
        }
    } else {
        renderListPositional(items, placeholders, poolCap, beforeModifier, render)
    }
}

// ---------------------------------------------------------------------------
// Internal implementations (factored bodies of the old functions)
// ---------------------------------------------------------------------------

/**
 * Full-rebuild path (old forEach body).  Receives raw T from the reactive rebuild.
 * No Signal wrapper needed — the whole list is rebuilt on every change.
 */
@InternalKiteUi
private fun <T> ContainerElement.renderListSimple(
    items: Reactive<List<T>>,
    beforeModifier: ViewWriter.() -> ElementWriter.CanAddListElementModifier,
    render: ElementWriter.CanAddListElementModifier.(T) -> Unit
) {
    setupAsListContainer()
    reactive {
        clearChildren()
        items().forEach { item ->
            beforeModifier().asListItem.render(item)
        }
    }
}

/** Positional slot-reuse path with poolCap eviction (old forEachUpdating body + eviction). */
@InternalKiteUi
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

/** Keyed diffing with animated entry/exit (old forEachById body).
 *
 * Uses [shownWhen] to drive CSS transitions, so render receives [CanAddTheme]
 * (the type returned after shownWhen consumes the CanAddShownWhen stage).
 */
@InternalKiteUi
private fun <T, ID> ContainerElement.renderListKeyedAnimated(
    items: Reactive<List<T>>,
    id: (T) -> ID,
    beforeModifier: ViewWriter.() -> ElementWriter.CanAddListElementModifier,
    render: ElementWriter.CanAddTheme.(Reactive<T>) -> Unit
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

/** Keyed diffing without animation (old forEachByIdWithoutAnimation body). */
@InternalKiteUi
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

/**
 * Full rebuild on every list change.
 * @deprecated Use [renderList] instead: `renderList(items) { render(it()) }`
 */
@InternalKiteUi
@Deprecated(
    "Use renderList instead",
    ReplaceWith(
        "renderList(items, beforeListModifier = beforeListModifier) { render(it()) }",
        "com.lightningkite.kiteui.views.renderList"
    )
)
fun <T> ContainerElement.forEach(
    items: Reactive<List<T>>,
    beforeListModifier: ViewWriter.() -> ElementWriter.CanAddListElementModifier = { this },
    render: ElementWriter.CanAddListElementModifier.(T) -> Unit
) = renderListSimple(items, beforeListModifier, render)

/**
 * Positional slot reuse with placeholder loading state.
 * @deprecated Use [renderList] with `placeholders` parameter instead:
 * `renderList(items, placeholders = placeholdersWhileLoading) { render(it) }`
 */
@InternalKiteUi
@Deprecated(
    "Use renderList instead",
    ReplaceWith(
        "renderList(items, placeholders = placeholdersWhileLoading, beforeModifier = beforeListModifier, render = render)",
        "com.lightningkite.kiteui.views.renderList"
    )
)
fun <T> ContainerElement.forEachUpdating(
    items: Reactive<List<T>>,
    placeholdersWhileLoading: Int = 5,
    beforeListModifier: ViewWriter.() -> ElementWriter.CanAddListElementModifier = { this },
    render: ElementWriter.CanAddListElementModifier.(Reactive<T>) -> Unit
) = renderListPositional(items, placeholdersWhileLoading, DEFAULT_LIST_POOL_CAP, beforeListModifier, render)

/**
 * Keyed diffing with animated entry/exit.
 * @deprecated Use [renderList] with `id` parameter instead:
 * `renderList(items, id = id) { render(it) }`
 */
@InternalKiteUi
@Deprecated(
    "Use renderList instead",
    ReplaceWith(
        "renderList(items, id = id, beforeModifier = { this }, render = render)",
        "com.lightningkite.kiteui.views.renderList"
    )
)
fun <T, ID> RowOrCol.forEachById(
    items: Reactive<List<T>>,
    id: (T) -> ID,
    preHidingModifiers: ViewWriter.(ID) -> ElementWriter.CanAddListElementModifier = { this },
    render: ElementWriter.CanAddTheme.(Reactive<T>) -> Unit
) = renderListKeyedAnimated(items, id, { this }) { render(it) }

/**
 * Keyed diffing without animation.
 * @deprecated Use [renderList] with `id` and `animate = false` instead:
 * `renderList(items, id = id, animate = false) { render(it) }`
 */
@InternalKiteUi
@Deprecated(
    "Use renderList instead",
    ReplaceWith(
        "renderList(items, id = id, animate = false, beforeModifier = { this }, render = render)",
        "com.lightningkite.kiteui.views.renderList"
    )
)
fun <T, ID> RowOrCol.forEachByIdWithoutAnimation(
    items: Reactive<List<T>>,
    id: (T) -> ID,
    beforeListModifier: ViewWriter.() -> ElementWriter.CanAddListElementModifier = { this },
    render: ElementWriter.CanAddListElementModifier.(Reactive<T>) -> Unit
) = renderListKeyedNoAnimation(items, id, beforeListModifier, render)

/**
 * Keyed by object equality with animated entry/exit.
 * Items are identified by their value via `==`; works best with data classes.
 *
 * @deprecated Use [renderList] with `id = { it }` instead:
 * `renderList(items, id = { it }) { render(it()) }`
 */
@InternalKiteUi
@Deprecated(
    "Use renderList instead",
    ReplaceWith(
        "renderList(items, id = { it }, beforeModifier = { preHidingModifiers(it()) }) { render(it()) }",
        "com.lightningkite.kiteui.views.renderList"
    )
)
fun <T> RowOrCol.forEachAnimated(
    items: Reactive<List<T>>,
    preHidingModifiers: ViewWriter.(T) -> ElementWriter.CanAddListElementModifier = { this },
    render: ElementWriter.CanAddTheme.(T) -> Unit
) {
    // The preHidingModifiers parameter takes T but renderList's beforeModifier does not,
    // so we keep the original body here to preserve full behavior.
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
                val result: Element = this@forEachAnimated.atIndex(oldPos).produceExactlyOneView {
                    preHidingModifiers(toRender).asListItem.shownWhen { shown() }.render(toRender)
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
