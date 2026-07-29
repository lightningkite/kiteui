@file:OptIn(InternalKiteUi::class)
@file:Suppress("DSL_MARKER_APPLIED_TO_WRONG_TARGET")

package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.InternalKiteUi
import com.lightningkite.kiteui.exceptions.ExceptionHandler
import com.lightningkite.kiteui.views.ElementWriter
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.l2.RecyclerViewRenderer
import com.lightningkite.kiteui.views.l2.RecyclerViewRendererSet
import com.lightningkite.reactive.core.Reactive

/**
 * Renders [items] in a container using keyed ID diffing with optional animation.
 *
 * **RECOMMENDED:** This is the most efficient strategy when items have stable IDs and can
 * be reordered, inserted, or removed. Items are tracked by [id] and matched during updates.
 *
 * @param container The container factory. Use a DSL function reference like `ElementWriter::col`
 *                  or `ElementWriter::row`. If using a lambda, it MUST call the passed setup
 *                  lambda, e.g., `{ setup -> col { setup() } }`.
 * @param id Function to extract unique identifier from each item
 * @param animate If true (default), animates entry/exit transitions
 * @see colOf for a col-specific convenience wrapper
 * @see rowOf for a row-specific convenience wrapper
 */
public inline fun <T, ID, C : ContainerElement> ElementWriter.renderListIn(
    container: ElementWriter.(C.() -> Unit) -> C,
    items: Reactive<List<T>>,
    noinline id: (T) -> ID,
    animate: Boolean = true,
    noinline beforeModifier: ViewWriter.() -> ElementWriter.CanAddListElementModifier = { this },
    noinline render: ElementWriter.CanAddSizing.(Reactive<T>) -> Unit
): C {
    var setupCalled = false
    val result = container {
        setupCalled = true
        renderListKeyed(items, id, animate, beforeModifier, render)
    }
    if (!setupCalled) context.handleException(
        Exception("renderListIn: container lambda did not call the setup lambda. Use a DSL function reference like ElementWriter::col or ElementWriter::row, or ensure your custom lambda invokes the passed setup function, e.g., { setup -> col { setup() } }"),
        ExceptionHandler.Metadata(
            source = result,
            process = null,
            foregroundProcess = null,
            context = mapOf(
                "container type" to result::class.toString(),
                "items" to (items.state.getOrNull()?.toString() ?: "NotReady"),
                "animate" to animate.toString(),
            )
        )
    )
    return result
}

/**
 * Renders [items] in a container using positional slot reuse with optional placeholders.
 *
 * Views are created once and reused by position. Efficient for lists where items change
 * but order/count is relatively stable. Shows placeholder items during loading.
 *
 * @param container The container factory. Use a DSL function reference like `ElementWriter::col`
 *                  or `ElementWriter::row`. If using a lambda, it MUST call the passed setup
 *                  lambda, e.g., `{ setup -> col { setup() } }`.
 * @param placeholdersWhileLoading Number of placeholder items to show while loading (default 5)
 * @param poolCap Maximum hidden views to retain beyond list size (default 32)
 * @see colOf for a col-specific convenience wrapper
 * @see rowOf for a row-specific convenience wrapper
 */
public inline fun <T, C : ContainerElement> ElementWriter.renderListIn(
    container: ElementWriter.(C.() -> Unit) -> C,
    items: Reactive<List<T>>,
    placeholdersWhileLoading: Int = 5,
    poolCap: Int = 32,
    noinline beforeModifier: ViewWriter.() -> ElementWriter.CanAddListElementModifier = { this },
    noinline render: ElementWriter.CanAddListElementModifier.(Reactive<T>) -> Unit
): C {
    var setupCalled = false
    val result = container {
        setupCalled = true
        renderList(items, placeholdersWhileLoading, poolCap, beforeModifier, render)
    }
    if (!setupCalled) context.handleException(
        Exception("renderListIn: container lambda did not call the setup lambda. Use a DSL function reference like ElementWriter::col or ElementWriter::row, or ensure your custom lambda invokes the passed setup function, e.g., { setup -> col { setup() } }"),
        ExceptionHandler.Metadata(
            source = result,
            process = null,
            foregroundProcess = null,
            context = mapOf(
                "container type" to result::class.toString(),
                "items" to (items.state.getOrNull()?.toString() ?: "NotReady"),
                "placeholders" to placeholdersWhileLoading.toString(),
                "poolCap" to poolCap.toString()
            )
        )
    )
    return result
}

