package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.views.ViewWriter
import android.content.Context
import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView.LayoutManager
import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import androidx.recyclerview.widget.RecyclerView as AndroidRecyclerView

actual class RecyclerView actual constructor(context: RContext) : RView(context) {
    private val firstVisibleIndexProp = Property(0)
    private val lastVisibleIndexProp = Property(0)

    actual val firstVisibleIndex: Readable<Int> get() = firstVisibleIndexProp
    actual val lastVisibleIndex: Readable<Int> get() = lastVisibleIndexProp

    actual var columns: Int = 1
        set(value) {
            field = value
            (native.layoutManager as? GridLayoutManager)?.spanCount = value
        }
    actual var vertical: Boolean = true
        set(value) {
            field = value
            native.layoutManager = GridLayoutManager(context.activity, columns, if(value) GridLayoutManager.VERTICAL else GridLayoutManager.HORIZONTAL, false)
        }

    override val native = AndroidRecyclerView(context.activity).apply {
        layoutManager = GridLayoutManager(context.activity, 1, GridLayoutManager.VERTICAL, false)
        addOnScrollListener(object : androidx.recyclerview.widget.RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: androidx.recyclerview.widget.RecyclerView, dx: Int, dy: Int) {
                ((recyclerView.layoutManager as? LinearLayoutManager)?.findFirstCompletelyVisibleItemPosition()
                    ?: (recyclerView.layoutManager as? GridLayoutManager)?.findFirstCompletelyVisibleItemPosition())?.let {
                    if (firstVisibleIndexProp.value != it) firstVisibleIndexProp.value = it
                }
                ((recyclerView.layoutManager as? LinearLayoutManager)?.findLastCompletelyVisibleItemPosition()
                    ?: (recyclerView.layoutManager as? GridLayoutManager)?.findLastCompletelyVisibleItemPosition())?.let {
                    if (lastVisibleIndexProp.value != it) lastVisibleIndexProp.value = it
                }
            }
        })
    }

    actual fun <T> children(
        items: Readable<List<T>>,
        render: ViewWriter.(value: Readable<T>) -> Unit
    ): Unit {
        native.adapter = object : ObservableRVA<T>(
            recyclerView = this,
            calculationContext = this,
            layoutManager = native.layoutManager,
            placeholderCount = 5,
            determineType = { 0 },
            makeView = { _, obs -> render(obs) }) {
            init {
                reactiveScope(onLoad = {
                    loading = true
                    notifyDataSetChanged()
                }) {
                    val new = items.await().toList()
                    loading = false
                    lastPublished = new
                    notifyDataSetChanged()
                }
            }
        }
    }


    class SpacingItemDecoration(var spacing: Int) : AndroidRecyclerView.ItemDecoration() {
        override fun getItemOffsets(
            outRect: Rect,
            view: View,
            parent: androidx.recyclerview.widget.RecyclerView,
            state: androidx.recyclerview.widget.RecyclerView.State
        ) {
            outRect.left = spacing
            outRect.top = spacing
            outRect.bottom = spacing
            outRect.right = spacing
        }
    }


    internal open class ObservableRVA<T>(
        val recyclerView: RView,
        val calculationContext: CalculationContext,
        val layoutManager: LayoutManager?,
        val placeholderCount: Int = 5,
        val determineType: (T) -> Int,
        val makeView: ViewWriter.(Int, Readable<T>) -> Unit
    ) : AndroidRecyclerView.Adapter<AndroidRecyclerView.ViewHolder>() {
        interface ParentView {
        }

        var lastPublished: List<T> = listOf()
        var loading: Boolean = false

        override fun getItemViewType(position: Int): Int {
            return if (loading) 0
            else determineType(lastPublished[position])
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AndroidRecyclerView.ViewHolder {
            val event = LateInitProperty<T>()
            var newView: RView? = null
            val w = object: ViewWriter() {
                override val context: RContext
                    get() = recyclerView.context

                override fun addChild(view: RView) {
                    view.parent = recyclerView
                    newView = view
                }
            }
            recyclerView.withoutAnimation {
                w.makeView(viewType, event)
            }
            val subview = newView?.native ?: throw IllegalArgumentException("makeView created no views in a RecyclerView!")
            subview.layoutParams = AndroidRecyclerView.LayoutParams(
                if ((layoutManager as? LinearLayoutManager)?.orientation != LinearLayoutManager.HORIZONTAL)
                    AndroidRecyclerView.LayoutParams.MATCH_PARENT else AndroidRecyclerView.LayoutParams.WRAP_CONTENT,
                if ((layoutManager as? LinearLayoutManager)?.orientation != LinearLayoutManager.VERTICAL)
                    AndroidRecyclerView.LayoutParams.MATCH_PARENT else AndroidRecyclerView.LayoutParams.WRAP_CONTENT,
            )
            subview.tag = event
//            calculationContext.onRemove { newView?.shutdown() }
            return object : AndroidRecyclerView.ViewHolder(subview) {}
        }

        override fun getItemCount(): Int = if (loading) placeholderCount else lastPublished.size

        @Suppress("UNCHECKED_CAST")
        override fun onBindViewHolder(holder: AndroidRecyclerView.ViewHolder, position: Int) {
            val prop = (holder.itemView.tag as? LateInitProperty<T> ?: run {
                println("Failed to find property to update")
                null
            })
            holder.itemView.withoutAnimation {
                if (loading)
                    prop?.unset()
                else
                    prop?.value = (lastPublished[position])
            }
        }
    }

    actual fun scrollToIndex(
        index: Int,
        align: Align?,
        animate: Boolean
    ) {
        if (animate) {
            when (val lm = native.layoutManager ?: return) {
                is LinearLayoutManager -> if (align == null) lm.smoothScrollToPosition(
                    native,
                    AndroidRecyclerView.State(),
                    index
                ) else lm.startSmoothScroll(AlignSmoothScroller(native.context, align).also {
                    it.targetPosition = index
                })

                else -> lm.smoothScrollToPosition(native, AndroidRecyclerView.State(), index)
            }
        } else {
            when (val lm = native.layoutManager ?: return) {
                is LinearLayoutManager -> if (align == null) lm.scrollToPosition(index)
                else lm.scrollToPositionWithOffset(index, native.height / 2)

                else -> lm.scrollToPosition(index)
            }
        }
    }


    private class AlignSmoothScroller(context: Context, val align: Align?) : LinearSmoothScroller(context) {
        override fun calculateDtToFit(
            viewStart: Int,
            viewEnd: Int,
            boxStart: Int,
            boxEnd: Int,
            snapPreference: Int
        ): Int {
            return when (align) {
                Align.Start -> boxStart - viewStart
                Align.Center -> boxStart + (boxEnd - boxStart) / 2 - (viewStart + (viewEnd - viewStart) / 2)
                Align.End -> boxStart + (boxEnd - boxStart) - (viewStart + (viewEnd - viewStart))
                else -> boxStart + (boxEnd - boxStart) / 2 - (viewStart + (viewEnd - viewStart) / 2)
            }
        }
    }
}
