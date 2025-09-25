package com.lightningkite.kiteui.views.l2

import com.lightningkite.kiteui.models.DragData
import com.lightningkite.kiteui.models.DragEvent
import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.models.ThemeDerivation
import com.lightningkite.kiteui.models.div
import com.lightningkite.kiteui.models.px
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.viewDebugTarget
import com.lightningkite.kiteui.views.DropTargetDelegate
import com.lightningkite.kiteui.views.RView
import com.lightningkite.kiteui.views.ViewModifiable
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.direct.col
import com.lightningkite.kiteui.views.direct.frame
import com.lightningkite.kiteui.views.direct.separator
import com.lightningkite.kiteui.views.forEachUpdating
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch


class DragDropReordering(
    private val scope: CoroutineScope,
    val mimeType: String = "application/kiteui-index",
    val reorder: suspend (Move) -> Unit
) {
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
                        exception = { println("Exception"); false },
                        notReady = { println("Not Ready"); false }
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

fun <T> RView.forEachReorderable(
    items: Reactive<List<T>>,
    reorder: suspend (DragDropReordering.Move) -> Unit,
    separator: ViewWriter.(Reactive<T>) -> ViewModifiable = { separator() },
    dataTransform: (DragData) -> DragData = { it },
    render: ViewWriter.(Reactive<T>) -> ViewModifiable
) {
    val handler = DragDropReordering(this, reorder = reorder)

    this@forEachReorderable.gap = 0.px

    forEachUpdating(
        remember { items().mapIndexed { idx, it -> IndexedValue(idx, it) } }
    ) { indexed ->
        val item = indexed.lens { it.value }
        val idx = indexed.lens { it.index }
        col {
            themeTakeNonCascadingFromParent = true
            gap = theme.gap / 2
            this@forEachReorderable.gap = 0.px

            themeChoice += ThemeDerivation {
                it.copy(
                    id = "reorderGap",
                    cascading = false,
                    gap = it.gap / 2
                ).withoutBack
            }

            dropTargetDelegate = handler.Delegate(idx)

            beforeNextElementSetup {
                ::visible shown@{
                    val move = handler.willMove() ?: return@shown false
                    val i = idx()
                    move.end == i && move.start >= i
                }
            } - separator(item)

            beforeNextElementSetup {
                ::dragData { dataTransform(handler.encode(idx())) }
            } - render(item)

            beforeNextElementSetup {
                ::visible shown@{
                    val move = handler.willMove() ?: return@shown false
                    val i = idx()
                    move.end == i && move.start < i
                }
            } - separator(item)
        }
    }
}

class RecyclerReorderable<T, ID>(
    val wraps: RecyclerViewRendererSet<T, ID>,
    val view: Recycler2,
    val separator: ViewWriter.(Reactive<T>) -> RView = { separator() },
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
        override fun render(viewWriter: ViewWriter, data: Reactive<T>, index: Reactive<Int>): ViewModifiable = with(viewWriter) {
            col {
                themeTakeNonCascadingFromParent = true

                themeChoice

                dropTargetDelegate = handler.Delegate(index)

                separator(data).apply {
                    ::shown shown@{
                        val move = handler.willMove() ?: return@shown false
                        val i = index()
                        move.end == i && move.start >= i
                    }
                }

                beforeNextElementSetup {
                    ::dragData { handler.encode(index()) }
                } - renderer.render(this, data, index)

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

    override fun renderer(item: T): RecyclerViewRenderer<T> = ReorderWrapper(wraps.renderer(item))
}

fun <T, ID> Recycler2.childrenReorderable(
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