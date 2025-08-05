package com.lightningkite.kiteui.views.l2

import com.lightningkite.kiteui.models.DragData
import com.lightningkite.kiteui.models.DragEvent
import com.lightningkite.kiteui.models.div
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.DropTargetDelegate
import com.lightningkite.kiteui.views.RView
import com.lightningkite.kiteui.views.ViewModifiable
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.direct.col
import com.lightningkite.kiteui.views.direct.separator
import com.lightningkite.kiteui.views.forEachUpdating
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch


public class DragDropReordering(
    private val scope: CoroutineScope,
    public val mimeType: String = "application/kiteui-index",
    public val reorder: suspend (Move) -> Unit
) {
    public fun encode(index: Int): DragData = DragData("Source Index", mimeType, index.toString())
    public fun decode(data: DragData): Int? = data[mimeType]?.toInt()

    public data class Move(val start: Int, val end: Int) {
        public fun <T> reorder(list: List<T>): List<T> =
            if (start == end) list
            else list
                .toMutableList()
                .apply { add(end, removeAt(start)) }
                .toList()
    }

    public val willMove: Signal<Move?> = Signal<Move?>(null)

    public inner class Delegate(public val index: Reactive<Int>) : DropTargetDelegate {
        public override fun enter(event: DragEvent): Boolean =
            decode(event.data)
                ?.let { source ->
                    index.state.handle(
                        success = { willMove.value = Move(source, it); true },
                        exception = { println("Exception"); false },
                        notReady = { println("Not Ready"); false }
                    )
                }
                ?: false

        public override fun end(event: DragEvent): Boolean {
            willMove.value = null
            return true
        }

        public override fun drop(event: DragEvent): Boolean =
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

public fun <T> RView.forEachReorderable(
    items: Reactive<List<T>>,
    reorder: suspend (DragDropReordering.Move) -> Unit,
    separator: ViewWriter.(Reactive<T>) -> RView = { separator() },
    render: ViewWriter.(Reactive<T>) -> ViewModifiable
) {
    val handler = DragDropReordering(this, reorder = reorder)

    forEachUpdating(
        remember { items().mapIndexed { idx, it -> IndexedValue(idx, it) } }
    ) { indexed ->
        val item = indexed.lens { it.value }
        val idx = indexed.lens { it.index }
        col {
            themeTakeNonCascadingFromParent = true

            dropTargetDelegate = handler.Delegate(idx)
            ::dragData { handler.encode(idx()) }

            separator(item).apply {
                ::shown shown@{
                    val move = handler.willMove() ?: return@shown false
                    val i = idx()
                    move.end == i && move.start >= i
                }
            }

            render(item)

            separator(item).apply {
                ::shown shown@{
                    val move = handler.willMove() ?: return@shown false
                    val i = idx()
                    move.end == i && move.start < i
                }
            }
        }
    }
}

public class RecyclerReorderable<T, ID>(
    public val wraps: RecyclerViewRendererSet<T, ID>,
    public val view: Recycler2,
    public val separator: ViewWriter.(Reactive<T>) -> RView = { separator() },
    reorder: suspend (DragDropReordering.Move) -> Unit
) : RecyclerViewRendererSet<T, ID> {
    public val handler: DragDropReordering = DragDropReordering(view, reorder = reorder)

    public override fun id(item: T): ID = wraps.id(item)

    public inner class ReorderWrapper(
        public val renderer: RecyclerViewRenderer<T>
    ) : RecyclerViewRenderer<T> {
        public override fun render(viewWriter: ViewWriter, data: Reactive<T>, index: Reactive<Int>): ViewModifiable = with(viewWriter) {
            col {
                themeTakeNonCascadingFromParent = true

                dropTargetDelegate = handler.Delegate(index)
                ::dragData { handler.encode(index()) }

                separator(data).apply {
                    ::shown shown@{
                        val move = handler.willMove() ?: return@shown false
                        val i = index()
                        move.end == i && move.start >= i
                    }
                }

                renderer.render(this, data, index)

                separator(data).apply {
                    ::shown shown@{
                        val move = handler.willMove() ?: return@shown false
                        val i = index()
                        move.end == i && move.start < i
                    }
                }
            }
        }
    }

    public override fun renderer(item: T): RecyclerViewRenderer<T> = ReorderWrapper(wraps.renderer(item))
}

public fun <T, ID> Recycler2.childrenReorderable(
    items: Reactive<List<T>>,
    id: (T) -> ID,
    reorder: suspend (DragDropReordering.Move) -> Unit,
    separator: ViewWriter.(Reactive<T>) -> RView = { separator() },
    render: ViewWriter.(Reactive<T>) -> ViewModifiable
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