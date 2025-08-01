package com.lightningkite.kiteui.views.l2

import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.l2.RecyclerViewRendererSet.Companion.MultiBuilder
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*

fun <T, ID> Recycler2.children(items: Reactive<List<T>>, id: (T)->ID, render: ViewWriter.(value: Reactive<T>) -> ViewModifiable): Unit {
    rendererSet = RecyclerViewRendererSet.single(id, render)
    reactive {
        data = RecyclerViewData.fromList(items())
    }
}
fun <T, ID> Recycler2.children(items: ReactiveContext.()->List<T>, id: (T)->ID, render: ViewWriter.(value: Reactive<T>) -> ViewModifiable): Unit {
    rendererSet = RecyclerViewRendererSet.single(id, render)
    reactive {
        data = RecyclerViewData.fromList(items())
    }
}
fun <T, ID> Recycler2.childrenMultipleTypes(items: Reactive<List<T>>, id: (T)->ID, renderers: MultiBuilder<T, ID>.() -> Unit): Unit {
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