package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.Rect
import com.lightningkite.kiteui.reactive.BasicListenable
import com.lightningkite.kiteui.reactive.Readable
import com.lightningkite.kiteui.reactive.onRemove
import com.lightningkite.kiteui.reactive.lens
import com.lightningkite.kiteui.reactive.plus
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGPointMake
import platform.UIKit.UIScrollView
import platform.UIKit.UIScrollViewDelegateProtocol
import platform.darwin.NSObject

actual class ScrollView actual constructor(
    context: RContext,
    actual val horizontal: Boolean,
    actual val vertical: Boolean
) : RView(context) {
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

    actual var showScrollBars: Boolean = true
        set(value) {
            field = value
            native.showsHorizontalScrollIndicator = value
            native.showsVerticalScrollIndicator = value
        }
    actual val scrollReason: Readable<ScrollReason>
        get() = TODO("Not yet implemented")
    actual val viewport: Readable<Rect> = (sizeChange + scroll).lens {
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
    actual val content: Readable<Rect> = (sizeChange).lens {
        val (sw, sh) = native.contentSize.useContents { width to height }
        val (vw, vh) = native.bounds.useContents { size.width to size.height }
        native.bounds.useContents {
            Rect.fromSize(
                width = vw + sw,
                height = vh + sh,
            )
        }
    }

    actual fun scrollTo(left: Double, top: Double, animated: Boolean) {
        val (existingX, existingY) = native.contentOffset.useContents { x to y }
//        println("ScrollView.scrollTo: ${existingX.toInt()}, ${existingY.toInt()} += ${left.toInt()}, ${top.toInt()}")
        native.setContentOffset(
            CGPointMake(
                x = if (horizontal) left else 0.0,
                y = if (vertical) top else 0.0,
            ),
            animated = animated
        )
    }
    actual fun scrollTo(element: RView, horizontal: Align, vertical: Align, animated: Boolean) {
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

    actual fun offset(x: Double, y: Double) {
        val (existingX, existingY) = native.contentOffset.useContents { this.x to this.y }
//        println("ScrollView.offset: ${existingX.toInt()}, ${existingY.toInt()} += ${x.toInt()}, ${y.toInt()}")
        native.contentOffset = CGPointMake(
            x = if (horizontal) existingX + x else existingX,
            y = if (vertical) existingY + y else existingY,
        )
    }
}