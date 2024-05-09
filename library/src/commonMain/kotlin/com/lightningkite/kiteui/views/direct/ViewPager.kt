package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.reactive.Readable
import com.lightningkite.kiteui.reactive.Writable
import com.lightningkite.kiteui.views.RContext
import ViewWriter
import com.lightningkite.kiteui.views.RView


expect class ViewPager(context: RContext) : RView {

    val index: Writable<Int>
    fun <T> children(items: Readable<List<T>>, render: ViewWriter.(value: Readable<T>) -> Unit): Unit
}