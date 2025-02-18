package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.Rect
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView
import com.lightningkite.kiteui.views.RViewWrapper
import kotlinx.cinterop.*
import platform.CoreGraphics.CGPoint
import platform.CoreGraphics.CGPointMake
import platform.UIKit.UIScrollView
import platform.UIKit.UIScrollViewDelegateProtocol
import platform.UIKit.UIView
import platform.darwin.NSObject
import kotlin.math.abs

class ScrollView(
    context: RContext,
    override val horizontal: Boolean,
    override val vertical: Boolean
) : RViewWrapper(context), ScrollingBehaviors {
    override val native = ScrollLayout()

    private var scrollCalcOngoing = false
    private val sizeChange = BasicListenable()
    private val scroll = BasicListenable()
    private val dg: UIScrollViewDelegateProtocol = object : NSObject(), UIScrollViewDelegateProtocol {
        override fun scrollViewDidScroll(scrollView: UIScrollView) {
//            println("scrollViewDidScroll: ${native.contentOffset.useContents { "$x, $y" }}")
            if (scrollCalcOngoing) return
            scrollCalcOngoing = true
            scroll.invokeAll()
            scrollCalcOngoing = false
        }

        override fun scrollViewWillBeginDragging(scrollView: UIScrollView) {
            _directlyInteractingWithScroller.value = true
        }

        override fun scrollViewDidEndDragging(scrollView: UIScrollView, willDecelerate: Boolean) {
        }

        override fun scrollViewWillEndDragging(
            scrollView: UIScrollView,
            withVelocity: CValue<CGPoint>,
            targetContentOffset: CPointer<CGPoint>?
        ) {
            targetContentOffset ?: return
            _directlyInteractingWithScroller.value = false

            // snap!
            val candidates = native.subviews.asSequence().flatMap {
                (it as UIView).subviews.asSequence() as Sequence<UIView>
            }
            if(candidates.none()) return

            fun UIView.x() = when(snapToElements.first) {
                Align.Start -> center.useContents { x } - bounds.useContents { size.width } / 2
                Align.End -> center.useContents { x } + bounds.useContents { size.width } / 2
                else -> center.useContents { x }
            }
            fun UIView.y() = when(snapToElements.second) {
                Align.Start -> center.useContents { y } - bounds.useContents { size.height } / 2
                Align.End -> center.useContents { y } + bounds.useContents { size.height } / 2
                else -> center.useContents { y }
            }
            val viewportXSize = scrollView.bounds.useContents { size.width }
            val viewportYSize = scrollView.bounds.useContents { size.height }
            val currentX = when(snapToElements.first) {
                Align.Start -> scrollView.contentOffset.useContents { x }
                Align.End -> scrollView.contentOffset.useContents { x } + viewportXSize
                else -> scrollView.contentOffset.useContents { x } + viewportXSize / 2
            }
            val currentY = when(snapToElements.second) {
                Align.Start -> scrollView.contentOffset.useContents { y }
                Align.End -> scrollView.contentOffset.useContents { y } + viewportYSize
                else -> scrollView.contentOffset.useContents { y } + viewportYSize / 2
            }
            val focusX = when(snapToElements.first) {
                Align.Start -> targetContentOffset.pointed.x
                Align.End -> targetContentOffset.pointed.x + viewportXSize
                else -> targetContentOffset.pointed.x + viewportXSize / 2
            }
            val focusY = when(snapToElements.second) {
                Align.Start -> targetContentOffset.pointed.y
                Align.End -> targetContentOffset.pointed.y + viewportYSize
                else -> targetContentOffset.pointed.y + viewportYSize / 2
            }
            println("currentX: $currentX")
            println("currentY: $currentY")
            println("focusX: $focusX")
            println("focusY: $focusY")


            val (candidatesX, candidatesY) = if(scrollSnapStop) {
                sequenceOf(
                    candidates.filter { it.x() < currentX }.maxByOrNull { it.x() } ?: candidates.minBy { it.x() },
                    candidates.filter { it.x() > currentX }.minByOrNull { it.x() } ?: candidates.maxBy { it.x() },
                ) to sequenceOf(
                    candidates.filter { it.y() < currentY }.maxByOrNull { it.y() } ?: candidates.minBy { it.y() },
                    candidates.filter { it.y() > currentY }.minByOrNull { it.y() } ?: candidates.maxBy { it.y() },
                )
            } else candidates to candidates

            val x = candidatesX.minBy { abs(it.x() - focusX) }
            val y = candidatesY.minBy { abs(it.y() - focusY) }

            snapToElements.first?.let {
                when(it) {
                    Align.Start -> targetContentOffset.pointed.x = x.x()
                    Align.End -> targetContentOffset.pointed.x = x.x() - viewportXSize
                    Align.Center,
                    Align.Stretch -> targetContentOffset.pointed.x = x.x() - viewportXSize / 2
                }
            }
            snapToElements.second?.let {
                when(it) {
                    Align.Start -> targetContentOffset.pointed.y = y.y()
                    Align.End -> targetContentOffset.pointed.y = y.y() - viewportYSize
                    Align.Center,
                    Align.Stretch -> targetContentOffset.pointed.y = y.y() - viewportYSize / 2
                }
            }
            println("targetContentOffset: ${targetContentOffset.pointed.run { "$x, $y" }}")
        }
    }

    init {
        native.horizontal = !vertical  //TODO: Support both directions
        native.onSizeChange = label@{
            if (scrollCalcOngoing) return@label
            scrollCalcOngoing = true
            sizeChange.invokeAll()
            scrollCalcOngoing = false
        }
        native.delegate = dg
        onRemove {
            native.delegate = null
            native.onSizeChange = {}
        }
    }

    override var showScrollBars: Boolean = true
        set(value) {
            field = value
            native.showsHorizontalScrollIndicator = value
            native.showsVerticalScrollIndicator = value
        }
    override val viewport: Readable<Rect> = (sizeChange + scroll).lensListenable {
        val (ox, oy) = native.contentOffset.useContents { x to y }
        val (vw, vh) = native.bounds.useContents { size.width to size.height }
        native.bounds.useContents {
            Rect.fromSize(
                left = ox,
                top = oy,
                width = vw,
                height = vh,
            )
        }
    }
    override val content: Readable<Rect> = (sizeChange).lensListenable {
        val (sw, sh) = native.contentSize.useContents { width to height }
        val (vw, vh) = native.bounds.useContents { size.width to size.height }
        native.bounds.useContents {
            Rect.fromSize(
                width = vw + sw,
                height = vh + sh,
            )
        }
    }
    private val _directlyInteractingWithScroller = Property(false)
    override val directlyInteractingWithScroller: Readable<Boolean> get() = _directlyInteractingWithScroller

    override var snapToElements: Pair<Align?, Align?> = null to null
        set(value) {
            field = value
            // scroll to nearest?
//            native.content
        }
    override var scrollSnapStop: Boolean = false

    override fun scrollTo(left: Double, top: Double, animated: Boolean) {
        val (existingX, existingY) = native.contentOffset.useContents { x to y }
//        println("ScrollView.scrollTo: ${existingX.toInt()}, ${existingY.toInt()} += ${left.toInt()}, ${top.toInt()}")
        native.setContentOffset(
            CGPointMake(
                x = if (horizontal) left.coerceAtLeast(0.0) else 0.0,
                y = if (vertical) top.coerceAtLeast(0.0) else 0.0,
            ),
            animated = animated
        )
    }
    override fun scrollTo(element: RView, horizontal: Align, vertical: Align, animated: Boolean) {
        scrollTo(
            left = when(horizontal) {
                Align.Start -> element.native.bounds.useContents { origin.x }
                Align.Center -> (element.native.bounds.useContents { origin.x * 2 + size.width }) / 2 - native.bounds.useContents { size.width } / 2
                Align.End -> (element.native.bounds.useContents { origin.x + size.width }) - native.bounds.useContents { size.width }
                Align.Stretch -> (element.native.bounds.useContents { origin.x * 2 + size.width }) / 2 - native.bounds.useContents { size.width } / 2
            }.toDouble(),
            top = when(vertical) {
                Align.Start -> element.native.bounds.useContents { origin.y }
                Align.Center -> (element.native.bounds.useContents { origin.y * 2 + size.height }) / 2 - native.bounds.useContents { size.height } / 2
                Align.End -> (element.native.bounds.useContents { origin.y + size.height }) - native.bounds.useContents { size.height }
                Align.Stretch -> (element.native.bounds.useContents { origin.y * 2 + size.height }) / 2 - native.bounds.useContents { size.height } / 2
            }.toDouble(),
            animated = animated
        )
    }

    override fun scrollToKeepAnimations(x: Double, y: Double) {
        val (existingX, existingY) = native.contentOffset.useContents { this.x to this.y }
//        println("ScrollView.offset: ${existingX.toInt()}, ${existingY.toInt()} += ${x.toInt()}, ${y.toInt()}")
        native.contentOffset = CGPointMake(
            x = if (horizontal) x.coerceAtLeast(0.0) else existingX,
            y = if (vertical) y.coerceAtLeast(0.0) else existingY,
        )
    }
}