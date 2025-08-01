package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.Rect
import com.lightningkite.signal.*
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView
import com.lightningkite.kiteui.views.RViewWrapper
import com.lightningkite.kiteui.views.extensionHorizontalAlign
import com.lightningkite.kiteui.views.extensionVerticalAlign
import kotlinx.cinterop.*
import platform.CoreGraphics.CGPoint
import platform.CoreGraphics.CGPointMake
import platform.UIKit.*
import platform.darwin.NSObject
import kotlin.math.abs

public class ScrollView(
    context: RContext,
    public override val horizontal: Boolean,
    public override val vertical: Boolean
) : RViewWrapper(context), ScrollingBehaviors {
    public override val native = FrameLayout()
    public override fun childTouches(side: Side, child: RView): Boolean {
        return when(side) {
            Side.Left -> child.native.extensionHorizontalAlign?.touchesStart != false
            Side.Top -> child.native.extensionVerticalAlign?.touchesStart != false
            Side.Right -> child.native.extensionHorizontalAlign?.touchesEnd != false
            Side.Bottom -> child.native.extensionVerticalAlign?.touchesEnd != false
        }
    }
    public val scroller = ScrollLayout()
    init { native.addSubview(scroller) }

    private var scrollCalcOngoing = false
    private val sizeChange = BasicListenable()
    private val scroll = BasicListenable()

    public override val addChildTarget get() = scroller

    private val dg: UIScrollViewDelegateProtocol = object : NSObject(), UIScrollViewDelegateProtocol {
        override fun scrollViewDidScroll(scrollView: UIScrollView) {
//            println("scrollViewDidScroll: ${scroller.contentOffset.useContents { "$x, $y" }}")
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
            val candidates = scroller.subviews.asSequence().flatMap {
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
        scroller.horizontal = !vertical  //TODO: Support both directions
        scroller.onSizeChange = label@{
            if (scrollCalcOngoing) return@label
            scrollCalcOngoing = true
            sizeChange.invokeAll()
            scrollCalcOngoing = false
        }
        scroller.delegate = dg
        onRemove {
            scroller.delegate = null
            scroller.onSizeChange = {}
        }
    }

    public override var showScrollBars: Boolean = true
        set(value) {
            field = value
            scroller.showsHorizontalScrollIndicator = value
            scroller.showsVerticalScrollIndicator = value
        }
    public override val viewport: Readable<Rect> = (sizeChange + scroll).lensListenable {
        val (ox, oy) = scroller.contentOffset.useContents { x to y }
        val (vw, vh) = scroller.bounds.useContents { size.width to size.height }
        scroller.bounds.useContents {
            Rect.fromSize(
                left = ox,
                top = oy,
                width = vw,
                height = vh,
            )
        }
    }
    public override val content: Readable<Rect> = (sizeChange).lensListenable {
        val (sw, sh) = scroller.contentSize.useContents { width to height }
        val (vw, vh) = scroller.bounds.useContents { size.width to size.height }
        scroller.bounds.useContents {
            Rect.fromSize(
                width = vw + sw,
                height = vh + sh,
            )
        }
    }
    private val _directlyInteractingWithScroller = Property(false)
    public override val directlyInteractingWithScroller: Readable<Boolean> get() = _directlyInteractingWithScroller

    public override var snapToElements: Pair<Align?, Align?> = null to null
        set(value) {
            field = value
            // scroll to nearest?
            if(value.first == null && value.second == null) {
                scroller.decelerationRate = UIScrollViewDecelerationRateNormal
            } else {
                scroller.decelerationRate = UIScrollViewDecelerationRateFast
            }
//            scroller.content
        }
    public override var scrollSnapStop: Boolean = false

    public override fun scrollTo(left: Double, top: Double, animated: Boolean) {
        val (existingX, existingY) = scroller.contentOffset.useContents { x to y }
        val (maxX, maxY) = scroller.contentSize.useContents { width to height }
        val (sizeX, sizeY) = scroller.bounds.useContents { size.width to size.height }
//        println("ScrollView.scrollTo: ${existingX.toInt()}, ${existingY.toInt()} += ${left.toInt()}, ${top.toInt()}")
        scroller.setContentOffset(
            CGPointMake(
                x = if (horizontal) left.coerceAtMost(maxX - sizeX).coerceAtLeast(0.0) else 0.0,
                y = if (vertical) top.coerceAtMost(maxY - sizeY).coerceAtLeast(0.0) else 0.0,
            ),
            animated = animated
        )
    }
    public override fun scrollTo(element: RView, horizontal: Align, vertical: Align, animated: Boolean) {
        scrollTo(
            left = when(horizontal) {
                Align.Start -> element.native.bounds.useContents { origin.x }
                Align.Center -> (element.native.bounds.useContents { origin.x * 2 + size.width }) / 2 - scroller.bounds.useContents { size.width } / 2
                Align.End -> (element.native.bounds.useContents { origin.x + size.width }) - scroller.bounds.useContents { size.width }
                Align.Stretch -> (element.native.bounds.useContents { origin.x * 2 + size.width }) / 2 - scroller.bounds.useContents { size.width } / 2
            }.toDouble(),
            top = when(vertical) {
                Align.Start -> element.native.bounds.useContents { origin.y }
                Align.Center -> (element.native.bounds.useContents { origin.y * 2 + size.height }) / 2 - scroller.bounds.useContents { size.height } / 2
                Align.End -> (element.native.bounds.useContents { origin.y + size.height }) - scroller.bounds.useContents { size.height }
                Align.Stretch -> (element.native.bounds.useContents { origin.y * 2 + size.height }) / 2 - scroller.bounds.useContents { size.height } / 2
            }.toDouble(),
            animated = animated
        )
    }

    public override fun scrollToKeepAnimations(x: Double, y: Double) {
        val (existingX, existingY) = scroller.contentOffset.useContents { this.x to this.y }
        // Don't apply boundary locks here - caller knows what they're doing.
        scroller.contentOffset = CGPointMake(
            x = if (horizontal) x else existingX,
            y = if (vertical) y else existingY,
        )
    }
}