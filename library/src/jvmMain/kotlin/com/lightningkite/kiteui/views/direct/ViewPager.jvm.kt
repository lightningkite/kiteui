package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.Icon
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*

actual class ViewPager actual constructor(context: RContext): RView(context) {
    actual val index: Writable<Int>
        get() = TODO("Not yet implemented")

    actual fun <T> children(
        items: Readable<List<T>>,
        render: ViewWriter.(value: Readable<T>) -> Unit
    ) {
    }
}