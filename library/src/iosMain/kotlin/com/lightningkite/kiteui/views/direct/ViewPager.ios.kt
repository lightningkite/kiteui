package com.lightningkite.kiteui.views.direct


import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import kotlinx.cinterop.ExperimentalForeignApi
import platform.UIKit.*

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual typealias NViewPager = NRecyclerView
actual class ViewPager actual constructor(context: RContext): RView(context) {
    override val native = NRecyclerView(false, object: ViewWriter() {

    }).apply {
        backgroundColor = UIColor.clearColor
        forceCentering = true
        elementsMatchSize = true
    }

    @OptIn(ExperimentalForeignApi::class)
    actual val index: Writable<Int>
        get() = native.centerVisible
            .withWrite { native.jump(it, Align.Center, animationsEnabled) }

    actual fun <T> children(
        items: Readable<List<T>>,
        render: ViewWriter.(value: Readable<T>) -> Unit
    )  {
        native.renderer = ItemRenderer<T>(
            create = { _, value ->
                val prop = Property(value)
                render(native.newViews, prop)
                native.newViews.rootCreated!!.also {
                    it.extensionProp = prop
                }
            },
            update = { _, element, value ->
                @Suppress("UNCHECKED_CAST")
                (element.extensionProp as Property<T>).value = value
            }
        )
        calculationContext.reactiveScope {
            native.data = items.await().asIndexed()
        }
    }

}
