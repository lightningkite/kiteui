package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*


//val LocalDateField.content: Writable<LocalDate?>


expect class RecyclerView(context: RContext) : RView {
    var columns: Int
    fun <T> children(items: Readable<List<T>>, render: ViewWriter.(value: Readable<T>) -> Unit): Unit
    // TODO: Diffing
//    fun <T, ID> children(items: Readable<List<T>>, identity: (T)->ID, render: ViewWriter.(value: Readable<T>) -> Unit): Unit
    fun scrollToIndex(index: Int, align: Align? = null, animate: Boolean = true)
    val firstVisibleIndex: Readable<Int>
    val lastVisibleIndex: Readable<Int>
    var vertical: Boolean
//    var reverse: Boolean
}