package com.lightningkite.kiteui.views.direct

import android.content.Context
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.viewpager2.widget.ViewPager2
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.ViewDsl
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.calculationContext

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual class NViewPager(context: Context) : SlightlyModifiedFrameLayout(context) {
    lateinit var viewWriter: ViewWriter
    val pager = ViewPager2(context)
    val page = Property(0)
    init {
        addView(pager, LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        pager.registerOnPageChangeCallback(object: ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                page.value = position
            }
        })
        page.addListener {
            if(page.value != pager.currentItem) {
                pager.currentItem = page.value
            }
        }
    }
}

@ViewDsl
actual inline fun ViewWriter.viewPagerActual(crossinline setup: ViewPager.() -> Unit) {
    element(NViewPager(context))  {
        viewWriter = newViews()
        handleTheme(this, viewDraws = false) {
            setup(ViewPager(this))
        }
    }
}

actual val ViewPager.index: Writable<Int> get() = native.page

actual fun <T> ViewPager.children(
    items: Readable<List<T>>,
    render: ViewWriter.(value: Readable<T>) -> Unit
) {
    native.pager.adapter = object : ObservableRVA<T>(
        viewWriter = native.viewWriter,
        calculationContext = native.calculationContext,
        layoutManager = null,
        placeholderCount = 5,
        determineType = { 0 },
        makeView = { _, obs -> render(obs) }) {
        init {
            native.calculationContext.reactiveScope(onLoad = {
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