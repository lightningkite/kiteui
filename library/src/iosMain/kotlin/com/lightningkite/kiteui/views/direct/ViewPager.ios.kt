package com.lightningkite.kiteui.views.direct


import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import kotlinx.cinterop.ExperimentalForeignApi

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual typealias NViewPager = NRecyclerView
actual class ViewPager actual constructor(context: RContext): RView(context) {
    override val native = NRecyclerView().apply {
        vertical = false
        forceCentering = true
        elementsMatchSize = true
    }
    val newViews = NewViewWriter(context)


    override fun internalAddChild(index: Int, view: RView) {
        // Do nothing.  All children are virtual and managed by the native recycler view.
    }

    override fun internalClearChildren() {
        // Do nothing.  All children are virtual and managed by the native recycler view.
    }

    override fun internalRemoveChild(index: Int) {
        // Do nothing.  All children are virtual and managed by the native recycler view.
    }

    @OptIn(ExperimentalForeignApi::class)
    actual val index: Writable<Int>
        get() = native.centerVisible
            .withWrite { native.jump(it, Align.Center, animationsEnabled) }


    actual fun <T> children(
        items: Readable<List<T>>,
        render: ViewWriter.(value: Readable<T>) -> Unit
    ): Unit {
        native.renderer = ItemRenderer<T>(
            create = { parent, value ->
                val prop = Property(value)
                render(newViews, prop)
                val new = newViews.newView!!
                addChild(new)
                new.tag = prop
                new.native
            },
            update = { parent, element, value ->
                @Suppress("UNCHECKED_CAST")
                (children.find { it.native === element }?.tag as? Property<T>)?.value = value
            },
            shutdown = { parent, element ->
                removeChild(children.indexOfFirst { it.native === element })
            }
        )
        reactiveScope {
            native.data = items.await().asIndexed()
        }
    }

}
