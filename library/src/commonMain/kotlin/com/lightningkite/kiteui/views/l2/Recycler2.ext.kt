package com.lightningkite.kiteui.views.l2

import com.lightningkite.signal.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.l2.RecyclerViewRendererSet.Companion.MultiBuilder

public fun <T, ID> Recycler2.children(items: Readable<List<T>>, id: (T)->ID, render: ViewWriter.(value: Readable<T>) -> ViewModifiable): Unit {
    rendererSet = RecyclerViewRendererSet.single(id, render)
    reactive {
        data = RecyclerViewData.fromList(items())
    }
}
public fun <T, ID> Recycler2.children(items: ReactiveContext.()->List<T>, id: (T)->ID, render: ViewWriter.(value: Readable<T>) -> ViewModifiable): Unit {
    rendererSet = RecyclerViewRendererSet.single(id, render)
    reactive {
        data = RecyclerViewData.fromList(items())
    }
}
public fun <T, ID> Recycler2.childrenMultipleTypes(items: Readable<List<T>>, id: (T)->ID, renderers: MultiBuilder<T, ID>.() -> Unit): Unit {
    rendererSet = RecyclerViewRendererSet.multi(id, renderers)
    reactive {
        data = RecyclerViewData.fromList(items())
    }
}
public fun <T, ID> Recycler2.childrenMultipleTypes(items: ReactiveContext.()->List<T>, id: (T)->ID, renderers: MultiBuilder<T, ID>.() -> Unit): Unit {
    rendererSet = RecyclerViewRendererSet.multi(id, renderers)
    reactive {
        data = RecyclerViewData.fromList(items())
    }
}