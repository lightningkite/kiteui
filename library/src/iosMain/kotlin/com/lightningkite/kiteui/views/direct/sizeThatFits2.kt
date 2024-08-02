package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.SizeConstraints
import com.lightningkite.kiteui.utils.fitInsideBox
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSize
import platform.CoreGraphics.CGSizeMake
import platform.UIKit.UIView
import com.lightningkite.kiteui.PerformanceInfo

@OptIn(ExperimentalForeignApi::class)
fun UIView.sizeThatFits2(
    size: CValue<CGSize>,
    sizeConstraints: SizeConstraints?
): CValue<CGSize> {
    // Completely override native sizeThatFits algo for some view elements
    val nativeBestSize = when (this) {
        is MyImageView -> size
        else -> sizeThatFits(size)
    }
    val newSize = sizeConstraints?.let {
        var w = nativeBestSize.useContents { width }
        var h = nativeBestSize.useContents { height }
        it.maxWidth?.let { w = w.coerceAtMost(it.value) }
        it.maxHeight?.let { h = h.coerceAtMost(it.value) }
        it.minWidth?.let { w = w.coerceAtLeast(it.value) }
        it.minHeight?.let { h = h.coerceAtLeast(it.value) }
        it.aspectRatio?.let { aspectRatio ->
            aspectRatio.fitInsideBox(w, h).let { innerBox ->
                w = innerBox.first
                h = innerBox.second
            }
        }
        it.width?.let { w = it.value }
        it.height?.let { h = it.value }
        CGSizeMake(w, h)
    } ?: nativeBestSize
    return when (this) {
        is LinearLayout,
        is FrameLayout,
        is FrameLayoutButton -> sizeThatFits(newSize)

        else -> {
            // Uncomment this code if you believe some fool is using the default sizeThatFits.
            // That default implementation just returns the current size, leading to really strange and hard to track errors.
            // Don't leave this uncommented, though; it's slow.  Just override the view in question and fix its sizeThatFits implementation.
//            val xExisting = bounds.useContents { origin.x }
//            val yExisting = bounds.useContents { origin.y }
//            val widthExisting = bounds.useContents { this.size.width }
//            val heightExisting = bounds.useContents { this.size.height }
//            setBounds(CGRectMake(0.0, 0.0, 0.0, 0.0))
            val result = PerformanceInfo["nativeSizeThatFits"]{ sizeThatFits(newSize) }
//            setBounds(
//                CGRectMake(
//                xExisting,
//                yExisting,
//                widthExisting,
//                heightExisting,
//            )
//            )
            result
        }
    }
}