/**
 * Renders [items] in a container using keyed ID diffing with support for multiple renderer types.
 *
 * **HETEROGENEOUS LISTS:** Use this when different items in your list need different renderers
 * (e.g., a feed with posts, ads, and comments each having different layouts). The [rendererSet]
 * determines which renderer to use for each item based on its type/properties.
 *
 * **Performance modes:**
 * - `animate = true` (default): Uses keyed diffing with animated transitions. Best for user-facing
 *   lists where items can be reordered, inserted, or removed.
 * - `animate = false`: Uses positional slot reuse. Maximum efficiency but no support for reordering.
 *   Best for server-driven lists where the entire list is replaced.
 *
 * **Example:**
 * ```kotlin
 * val feedRendererSet = RecyclerViewRendererSet(
 *     id = { it.id },
 *     renderer = { item ->
 *         when (item.type) {
 *             "post" -> postRenderer
 *             "ad" -> adRenderer
 *             "comment" -> commentRenderer
 *         }
 *     }
 * )
 * renderListIn(ElementWriter::col, feedItems, feedRendererSet)
 * ```
 *
 * @param container The container factory. Use a DSL function reference like `ElementWriter::col`
 *                  or `ElementWriter::row`. If using a lambda, it MUST call the passed setup
 *                  lambda, e.g., `{ setup -> col { setup() } }`.
 * @param rendererSet Provides ID and renderer for each item
 * @param animate If true (default), uses keyed diffing with animations. If false, uses positional reuse.
 * @param poolCap Maximum hidden views to retain (positional mode only, default 32)
 * @param placeholdersWhileLoading Renderers to show while data loads
 * @see colOf for a col-specific convenience wrapper
 * @see rowOf for a row-specific convenience wrapper
 */
@ViewDsl
public inline fun <T, ID : Any, C : ContainerElement> ElementWriter.renderListIn(
    container: ElementWriter.(C.() -> Unit) -> C,
    items: Reactive<List<T>>,
    rendererSet: RecyclerViewRendererSet<T, ID>,
    animate: Boolean = true,
    poolCap: Int = 32,
    placeholdersWhileLoading: List<RecyclerViewRenderer<T>> = emptyList(),
): C {
    var setupCalled = false
    val result = container {
        setupCalled = true
        renderHeterogeneousList(items, rendererSet, animate, poolCap, placeholdersWhileLoading)
    }
    if (!setupCalled) context.handleException(
        Exception("renderListIn: container lambda did not call the setup lambda. Use a DSL function reference like ElementWriter::col or ElementWriter::row, or ensure your custom lambda invokes the passed setup function, e.g., { setup -> col { setup() } }"),
        ExceptionHandler.Metadata(
            source = result,
            process = null,
            foregroundProcess = null,
            context = mapOf(
                "container type" to result::class.toString(),
                "items" to (items.state.getOrNull()?.toString() ?: "NotReady"),
                "placeholders" to placeholdersWhileLoading.toString(),
                "poolCap" to poolCap.toString(),
                "animate" to animate.toString()
            )
        )
    )
    return result
}

/**
 * Renders [items] in a container with full rebuild on every change, optionally animated.
 *
 * **LAST RESORT:** This clears and recreates all views whenever the list changes.
 * Only use when keyed or positional strategies don't apply. When animated, uses
 * object equality (==) to match items for transitions (works best with data classes).
 *
 * @param container The container factory. Use a DSL function reference like `ElementWriter::col`
 *                  or `ElementWriter::row`. If using a lambda, it MUST call the passed setup
 *                  lambda, e.g., `{ setup -> col { setup() } }`.
 * @param animate If true, animates entry/exit using object equality matching
 * @see colOfExpensive for a col-specific convenience wrapper
 * @see rowOfExpensive for a row-specific convenience wrapper
 */
public inline fun <T, C : ContainerElement> ElementWriter.renderListInExpensive(
    container: ElementWriter.(C.() -> Unit) -> C,
    items: Reactive<List<T>>,
    animate: Boolean,
    noinline beforeModifier: ViewWriter.(T) -> ElementWriter.CanAddListElementModifier = { this },
    noinline render: ElementWriter.CanAddSizing.(T) -> Unit
): C {
    var setupCalled = false
    val result = container {
        setupCalled = true
        renderListExpensive(items, animate, beforeModifier, render)
    }
    if (!setupCalled) context.handleException(
        Exception("renderListInExpensive: container lambda did not call the setup lambda. Use a DSL function reference like ElementWriter::col or ElementWriter::row, or ensure your custom lambda invokes the passed setup function, e.g., { setup -> col { setup() } }"),
        ExceptionHandler.Metadata(
            source = result,
            process = null,
            foregroundProcess = null,
            context = mapOf(
                "container type" to result::class.toString(),
                "items" to (items.state.getOrNull()?.toString() ?: "NotReady"),
                "animate" to animate.toString()
            )
        )
    )
    return result
}

