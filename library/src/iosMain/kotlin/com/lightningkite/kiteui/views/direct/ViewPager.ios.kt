package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import kotlinx.cinterop.ExperimentalForeignApi
import platform.UIKit.*

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual typealias NViewPager = NRecyclerView

actual fun <T> ViewPager.children(
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

@OptIn(ExperimentalForeignApi::class)
@ViewDsl
actual inline fun ViewWriter.viewPagerActual(crossinline setup: ViewPager.() -> Unit) = element(
    NRecyclerView(false, newViews())
) {
    calculationContext.onRemove {
        extensionStrongRef = null
    }
    backgroundColor = UIColor.clearColor
    handleTheme(
        this, viewDraws = false,
        foreground = {
            spacing = spacingOverride?.value ?: it.spacing
        },
    ) {
        forceCentering = true
        elementsMatchSize = true
        extensionViewWriter = newViews()
        setup(ViewPager(this))
    }
}

@OptIn(ExperimentalForeignApi::class)
actual val ViewPager.index: Writable<Int>
    get() = native.centerVisible
        .withWrite { native.jump(it, Align.Center, animationsEnabled) }
