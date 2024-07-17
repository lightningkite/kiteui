package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.views.ViewWriter
import androidx.viewpager2.widget.ViewPager2
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*

actual class ViewPager actual constructor(context: RContext): RView(context) {
    override val native = ViewPager2(context.activity).apply {
        registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                page.value = position
            }
        })
    }
    val page: Property<Int> = Property(0).apply {
        addListener {
            native.setCurrentItem(value, animationsEnabled)
        }
    }

    override fun internalAddChild(index: Int, view: RView) {
        // Do nothing.  All children are virtual and managed by the native recycler view.
    }

    override fun internalClearChildren() {
        // Do nothing.  All children are virtual and managed by the native recycler view.
    }

    override fun internalRemoveChild(index: Int) {
        // Do nothing.  All children are virtual and managed by the native recycler view.
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