/**
 * Renders [items] in a container with full rebuild on every change, no animation.
 *
 * **LAST RESORT:** This clears and recreates all views whenever the list changes.
 * Only use when keyed or positional strategies don't apply.
 *
 * @param container The container factory. Use a DSL function reference like `ElementWriter::col`
 *                  or `ElementWriter::row`. If using a lambda, it MUST call the passed setup
 *                  lambda, e.g., `{ setup -> col { setup() } }`.
 * @see colOfExpensive for a col-specific convenience wrapper
 * @see rowOfExpensive for a row-specific convenience wrapper
 */
public inline fun <T, C : ContainerElement> ElementWriter.renderListInExpensive(
    container: ElementWriter.(C.() -> Unit) -> C,
    items: Reactive<List<T>>,
    noinline beforeModifier: ViewWriter.(T) -> ElementWriter.CanAddListElementModifier = { this },
    noinline render: ElementWriter.CanAddListElementModifier.(T) -> Unit
): C {
    var setupCalled = false
    val result = container {
        setupCalled = true
        renderListExpensive(items, beforeModifier, render)
    }
    if (!setupCalled) context.handleException(
        Exception("renderListInExpensive: container lambda did not call the setup lambda. Use a DSL function reference like ElementWriter::col or ElementWriter::row, or ensure your custom lambda invokes the passed setup function, e.g., { setup -> col { setup() } }"),
        ExceptionHandler.Metadata(
            source = result,
            process = null,
            foregroundProcess = null,
            context = mapOf(
                "container type" to result::class.toString(),
                "items" to (items.state.getOrNull()?.toString() ?: "NotReady")
            )
        )
    )
    return result
}



/**
 * Creates a column ([col]) and renders [items] using keyed ID diffing with optional animation.
 *
 * **RECOMMENDED:** This is the most efficient strategy when items have stable IDs and can
 * be reordered, inserted, or removed. Items are tracked by [id] and matched during updates.
 *
 * **Example:**
 * ```kotlin
 * colOf(users, id = { it.id }) { user ->
 *     text { ::content { user().name } }
 * }
 * ```
 *
 * @param id Function to extract unique identifier from each item
 * @param animate If true (default), animates entry/exit transitions
 * @return The created RowOrCol container
 */
public fun <T, ID> ElementWriter.colOf(
    items: Reactive<List<T>>,
    id: (T) -> ID,
    animate: Boolean = true,
    preHidingModifiers: ViewWriter.() -> ElementWriter.CanAddListElementModifier = { this },
    render: ElementWriter.CanAddSizing.(Reactive<T>) -> Unit
): RowOrCol = col {
    // Optimized: directly calls renderListKeyed. For custom containers, use:
    // renderListIn(ElementWriter::yourContainer, items, id, animate, preHidingModifiers, render)
    renderListKeyed(items, id, animate, preHidingModifiers, render)
}

/**
 * Creates a column ([col]) and renders [items] using positional slot reuse with placeholders.
 *
 * Views are created once and reused by position. Efficient for lists where items change
 * but order/count is relatively stable. Shows placeholder items during loading.
 *
 * **Example:**
 * ```kotlin
 * colOf(posts, placeholdersWhileLoading = 3) { post ->
 *     text { ::content { post().title } }
 * }
 * ```
 *
 * @param placeholdersWhileLoading Number of placeholder items to show while loading (default 5)
 * @param poolCap Maximum hidden views to retain beyond list size (default 32)
 * @return The created RowOrCol container
 */
public fun <T> ElementWriter.colOf(
    items: Reactive<List<T>>,
    placeholdersWhileLoading: Int = 5,
    poolCap: Int = 32,
    beforeModifier: ViewWriter.() -> ElementWriter.CanAddListElementModifier = { this },
    render: ElementWriter.CanAddListElementModifier.(Reactive<T>) -> Unit
): RowOrCol = col {
    // Optimized: directly calls renderList. For custom containers, use:
    // renderListIn(ElementWriter::yourContainer, items, placeholdersWhileLoading, poolCap, beforeModifier, render)
    renderList(items, placeholdersWhileLoading, poolCap, beforeModifier, render)
}

/**
 * Creates a column ([col]) and renders [items] with full rebuild on every change, optionally animated.
 *
 * **LAST RESORT:** This clears and recreates all views whenever the list changes.
 * Only use when keyed or positional strategies don't apply. When animated, uses
 * object equality (==) to match items for transitions (works best with data classes).
 *
 * @param animate If true, animates entry/exit using object equality matching
 * @return The created RowOrCol container
 */
