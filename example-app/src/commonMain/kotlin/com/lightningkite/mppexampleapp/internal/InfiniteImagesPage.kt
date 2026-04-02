package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.*
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.navigation.dialogPageNavigator
import com.lightningkite.kiteui.navigation.pageNavigator
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.l2.RecyclerViewPlacerVerticalGrid
import com.lightningkite.kiteui.views.l2.RecyclerViewPlacerVerticalTrueGrid
import com.lightningkite.kiteui.views.l2.children
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*

@Routable("recycler-view-infinite-images")
object InfiniteImagesPage : Page {
    override val title: Reactive<String>
        get() = super.title

    object ReturnIndexList: List<Int>{
        override val size: Int
            get() = 10_000
        override fun get(index: Int): Int = index
        override fun isEmpty(): Boolean = false
        override fun iterator(): Iterator<Int> = (0..<10_000).iterator()
        override fun listIterator(): ListIterator<Int> = object: ListIterator<Int> {
            var n = -1
            override fun hasNext(): Boolean = n < 10_000
            override fun hasPrevious(): Boolean = n > 0
            override fun next(): Int = ++n
            override fun nextIndex(): Int = ++n
            override fun previous(): Int = --n
            override fun previousIndex(): Int = --n
        }
        override fun listIterator(index: Int): ListIterator<Int> = object: ListIterator<Int> {
            var n = index - 1
            override fun hasNext(): Boolean = n < 10_000
            override fun hasPrevious(): Boolean = n > 0
            override fun next(): Int = ++n
            override fun nextIndex(): Int = ++n
            override fun previous(): Int = --n
            override fun previousIndex(): Int = --n
        }
        override fun subList(fromIndex: Int, toIndex: Int): List<Int> = (fromIndex..<toIndex).toList()
        override fun lastIndexOf(element: Int): Int = element
        override fun indexOf(element: Int): Int = element
        override fun containsAll(elements: Collection<Int>): Boolean = true
        override fun contains(element: Int): Boolean = true
    }

    override fun ElementWriter.CanAddTheme.render(): Unit = run {
        recyclerView {
            placer = RecyclerViewPlacerVerticalGrid(4, 1.0)
            children(Constant(ReturnIndexList), id = { it }) {
                unpadded.button {
                    ::transitionId { it().toString() }
                    sizeConstraints(aspectRatio = 1.0).image {
                        scaleType = ImageScaleType.Crop
                        ::source { ImageRemote("https://picsum.photos/seed/${it()}/100/100") }
                    }
                    onClick {
                        dialogPageNavigator.navigate(ImageViewPager(it.await()))
                    }
                }
            }
        }
    }
}

class ImageViewPager(val initialIndex: Int) : Page {
    val currentPage = Signal(initialIndex)

    override fun ElementWriter.CanAddTheme.render(): Unit = run {
        themed(ThemeDerivation { it.copy(id="dumb", background = Color.black, foreground = Color.white).withoutBack }).frame {
            val rv: ViewPager
            viewPager {
                rv = this
                children(Constant(InfiniteImagesPage.ReturnIndexList), id = { it }) { currImage ->
                    val renders = Signal(0)
                    frame {
                        ::transitionId { currImage().toString() }
                        image {
                            reactiveScope {
                                renders.value++
                                val index = currImage()
                                info = ImageView.Info(
                                    sources = listOf(
                                        ImageRemote("https://picsum.photos/seed/${index}/100/100"),
                                        ImageRemote("https://picsum.photos/seed/${index}/1000/1000"),
                                    ),
                                    scaleType = ImageScaleType.Fit,
                                    description = "an image"
                                )
                            }
                        }
                        centered.h2 { ::content { renders().toString() } }
                    }
                }
                centerIndex bind currentPage
            }
            align(Align.End, Align.Start).button {
                icon { source = Icon.close }
                onClick {
                    pageNavigator.dismiss()
                }
            }
            atBottomCenter.row {
                text {
                    ::content { "currentPage ${currentPage()}" }
                }
                text {
                    content = "I never update because I'm a loser"
                    ::content { "rv.index ${rv.centerIndex()}" }
                }
            }
            atBottomStart.button {
                text("jump to #20")
                onClick {
                    currentPage.set(20)
                }
            }
        }
    }
}