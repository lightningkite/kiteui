package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.views.ViewWriter
import android.content.Context
import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView.LayoutManager
import com.lightningkite.kiteui.afterTimeout
import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.Dimension
import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import kotlin.math.roundToInt
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

    val spacingDecor = SpacingItemDecoration(0).apply {
        native.addItemDecoration(this)
    }
    override fun applyForeground(theme: Theme) {
        super.applyForeground(theme)
        spacingDecor.spacing = (spacing ?: if(useNavSpacing) theme.navSpacing else theme.spacing).value.roundToInt()
        native.requestLayout()
    }
    override fun spacingSet(value: Dimension?) {
        super.spacingSet(value)
        spacingDecor.spacing = (spacing ?: if(useNavSpacing) theme.navSpacing else theme.spacing).value.roundToInt()
        native.requestLayout()
    }

    override fun internalAddChild(index: Int, view: RView) {
        // Do nothing.  All children are virtual and managed by the native recycler view.
    }

    override fun internalClearChildren() {
         // Do nothing.  All children are virtual and managed by the native recycler view.
    }

    override fun internalRemoveChild(index: Int) {
        // Do nothing.  All children are virtual and managed by the native recycler view.
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
//                    println("Data set to loading")
                    loading = true
                    notifyDataSetChanged()
//                    println("Data set to loading complete")
                }) {
                    val new = items().toList()
//                    println("Data set to $new")
                    loading = false
                    lastPublished = new
                    notifyDataSetChanged()
//                    println("Data set to new complete")
                }
            }
        }
    }
//    actual fun <T, ID> children(
//        items: Readable<List<T>>,
//        identity: (T) -> ID,
//        render: ViewWriter.(value: Readable<T>) -> Unit
//    ): Unit {
//        native.adapter = object : ObservableRVA<T>(
//            recyclerView = this,
//            calculationContext = this,
//            layoutManager = native.layoutManager,
//            placeholderCount = 5,
//            determineType = { 0 },
//            makeView = { _, obs -> render(obs) }) {
//            init {
//                val differ = AsyncListDiffer(this, object: DiffUtil.ItemCallback<T>() {
//                    override fun areContentsTheSame(oldItem: T & Any, newItem: T & Any): Boolean = identity(oldItem) == identity(newItem)
//                    override fun areItemsTheSame(oldItem: T & Any, newItem: T & Any): Boolean = oldItem == newItem
//                })
//                reactiveScope(onLoad = {
////                    println("Data set to loading")
//                    loading = true
//                    notifyDataSetChanged()
////                    println("Data set to loading complete")
//                }) {
//                    val new = items().toList()
////                    println("Data set to $new")
//                    loading = false
//                    lastPublished = new
//                    differ.submitList(new)
//                    notifyDataSetChanged()
////                    println("Data set to new complete")
//                }
//            }
//        }
//    }


    class SpacingItemDecoration(var spacing: Int) : AndroidRecyclerView.ItemDecoration() {
        override fun getItemOffsets(
            outRect: Rect,
            view: View,
            parent: androidx.recyclerview.widget.RecyclerView,
            state: androidx.recyclerview.widget.RecyclerView.State
        ) {
            when(val m = parent.layoutManager) {
                is GridLayoutManager -> {
                    val horizontal = m.orientation == GridLayoutManager.HORIZONTAL
                    val pos = parent.getChildAdapterPosition(view)
                    val firstSpan = pos / m.spanCount == 0
                    val firstInSpan = pos % m.spanCount == 0
                    if (horizontal) {
                        outRect.left = if(firstSpan) 0 else spacing
                        outRect.top = if(firstInSpan) 0 else spacing
                    } else {
                        outRect.left = if(firstInSpan) 0 else spacing
                        outRect.top = if(firstSpan) 0 else spacing
                    }
                }
                is LinearLayoutManager -> {
                    val horizontal = m.orientation == LinearLayoutManager.HORIZONTAL
                    val first = parent.getChildAdapterPosition(view) == 0
                    outRect.left = if (!first && horizontal) spacing else 0
                    outRect.top = if (!first && !horizontal) spacing else 0
                }
            }
        }
    }


    internal open class ObservableRVA<T>(
        val recyclerView: RView,
        val calculationContext: CalculationContext,
        val layoutManager: LayoutManager?,
        val placeholderCount: Int = 0,
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
            val w = object: ViewWriter(), CalculationContext by recyclerView {
                override val context: RContext
                    get() = recyclerView.context

                override fun addChild(view: RView) {
                    recyclerView.addChild(view)
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
    fun scrollto() {
        if (animate) {
            when (val lm = native.layoutManager ?: return) {
                is LinearLayoutManager -> if (align == null) lm.smoothScrollToPosition(
                    native,
                    AndroidRecyclerView.State(),
                    index
                ) else lm.startSmoothScroll(AlignSmoothScroller(native.context, align).also { it.targetPosition = index })

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
    if(index in 0..<(native.adapter?.itemCount ?: 0)) scrollto()
    else afterTimeout(20) { if(index in 0..<(native.adapter?.itemCount ?: 0)) scrollto() }
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
