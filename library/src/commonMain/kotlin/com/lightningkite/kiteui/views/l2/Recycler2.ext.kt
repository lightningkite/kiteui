package com.lightningkite.kiteui.views.l2

import com.lightningkite.readable.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.l2.RecyclerViewRendererSet.Companion.MultiBuilder

fun <T, ID> Recycler2.children(items: Readable<List<T>>, id: (T)->ID, render: ViewWriter.(value: Readable<T>) -> ViewModifiable): Unit {
    rendererSet = RecyclerViewRendererSet.single(id, render)
    reactive {
        data = RecyclerViewData.fromList(items())
    }
}
fun <T, ID> Recycler2.children(items: ReactiveContext.()->List<T>, id: (T)->ID, render: ViewWriter.(value: Readable<T>) -> ViewModifiable): Unit {
    rendererSet = RecyclerViewRendererSet.single(id, render)
    reactive {
        data = RecyclerViewData.fromList(items())
    }
}
fun <T, ID> Recycler2.childrenMultipleTypes(items: Readable<List<T>>, id: (T)->ID, renderers: MultiBuilder<T, ID>.() -> Unit): Unit {
    rendererSet = RecyclerViewRendererSet.multi(id, renderers)
    reactive {
        data = RecyclerViewData.fromList(items())
    }
}
fun <T, ID> Recycler2.childrenMultipleTypes(items: ReactiveContext.()->List<T>, id: (T)->ID, renderers: MultiBuilder<T, ID>.() -> Unit): Unit {
    rendererSet = RecyclerViewRendererSet.multi(id, renderers)
    reactive {
        data = RecyclerViewData.fromList(items())
    }
}

//fun Recycler2.sample() {
//    children(Constant((1..20).toList()), id = { it }) {
//        text { ::content { it().toString() } }
//    }
//    childrenMultipleTypes(Constant((1..20).toList()), id = { it }) {
//        elementsMatching { it % 2 == 0 } renderedAs { text { ::content { it().toString() } } }
//        elementsMatching { it % 2 == 1 } renderedAs { card - text { ::content { it().toString() } } }
//    }
//}