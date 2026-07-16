package com.lightningkite.kiteui.views.l2

import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.l2.RecyclerViewRendererSet.Companion.MultiBuilder
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*

/**
 * Configures the recycler to display [items] using keyed ID diffing with virtualization.
 * Equivalent to the old [children] function.
 */
fun <T, ID> Recycler2.renderList(items: Reactive<List<T>>, id: (T) -> ID, render: ViewWriter.(value: Reactive<T>) -> Unit): Unit {
    rendererSet = RecyclerViewRendererSet.single(id, render)
    reactive {
        data = RecyclerViewData.fromList(items())
    }
}

/**
 * Configures the recycler to display [items] using keyed ID diffing with virtualization.
 * @deprecated Use [renderList] instead.
 */
@Deprecated(
    "Use renderList instead",
    ReplaceWith("renderList(items, id, render)")
)
fun <T, ID> Recycler2.children(items: Reactive<List<T>>, id: (T)->ID, render: ViewWriter.(value: Reactive<T>) -> Unit): Unit =
    renderList(items, id, render)

/**
 * Configures the recycler to display [items] using keyed ID diffing with virtualization.
 * @deprecated Use [renderList] instead.
 */
@Deprecated(
    "Use renderList instead",
    ReplaceWith("renderList(items, id, render)")
)
fun <T, ID> Recycler2.children(items: ReactiveContext.()->List<T>, id: (T)->ID, render: ViewWriter.(value: Reactive<T>) -> Unit): Unit {
    rendererSet = RecyclerViewRendererSet.single(id, render)
    reactive {
        data = RecyclerViewData.fromList(items())
    }
}

/**
 * Configures the recycler to display [items] with multiple renderer types.
 * @deprecated Use [renderListMultipleTypes] instead.
 */
@Deprecated(
    "Use renderListMultipleTypes instead",
    ReplaceWith("renderListMultipleTypes(items, id, renderers)")
)
fun <T, ID> Recycler2.childrenMultipleTypes(items: Reactive<List<T>>, id: (T)->ID, renderers: MultiBuilder<T, ID>.() -> Unit): Unit =
    renderListMultipleTypes(items, id, renderers)

/**
 * Configures the recycler to display [items] with multiple renderer types.
 * @deprecated Use [renderListMultipleTypes] instead.
 */
@Deprecated(
    "Use renderListMultipleTypes instead",
    ReplaceWith("renderListMultipleTypes(items, id, renderers)")
)
fun <T, ID> Recycler2.childrenMultipleTypes(items: ReactiveContext.()->List<T>, id: (T)->ID, renderers: MultiBuilder<T, ID>.() -> Unit): Unit =
    renderListMultipleTypes(items, id, renderers)

/**
 * Configures the recycler to display [items] with multiple renderer types and virtualization.
 */
fun <T, ID> Recycler2.renderListMultipleTypes(items: Reactive<List<T>>, id: (T)->ID, renderers: MultiBuilder<T, ID>.() -> Unit): Unit {
    rendererSet = RecyclerViewRendererSet.multi(id, renderers)
    reactive {
        data = RecyclerViewData.fromList(items())
    }
}

/**
 * Configures the recycler to display [items] with multiple renderer types and virtualization.
 */
fun <T, ID> Recycler2.renderListMultipleTypes(items: ReactiveContext.()->List<T>, id: (T)->ID, renderers: MultiBuilder<T, ID>.() -> Unit): Unit {
    rendererSet = RecyclerViewRendererSet.multi(id, renderers)
    reactive {
        data = RecyclerViewData.fromList(items())
    }
}
