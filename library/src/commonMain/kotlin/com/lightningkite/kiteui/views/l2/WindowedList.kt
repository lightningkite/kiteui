package com.lightningkite.kiteui.views.l2

import com.lightningkite.kiteui.ExperimentalKiteUi
import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.views.Element
import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.ElementWriter
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.l2.RecyclerViewRendererSet.Companion.MultiBuilder
import com.lightningkite.kiteui.views.write
import com.lightningkite.reactive.core.Reactive

/**
 * A vertical list that renders only the items near the viewport.
 *
 * A list resting at its end stays there as items arrive or grow, until it is scrolled away.
 *
 * On the web the cells are laid out by the browser, which is far cheaper there than [Recycler2]
 * positioning each one.  Other platforms use a [Recycler2].
 */
@ExperimentalKiteUi
public interface WindowedList : Element {
    public val firstIndex: Reactive<Int>
    public val lastIndex: Reactive<Int>
    public fun <T, ID> children(items: Reactive<List<T>>, id: (T) -> ID, render: ViewWriter.(value: Reactive<T>) -> Unit)

    /** Like [children], for lists whose items don't all look alike.  A cell is only reused for items of its own kind. */
    public fun <T, ID> childrenMultipleTypes(items: Reactive<List<T>>, id: (T) -> ID, renderers: MultiBuilder<T, ID>.() -> Unit)

    public fun scrollToIndex(index: Int, align: Align, animate: Boolean = true)
}

@ExperimentalKiteUi
internal class Recycler2WindowedList(private val recycler: Recycler2) : WindowedList, Element by recycler {
    override val firstIndex: Reactive<Int> get() = recycler.firstIndex
    override val lastIndex: Reactive<Int> get() = recycler.lastIndex
    override fun <T, ID> children(items: Reactive<List<T>>, id: (T) -> ID, render: ViewWriter.(value: Reactive<T>) -> Unit) {
        recycler.children(items, id, render)
    }
    override fun <T, ID> childrenMultipleTypes(items: Reactive<List<T>>, id: (T) -> ID, renderers: MultiBuilder<T, ID>.() -> Unit) {
        recycler.childrenMultipleTypes(items, id, renderers)
    }
    override fun scrollToIndex(index: Int, align: Align, animate: Boolean): Unit = recycler.scrollToIndex(index, align, animate)
}

@ExperimentalKiteUi
internal expect fun platformWindowedList(context: ElementContext): WindowedList

@ExperimentalKiteUi
public fun ElementWriter.windowedList(setup: WindowedList.() -> Unit = {}): WindowedList =
    write(platformWindowedList(context), setup)
