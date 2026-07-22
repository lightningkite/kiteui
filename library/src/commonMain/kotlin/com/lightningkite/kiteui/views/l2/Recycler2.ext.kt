package com.lightningkite.kiteui.views.l2

import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.l2.RecyclerViewRendererSet.Companion.MultiBuilder
import com.lightningkite.reactive.context.ReactiveContext
import com.lightningkite.reactive.context.reactive
import com.lightningkite.reactive.core.Reactive

/**
 * Configures the recycler to display [items] using keyed ID diffing with virtualization.
 */
fun <T, ID> Recycler2.children(items: Reactive<List<T>>, id: (T)->ID, render: ViewWriter.(value: Reactive<T>) -> Unit): Unit {
    rendererSet = RecyclerViewRendererSet.single(id, render)
    reactive {
        data = RecyclerViewData.fromList(items())
    }
}

/**
 * Configures the recycler to display [items] using keyed ID diffing with virtualization.
 */
fun <T, ID> Recycler2.children(items: ReactiveContext.()->List<T>, id: (T)->ID, render: ViewWriter.(value: Reactive<T>) -> Unit): Unit {
    rendererSet = RecyclerViewRendererSet.single(id, render)
    reactive {
        data = RecyclerViewData.fromList(items())
    }
}

/**
 * Configures the recycler to display [items] with multiple renderer types.
 */
fun <T, ID> Recycler2.childrenMultipleTypes(items: Reactive<List<T>>, id: (T)->ID, renderers: MultiBuilder<T, ID>.() -> Unit): Unit {
    rendererSet = RecyclerViewRendererSet.multi(id, renderers)
    reactive {
        data = RecyclerViewData.fromList(items())
    }
}

/**
 * Configures the recycler to display [items] with multiple renderer types.
 */
fun <T, ID> Recycler2.childrenMultipleTypes(items: ReactiveContext.()->List<T>, id: (T)->ID, renderers: MultiBuilder<T, ID>.() -> Unit): Unit {
    rendererSet = RecyclerViewRendererSet.multi(id, renderers)
    reactive {
        data = RecyclerViewData.fromList(items())
    }
}