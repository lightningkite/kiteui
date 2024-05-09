package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.dom.HTMLElement
import com.lightningkite.kiteui.reactive.Readable
import com.lightningkite.kiteui.reactive.Writable
import com.lightningkite.kiteui.views.ViewDsl
import ViewWriter

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual typealias NViewPager = HTMLElement

@ViewDsl
actual inline fun ViewWriter.viewPagerActual(crossinline setup: ViewPager.() -> Unit) {
}

actual val ViewPager.index: Writable<Int>
    get() = TODO("Not yet implemented")

actual fun <T> ViewPager.children(
    items: Readable<List<T>>,
    render: ViewWriter.(value: Readable<T>) -> Unit
) {
}