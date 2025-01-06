package com.lightningkite.kiteui.views.direct

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

    actual fun scrollTo(top: Double, left: Double, animated: Boolean) {
        native.setContentOffset(
            CGPointMake(
                x = if (horizontal) left else 0.0,
                y = if (vertical) top else 0.0,
            ),
            animated = animated
        )
    }
}