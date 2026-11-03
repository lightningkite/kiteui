package com.lightningkite.kiteui.views.l2

import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*

public interface RecyclerViewRendererSet<in T, out ID> {
    public fun id(item: T): ID
    public fun renderer(item: T): RecyclerViewRenderer<T>

    public object Empty : RecyclerViewRendererSet<Any?, Unit> {
        override fun id(item: Any?): Unit = Unit
        override fun renderer(item: Any?): RecyclerViewRenderer<Any?> = RecyclerViewRenderer.Blank
    }

    public companion object {
        public fun <T, ID> single(id: (T) -> ID, render: ViewWriter.(data: Reactive<T>) -> Unit): RecyclerViewRendererSet<T, ID> =
            object : RecyclerViewRendererSet<T, ID> {
                override fun id(item: T): ID = id(item)
                val r = object : RecyclerViewRenderer<T> {
                    override fun render(
                        viewWriter: ViewWriter,
                        data: Reactive<T>,
                        index: Reactive<Int>
                    ): Unit = viewWriter.render(data)
                }

                override fun renderer(item: T): RecyclerViewRenderer<T> = r
            }

        public class MultiBuilder<T, ID> internal constructor(public val id: (T)->ID) {
            internal val entries = ArrayList<Pair<(T)->Boolean, RecyclerViewRenderer<T>>>()
            public fun elementsMatching(predicate: (T)->Boolean): (T)->Boolean = predicate
            public infix fun ((T)->Boolean).renderedAs(renderer: ViewWriter.(data: Reactive<T>) -> Unit) {
                entries += this to object : RecyclerViewRenderer<T> {
                    override fun render(
                        viewWriter: ViewWriter,
                        data: Reactive<T>,
                        index: Reactive<Int>
                    ): Unit = viewWriter.renderer(data)
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