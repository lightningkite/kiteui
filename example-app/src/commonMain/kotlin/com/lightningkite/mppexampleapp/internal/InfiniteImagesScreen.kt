package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.*
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.navigation.Screen
import com.lightningkite.kiteui.navigation.dialogScreenNavigator
import com.lightningkite.kiteui.navigation.screenNavigator
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import kotlinx.coroutines.delay

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
                    ::transitionId { it().toString() }
                    sizeConstraints(aspectRatio = 1.0) - image {
                        viewDebugTarget = this
                        scaleType = ImageScaleType.Crop
                        ::source { ImageRemote("https://picsum.photos/seed/${it()}/100/100") }
                    }
                    onClick {
                        dialogScreenNavigator.navigate(ImageViewPager(it.await()))
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
                        ::transitionId { currImage().toString() }
                        spacing = 0.25.rem
                        zoomableImage {
                            reactiveScope {
                                renders.value++
                                val index = currImage()
                                source = ImageRemote("https://picsum.photos/seed/${index}/100/100")
                                async(index) { delay(1) }
                                source = ImageRemote("https://picsum.photos/seed/${index}/1000/1000")
                            }
                            scaleType = ImageScaleType.Fit
                        }
                        centered - h2 { ::content { renders().toString() } }
                    }
                }
                index bind currentPage
            }
            gravity(Align.End, Align.Start) - button {
                icon { source = Icon.close }
                onClick {
                    screenNavigator.dismiss()
                }
            }
            atBottomCenter - row {
                text {
                    ::content { "currentPage ${currentPage()}" }
                }
                text {
                    content = "I never update because I'm a loser"
                    ::content { "rv.index ${rv.index()}" }
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