public fun <T> ElementWriter.colOfExpensive(
    items: Reactive<List<T>>,
    animate: Boolean,
    beforeModifier: ViewWriter.(T) -> ElementWriter.CanAddListElementModifier = { this },
    render: ElementWriter.CanAddSizing.(T) -> Unit
): RowOrCol = col {
    // Optimized: directly calls renderListExpensive. For custom containers, use:
    // renderListInExpensive(ElementWriter::yourContainer, items, animate, beforeModifier, render)
    renderListExpensive(items, animate, beforeModifier, render)
}

/**
 * Creates a column ([col]) and renders [items] with full rebuild on every change, no animation.
 *
 * **LAST RESORT:** This clears and recreates all views whenever the list changes.
 * Only use when keyed or positional strategies don't apply.
 *
 * @return The created RowOrCol container
 */
public fun <T> ElementWriter.colOfExpensive(
    items: Reactive<List<T>>,
    beforeModifier: ViewWriter.(T) -> ElementWriter.CanAddListElementModifier = { this },
    render: ElementWriter.CanAddListElementModifier.(T) -> Unit
): RowOrCol = col {
    // Optimized: directly calls renderListExpensive. For custom containers, use:
    // renderListInExpensive(ElementWriter::yourContainer, items, beforeModifier, render)
    renderListExpensive(items, beforeModifier, render)
}

/**
 * Creates a column ([col]) and renders [items] with support for multiple renderer types.
 *
 * **HETEROGENEOUS LISTS:** Use this when different items need different renderers
 * (e.g., a feed with posts, ads, and comments). The [rendererSet] determines which
 * renderer to use for each item.
 *
 * **Example:**
 * ```kotlin
 * val feedRendererSet = RecyclerViewRendererSet(
 *     id = { it.id },
 *     renderer = { item ->
 *         when (item) {
 *             is Post -> postRenderer
 *             is Ad -> adRenderer
 *             is Comment -> commentRenderer
 *         }
 *     }
 * )
 * colOf(feedItems, feedRendererSet)
 * ```
 *
 * @param rendererSet Provides ID and renderer for each item
 * @param animate If true (default), uses keyed diffing with animations. If false, uses positional reuse.
 * @param poolCap Maximum hidden views to retain (positional mode only, default 32)
 * @param placeholdersWhileLoading Renderers to show while data loads
 * @return The created RowOrCol container
 */
@ViewDsl
public fun <T, ID : Any> ElementWriter.colOf(
    items: Reactive<List<T>>,
    rendererSet: RecyclerViewRendererSet<T, ID>,
    animate: Boolean = true,
    poolCap: Int = 32,
    placeholdersWhileLoading: List<RecyclerViewRenderer<T>> = emptyList()
): RowOrCol = col {
    // Optimized: directly calls renderHeterogeneousList. For custom containers, use:
    // renderListIn(ElementWriter::yourContainer, items, rendererSet, animate, poolCap, placeholdersWhileLoading)
    renderHeterogeneousList(items, rendererSet, animate, poolCap, placeholdersWhileLoading)
}


/**
 * Creates a row ([row]) and renders [items] using keyed ID diffing with optional animation.
 *
 * **RECOMMENDED:** This is the most efficient strategy when items have stable IDs and can
 * be reordered, inserted, or removed. Items are tracked by [id] and matched during updates.
 *
 * **Example:**
 * ```kotlin
 * rowOf(tags, id = { it.id }) { tag ->
 *     text { ::content { tag().name } }
 * }
 * ```
 *
 * @param id Function to extract unique identifier from each item
 * @param animate If true (default), animates entry/exit transitions
 * @return The created RowOrCol container
 */
public fun <T, ID> ElementWriter.rowOf(
    items: Reactive<List<T>>,
    id: (T) -> ID,
    animate: Boolean = true,
    preHidingModifiers: ViewWriter.() -> ElementWriter.CanAddListElementModifier = { this },
    render: ElementWriter.CanAddSizing.(Reactive<T>) -> Unit
): RowOrCol = row {
    // Optimized: directly calls renderListKeyed. For custom containers, use:
    // renderListIn(ElementWriter::yourContainer, items, id, animate, preHidingModifiers, render)
    renderListKeyed(items, id, animate, preHidingModifiers, render)
}

