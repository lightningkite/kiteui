package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.views.ViewWriter
import androidx.viewpager2.widget.ViewPager2
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*

actual class ViewPager actual constructor(context: RContext): RView(context) {
    val page = Property(0)
    override val native = ViewPager2(context.activity).apply {
        registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                page.value = position
            }
        })
    }


    actual val index: Writable<Int> get() = page

    actual fun <T> children(
        items: Readable<List<T>>,
        render: ViewWriter.(value: Readable<T>) -> Unit
    ) {
        native.adapter = object : RecyclerView.ObservableRVA<T>(
            recyclerView = this,
            calculationContext = this,
            layoutManager = null,
            placeholderCount = 5,
            determineType = { 0 },
            makeView = { _, obs -> render(obs) }) {
            init {
                reactiveScope(onLoad = {
                    loading = true
                    notifyDataSetChanged()
                }) {
                    val new = items.await().toList()
                    loading = false
                    lastPublished = new
                    notifyDataSetChanged()
                }
            }
        }
    }
}