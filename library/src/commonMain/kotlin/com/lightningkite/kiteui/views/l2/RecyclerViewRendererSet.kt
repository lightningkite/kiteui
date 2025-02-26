package com.lightningkite.kiteui.views.l2

import com.lightningkite.readable.Readable
import com.lightningkite.kiteui.views.ViewModifiable
import com.lightningkite.kiteui.views.ViewWriter

interface RecyclerViewRendererSet<in T, out ID> {
    fun id(item: T): ID
    fun renderer(item: T): RecyclerViewRenderer<T>

    object Empty : RecyclerViewRendererSet<Any?, Unit> {
        override fun id(item: Any?): Unit = Unit
        override fun renderer(item: Any?): RecyclerViewRenderer<Any?> = RecyclerViewRenderer.Blank
    }

    companion object {
        fun <T, ID> single(id: (T) -> ID, render: ViewWriter.(data: Readable<T>) -> ViewModifiable) =
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

        class MultiBuilder<T, ID> internal constructor(val id: (T)->ID) {
            internal val entries = ArrayList<Pair<(T)->Boolean, RecyclerViewRenderer<T>>>()
            fun elementsMatching(predicate: (T)->Boolean): (T)->Boolean = predicate
            infix fun ((T)->Boolean).renderedAs(renderer: ViewWriter.(data: Readable<T>) -> ViewModifiable) {
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

        fun <T, ID> multi(
            id: (T) -> ID,
            builder: MultiBuilder<T, ID>.() -> Unit
        ) = MultiBuilder(id).also(builder).build()
    }
}