package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.printStackTrace2
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import org.w3c.dom.*
import kotlin.math.absoluteValue
import kotlin.random.Random


actual class RecyclerView actual constructor(context: RContext) : RView(context) {
    actual var columns: Int
        get() = TODO("Not yet implemented")
        set(value) {}

    actual fun <T> children(
        items: Readable<List<T>>,
        render: ViewWriter.(value: Readable<T>) -> Unit
    ) {
    }

    actual fun scrollToIndex(
        index: Int,
        align: Align?,
        animate: Boolean
    ) {
    }

    actual val firstVisibleIndex: Readable<Int>
        get() = TODO("Not yet implemented")
    actual val lastVisibleIndex: Readable<Int>
        get() = TODO("Not yet implemented")
    actual var vertical: Boolean
        get() = TODO("Not yet implemented")
        set(value) {}
}