package com.lightningkite.kiteui.views.l2

import com.lightningkite.kiteui.ExperimentalKiteUi
import com.lightningkite.kiteui.Log
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.models.DropTargetDelegate
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.LinearLayoutElement
import com.lightningkite.kiteui.views.direct.col
import com.lightningkite.kiteui.views.direct.separator
import com.lightningkite.reactive.context.invoke
import com.lightningkite.reactive.context.reactive
import com.lightningkite.reactive.core.Reactive
import com.lightningkite.reactive.core.Signal
import com.lightningkite.reactive.core.remember
import com.lightningkite.reactive.lensing.lens
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch


class DragDropReordering(
    private val scope: CoroutineScope,
    val mimeType: String = "application/kiteui-index",
    val reorder: suspend (Move) -> Unit
) {
    object HalfGap : Semantic("halfgap") {
        override fun default(theme: Theme): ThemeAndBack = theme.withoutBack(
            cascading = false,
            gap = theme.gap / 2,
        )
    }

    fun encode(index: Int) = DragData("Source Index", mimeType, index.toString())
    fun decode(data: DragData) = data[mimeType]?.toInt()

    data class Move(val start: Int, val end: Int) {
        fun <T> reorder(list: List<T>) =
            if (start == end) list
            else list
                .toMutableList()
                .apply { add(end, removeAt(start)) }
                .toList()
    }

    val willMove = Signal<Move?>(null)

    inner class Delegate(val index: Reactive<Int>) : DropTargetDelegate {
        override fun enter(event: DragEvent): Boolean =
            decode(event.data)
                ?.let { source ->
                    index.state.handle(
                        success = { willMove.value = Move(source, it); true },
                        exception = { Log.warn("forEachReorderable: exception reading drop target index"); false },
                        notReady = { Log.warn("forEachReorderable: drop target index not ready"); false }
                    )
                }
                ?: false

        override fun end(event: DragEvent): Boolean {
            willMove.value = null
            return true
        }

        override fun drop(event: DragEvent): Boolean =
            decode(event.data)
                ?.let { sourceIdx ->
                    scope.launch {
                        reorder(Move(sourceIdx, index()))
                    }
                    willMove.value = null
                    true
                }
                ?: false
    }
}

fun <T> ContainerElement.forEachReorderable(
    items: Reactive<List<T>>,
    reorder: suspend (DragDropReordering.Move) -> Unit,
    separator: ViewWriter.(Reactive<T>) -> Unit = { separator() },
    dataTransform: (DragData) -> DragData = { it },
    render: ViewWriter.(Reactive<T>) -> Unit
) {
    val handler = DragDropReordering(this, reorder = reorder)

    (this@forEachReorderable as? LinearLayoutElement)?.gap = 0.px

    renderList(
        remember { items().mapIndexed { idx, it -> IndexedValue(idx, it) } },
        placeholders = 5,
        beforeModifier = { this }
    ) { indexed ->
        val item = indexed.lens { it.value }
        val idx = indexed.lens { it.index }
        col {
            @OptIn(ExperimentalKiteUi::class)
            themeBase = NativeElementCommonCode.GetBaseTheme.fromParentNonCascading

            themeChoice += DragDropReordering.HalfGap
            dropTargetDelegate = handler.Delegate(idx)

            beforeSetup {
                ::visible shown@{
                    val move = handler.willMove() ?: return@shown false
                    val i = idx()
                    move.end == i && move.start >= i
                }
            }.separator(item)

            beforeSetup {
                ::dragData { dataTransform(handler.encode(idx())) }
            }.render(item)

            beforeSetup {
                ::visible shown@{
                    val move = handler.willMove() ?: return@shown false
                    val i = idx()
                    move.end == i && move.start < i
                }
            }.separator(item)
        }
    }
}

class RecyclerReorderable<T, ID>(
    val wraps: RecyclerViewRendererSet<T, ID>,
    val view: Recycler2,
    val separator: ViewWriter.(Reactive<T>) -> Unit = { separator() },
    reorder: suspend (DragDropReordering.Move) -> Unit
) : RecyclerViewRendererSet<T, ID> {
    val handler = DragDropReordering(view, reorder = reorder)

    override fun id(item: T): ID = wraps.id(item)

    init {
        view.gap = 0.px
    }

    inner class ReorderWrapper(
        val renderer: RecyclerViewRenderer<T>
    ) : RecyclerViewRenderer<T> {
        @OptIn(ExperimentalKiteUi::class)
        override fun render(viewWriter: ViewWriter, data: Reactive<T>, index: Reactive<Int>): Unit = with(viewWriter) {
            col {
                themeBase = NativeElementCommonCode.GetBaseTheme.fromParentNonCascading

                themeChoice += DragDropReordering.HalfGap

                dropTargetDelegate = handler.Delegate(index)

                beforeSetup {
                    ::visible shown@{
                        val move = handler.willMove() ?: return@shown false
                        val i = index()
                        move.end == i && move.start >= i
                    }
                }.separator(data)

                renderer.render(beforeSetup {
                    ::dragData { handler.encode(index()) }
                }, data, index)

                beforeSetup {
                    ::shown shown@{
                        val move = handler.willMove() ?: return@shown false
                        val i = index()
                        move.end == i && move.start < i
                    }
                }.separator(data)
            }
        }
    }

    override fun renderer(item: T): RecyclerViewRenderer<T> = ReorderWrapper(wraps.renderer(item))
}

fun <T, ID> Recycler2.childrenReorderable(
    items: Reactive<List<T>>,
    id: (T) -> ID,
    reorder: suspend (DragDropReordering.Move) -> Unit,
    separator: ViewWriter.(Reactive<T>) -> Unit = { separator() },
    render: ViewWriter.(Reactive<T>) -> Unit
) {
    rendererSet = RecyclerReorderable(
        RecyclerViewRendererSet.single(id, render),
        this,
        separator,
        reorder
    )

    reactive {
        data = RecyclerViewData.fromList(items())
    }
}