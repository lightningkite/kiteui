package com.lightningkite.mppexampleapp

import ViewWriter
import com.lightningkite.kiteui.*
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.navigation.Screen
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*

@Routable("recycler-view-infinite-images")
object InfiniteImagesScreen : Screen {
    override val title: Readable<String>
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

    override fun ViewWriter.render() {
        recyclerView {
            columns = 4
            children(Constant(ReturnIndexList)) {
                unpadded - button {
                    sizeConstraints(height = 16.rem) - image {
                        scaleType = ImageScaleType.Crop
                        ::source { ImageRemote("https://picsum.photos/seed/${it.await()}/1000/1000") }
//                            source = Resources.imagesSolera
                    }
                    onClick {
                        navigator.dialog.navigate(ImageViewPager(it.await()))
                    }
                }
            }
        }
    }
}

class ImageViewPager(val initialIndex: Int) : Screen {
    val currentPage = Property(initialIndex)

    override fun ViewWriter.render() {
        stack {
            val rv: ViewPager
            viewPager {
                rv = this
                children(Constant(InfiniteImagesScreen.ReturnIndexList)) { currImage ->
                    val renders = Property(0)
                    stack {
                        spacing = 0.25.rem
                        image {
                            reactiveScope {
                                renders.value++
                                val index = currImage.await()
                                source = ImageRemote("https://picsum.photos/seed/${index}/100/100")
                                delay(1)
                                source = ImageRemote("https://picsum.photos/seed/${index}/1000/1000")
                            }
                            scaleType = ImageScaleType.Fit
                        }
                        centered - h2 { ::content { renders.await().toString() } }
                    }
                }
                index bind currentPage
            }
            gravity(Align.End, Align.Start) - button {
                icon { source = Icon.close }
                onClick {
                    navigator.dismiss()
                }
            }
            atBottomCenter - row {
                text {
                    ::content { "currentPage ${currentPage.await()}" }
                }
                text {
                    content = "I never update because I'm a loser"
                    ::content { "rv.index ${rv.index.await()}" }
                }
            }
            atBottomStart - button {
                text("jump to #20")
                onClick {
                    currentPage.set(20)
                }
            }
        } in themeFromLast { it.copy(background = Color.black, foreground = Color.white) }
    }
}