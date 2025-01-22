@file:Suppress("DEPRECATION")

package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.px
import com.lightningkite.kiteui.reactive.Readable
import com.lightningkite.kiteui.reactive.Writable
import com.lightningkite.kiteui.reactive.reactive
import com.lightningkite.kiteui.views.RView
import com.lightningkite.kiteui.views.ViewDsl
import com.lightningkite.kiteui.views.ViewModifiable
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.l2.*
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.coroutines.CoroutineContext

typealias ContainingView = RView

@Deprecated("Move to using Recycler2 directly for more power")
class RecyclerUpgradeMidway(viewWriter: ViewWriter, val vertical: Boolean): ViewModifiable {
    val new = Recycler2(viewWriter, vertical)
    var spacing
        get() = new.cells.spacing
        set(value) { new.cells.spacing = value }
    var exists
        get() = new.outerStack.exists
        set(value) { new.outerStack.exists = value }
    override val coroutineContext: CoroutineContext
        get() = new.coroutineContext

    var columns: Int = 1
        set(value) { field = value; updatePlacer() }
    private fun updatePlacer() {
        new.placer = if(vertical)
            RecyclerViewPlacerVerticalGrid(columns)
        else
            RecyclerViewPlacerHorizontalGrid(columns)
    }

    fun <T> children(items: Readable<List<T>>, render: ViewWriter.(value: Readable<T>) -> Unit): Unit {
        var currentData: List<T> = listOf()
        new.rendererSet = object: RecyclerViewRendererSet<T, Int> {
            override fun id(item: T): Int = currentData.indexOf(item)
            val r = object: RecyclerViewRenderer<T> {
                override fun render(viewWriter: ViewWriter, data: Readable<T>, index: Readable<Int>): ViewModifiable {
                    viewWriter.render(data)
                    return viewWriter.lastWrittenView!!
                }
            }
            override fun renderer(item: T): RecyclerViewRenderer<T> = r
        }
        reactive {
            currentData = items()
            new.data = object: RecyclerViewData<T, Int> {
                override val range: IntRange = currentData.indices
                override fun get(index: Int): T {
                    if(index !in currentData.indices) throw IllegalStateException("Index $index out of range for ${currentData.indices}")
                    return currentData[index]
                }
            }
        }
    }
    fun scrollToIndex(index: Int, align: Align? = null, animate: Boolean = true) {
        new.scrollToIndex(index, align ?: Align.Center, animate)
    }
    val firstVisibleIndex: Readable<Int> get() = new.firstIndex
    val lastVisibleIndex: Readable<Int> get() = new.lastIndex
}

@Deprecated("Move to using Recycler2 directly for more power")
class ViewPagerUpgradeMidway(viewWriter: ViewWriter): ViewModifiable {
    val new = Recycler2(viewWriter, false)
    var spacing
        get() = new.cells.spacing
        set(value) { new.cells.spacing = value }
    var exists
        get() = new.outerStack.exists
        set(value) { new.outerStack.exists = value }
    override val coroutineContext: CoroutineContext
        get() = new.coroutineContext

    init {
        new.placer = RecyclerViewPagingPlacer()
        new.snapToElements = Align.Center
        new.scrollSnapStop = true
    }

    fun <T> children(items: Readable<List<T>>, render: ViewWriter.(value: Readable<T>) -> Unit): Unit {
        var currentData: List<T> = listOf()
        new.rendererSet = object: RecyclerViewRendererSet<T, Int> {
            override fun id(item: T): Int = currentData.indexOf(item)
            val r = object: RecyclerViewRenderer<T> {
                override fun render(viewWriter: ViewWriter, data: Readable<T>, index: Readable<Int>): ViewModifiable {
                    viewWriter.render(data)
                    return viewWriter.lastWrittenView!!
                }
            }
            override fun renderer(item: T): RecyclerViewRenderer<T> = r
        }
        reactive {
            currentData = items()
            new.data = object: RecyclerViewData<T, Int> {
                override val range: IntRange = currentData.indices
                override fun get(index: Int): T = currentData[index]
            }
        }
    }
    val index: Writable<Int> get() = new.centerIndex
}

@Deprecated("Move to using Recycler2 directly for more power") typealias RecyclerView = RecyclerUpgradeMidway
@Deprecated("Move to using Recycler2 directly for more power") typealias ViewPager = ViewPagerUpgradeMidway

@Deprecated("Use Recycler2 directly for more power")
@OptIn(ExperimentalContracts::class)
@ViewDsl
inline fun ViewWriter.recyclerView(setup: RecyclerUpgradeMidway.() -> Unit = {}): RecyclerUpgradeMidway {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return RecyclerUpgradeMidway(this, true).apply(setup)
}
@OptIn(ExperimentalContracts::class)
@ViewDsl
inline fun ViewWriter.viewPager(setup: ViewPagerUpgradeMidway.() -> Unit = {}): ViewPagerUpgradeMidway {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return ViewPagerUpgradeMidway(this).apply(setup)
}
@OptIn(ExperimentalContracts::class)
@ViewDsl
inline fun ViewWriter.horizontalRecyclerView(setup: RecyclerUpgradeMidway.() -> Unit = {}): RecyclerUpgradeMidway {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return RecyclerUpgradeMidway(this, false).apply(setup)
}