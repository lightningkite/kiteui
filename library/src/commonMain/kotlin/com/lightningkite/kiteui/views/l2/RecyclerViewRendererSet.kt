package com.lightningkite.kiteui.views.l2

import com.lightningkite.signal.Readable
import com.lightningkite.kiteui.views.ViewModifiable
import com.lightningkite.kiteui.views.ViewWriter

public interface RecyclerViewRendererSet<in T, out ID> {
    public fun id(item: T): ID
    public fun renderer(item: T): RecyclerViewRenderer<T>

    public object Empty : RecyclerViewRendererSet<Any?, Unit> {
        public override fun id(item: Any?): Unit = Unit
        public override fun renderer(item: Any?): RecyclerViewRenderer<Any?> = RecyclerViewRenderer.Blank
    }

    public companion object {
        public fun <T, ID> single(id: (T) -> ID, render: ViewWriter.(data: Readable<T>) -> ViewModifiable): RecyclerViewRendererSet<T, ID> =
            object : RecyclerViewRendererSet<T, ID> {
                override fun id(item: T): ID = id(item)
                val r = object : RecyclerViewRenderer<T> {
                    override fun render(
                        viewWriter: ViewWriter,
                        data: Readable<T>,
                        index: Readable<Int>
                    ): ViewModifiable = viewWriter.render(data)
                }

                override fun renderer(item: T): RecyclerViewRenderer<T> = r
            }

        public class MultiBuilder<T, ID> internal constructor(public val id: (T)->ID) {
            internal val entries = ArrayList<Pair<(T)->Boolean, RecyclerViewRenderer<T>>>()
            public fun elementsMatching(predicate: (T)->Boolean): (T)->Boolean = predicate
            public infix fun ((T)->Boolean).renderedAs(renderer: ViewWriter.(data: Readable<T>) -> ViewModifiable) {
                entries += this to object : RecyclerViewRenderer<T> {
                    override fun render(
                        viewWriter: ViewWriter,
                        data: Readable<T>,
                        index: Readable<Int>
                    ): ViewModifiable = viewWriter.renderer(data)
                }
            }
            internal fun build(): RecyclerViewRendererSet<T, ID> {
                return object : RecyclerViewRendererSet<T, ID> {
                    override fun id(item: T): ID = this@MultiBuilder.id(item)
                    override fun renderer(item: T): RecyclerViewRenderer<T> {
                        return entries.first { it.first(item) }.second
                    }
                }
            }
        }

        public fun <T, ID> multi(
            id: (T) -> ID,
            builder: MultiBuilder<T, ID>.() -> Unit
        ): RecyclerViewRendererSet<T, ID> = MultiBuilder(id).also(builder).build()
    }
}