/**
 * Creates a row ([row]) and renders [items] using positional slot reuse with placeholders.
 *
 * Views are created once and reused by position. Efficient for lists where items change
 * but order/count is relatively stable. Shows placeholder items during loading.
 *
 * **Example:**
 * ```kotlin
 * rowOf(icons, placeholdersWhileLoading = 3) { icon ->
 *     image { ::source { icon().url } }
 * }
 * ```
 *
 * @param placeholdersWhileLoading Number of placeholder items to show while loading (default 5)
 * @param poolCap Maximum hidden views to retain beyond list size (default 32)
 * @return The created RowOrCol container
 */
public fun <T> ElementWriter.rowOf(
    items: Reactive<List<T>>,
    placeholdersWhileLoading: Int = 5,
    poolCap: Int = 32,
    beforeModifier: ViewWriter.() -> ElementWriter.CanAddListElementModifier = { this },
    render: ElementWriter.CanAddListElementModifier.(Reactive<T>) -> Unit
): RowOrCol = row {
    // Optimized: directly calls renderList. For custom containers, use:
    // renderListIn(ElementWriter::yourContainer, items, placeholdersWhileLoading, poolCap, beforeModifier, render)
    renderList(items, placeholdersWhileLoading, poolCap, beforeModifier, render)
}

/**
 * Creates a row ([row]) and renders [items] with full rebuild on every change, optionally animated.
 *
 * **LAST RESORT:** This clears and recreates all views whenever the list changes.
 * Only use when keyed or positional strategies don't apply. When animated, uses
 * object equality (==) to match items for transitions (works best with data classes).
 *
 * @param animate If true, animates entry/exit using object equality matching
 * @return The created RowOrCol container
 */
public fun <T> ElementWriter.rowOfExpensive(
    items: Reactive<List<T>>,
    animate: Boolean,
    beforeModifier: ViewWriter.(T) -> ElementWriter.CanAddListElementModifier = { this },
    render: ElementWriter.CanAddSizing.(T) -> Unit
): RowOrCol = row {
    // Optimized: directly calls renderListExpensive. For custom containers, use:
    // renderListInExpensive(ElementWriter::yourContainer, items, animate, beforeModifier, render)
    renderListExpensive(items, animate, beforeModifier, render)
}

/**
 * Creates a row ([row]) and renders [items] with full rebuild on every change, no animation.
 *
 * **LAST RESORT:** This clears and recreates all views whenever the list changes.
 * Only use when keyed or positional strategies don't apply.
 *
 * @return The created RowOrCol container
 */
public fun <T> ElementWriter.rowOfExpensive(
    items: Reactive<List<T>>,
    beforeModifier: ViewWriter.(T) -> ElementWriter.CanAddListElementModifier = { this },
    render: ElementWriter.CanAddListElementModifier.(T) -> Unit
): RowOrCol = row {
    // Optimized: directly calls renderListExpensive. For custom containers, use:
    // renderListInExpensive(ElementWriter::yourContainer, items, beforeModifier, render)
    renderListExpensive(items, beforeModifier, render)
}

/**
 * Creates a row ([row]) and renders [items] with support for multiple renderer types.
 *
 * **HETEROGENEOUS LISTS:** Use this when different items need different renderers
 * (e.g., a horizontal feed with different card types). The [rendererSet] determines
 * which renderer to use for each item.
 *
 * **Example:**
 * ```kotlin
 * val cardRendererSet = RecyclerViewRendererSet(
 *     id = { it.id },
 *     renderer = { card ->
 *         when (card.type) {
 *             "image" -> imageCardRenderer
 *             "video" -> videoCardRenderer
 *             "text" -> textCardRenderer
 *         }
 *     }
 * )
 * rowOf(cards, cardRendererSet)
 * ```
 *
 * @param rendererSet Provides ID and renderer for each item
 * @param animate If true (default), uses keyed diffing with animations. If false, uses positional reuse.
 * @param poolCap Maximum hidden views to retain (positional mode only, default 32)
 * @param placeholdersWhileLoading Renderers to show while data loads
 * @return The created RowOrCol container
 */
@ViewDsl
public fun <T, ID : Any> ElementWriter.rowOf(
    items: Reactive<List<T>>,
    rendererSet: RecyclerViewRendererSet<T, ID>,
    animate: Boolean = true,
    poolCap: Int = 32,
    placeholdersWhileLoading: List<RecyclerViewRenderer<T>> = emptyList()
): RowOrCol = row {
    // Optimized: directly calls renderHeterogeneousList. For custom containers, use:
    // renderListIn(ElementWriter::yourContainer, items, rendererSet, animate, poolCap, placeholdersWhileLoading)
    renderHeterogeneousList(items, rendererSet, animate, poolCap, placeholdersWhileLoading)
}