package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import kotlinx.cinterop.ExperimentalForeignApi
import platform.UIKit.*

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual typealias NViewPager = ViewPagerScrollviewWrapper

actual fun <T> ViewPager.children(
    items: Readable<List<T>>,
    render: ViewWriter.(value: Readable<T>) -> Unit
)  {
    native.pagingRecyclerView.renderer = ItemRenderer<T>(
        create = { _, value ->
            val prop = Property(value)
            render(native.pagingRecyclerView.newViews, prop)
            native.pagingRecyclerView.newViews.rootCreated!!.also {
                it.extensionProp = prop
            }
        },
        update = { _, element, value ->
            @Suppress("UNCHECKED_CAST")
            (element.extensionProp as Property<T>).value = value
        }
    )
    calculationContext.reactiveScope {
        native.pagingRecyclerView.data = items.await().asIndexed()
    }
}

@OptIn(ExperimentalForeignApi::class)
@ViewDsl
actual inline fun ViewWriter.viewPagerActual(crossinline setup: ViewPager.() -> Unit) = element(
    ViewPagerScrollviewWrapper(newViews())
) {
    calculationContext.onRemove {
        extensionStrongRef = null
    }
    backgroundColor = UIColor.clearColor
    handleTheme(
        this, viewDraws = false,
        foregroundSkipAnimate = {
            spacing = getSpacingOverrideProperty().await() ?: it.spacing
        },
    ) {
        extensionViewWriter = newViews()
        setup(ViewPager(this))
    }
}

@OptIn(ExperimentalForeignApi::class)
actual val ViewPager.index: Writable<Int>
    get() = native.pagingRecyclerView.centerVisible
        .withWrite { native.pagingRecyclerView.jump(it, Align.Center, animationsEnabled) }
