package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.RView
import com.lightningkite.reactive.core.*


expect class Select(context: ElementContext) : RView {
    var enabled: Boolean
    fun <T> bind(edits: MutableReactive<T>, data: Reactive<List<T>>, render: (T) -> String)
}