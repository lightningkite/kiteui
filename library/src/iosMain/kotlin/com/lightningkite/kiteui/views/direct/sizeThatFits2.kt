package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.SizeConstraints
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSize
import platform.CoreGraphics.CGSizeMake
import platform.UIKit.UIView
import com.lightningkite.kiteui.PerformanceInfo
import com.lightningkite.kiteui.viewDebugTarget

@OptIn(ExperimentalForeignApi::class)
fun UIView.sizeThatFits2(
    size: CValue<CGSize>,
    sizeConstraints: SizeConstraints?
): CValue<CGSize> {
    val newSizeInput = sizeConstraints?.let {
        var w = size.useContents { width }
        var h = size.useContents { height }
        it.maxWidth?.let { w = w.coerceAtMost(it.value) }
        it.maxHeight?.let { h = h.coerceAtMost(it.value) }
        it.minWidth?.let { w = w.coerceAtLeast(it.value) }
        it.minHeight?.let { h = h.coerceAtLeast(it.value) }
        it.width?.let { w = it.value }
        it.height?.let { h = it.value }
        it.aspectRatio?.let { aspectRatio ->
            if (w / h > aspectRatio) {
                w = h * aspectRatio
            } else {
                h = w / aspectRatio
            }
        }
        CGSizeMake(w, h)
    } ?: size
    val measured: CValue<CGSize>
    if (sizeConstraints?.aspectRatio != null) {
        measured = newSizeInput
    } else {
        measured = when (this) {
            is LinearLayout,
            is FrameLayout,
            is FrameLayoutButton -> sizeThatFits(newSizeInput)

            else -> {
                // Uncomment this code if you believe some fool is using the default sizeThatFits.
                // That default implementation just returns the current size, leading to really strange and hard to track errors.
                // Don't leave this uncommented, though; it's slow.  Just override the view in question and fix its sizeThatFits implementation.
    //            val xExisting = bounds.useContents { origin.x }
    //            val yExisting = bounds.useContents { origin.y }
    //            val widthExisting = bounds.useContents { this.size.width }
    //            val heightExisting = bounds.useContents { this.size.height }
    //            setBounds(CGRectMake(0.0, 0.0, 0.0, 0.0))
                val result = PerformanceInfo["nativeSizeThatFits"]{ sizeThatFits(newSizeInput) }
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
    val result = measured
    if(this === viewDebugTarget?.native) {
        println("viewDebugTarget constraints: $sizeConstraints")
        println("viewDebugTarget size: ${size.useContents { "$width, $height" }}")
        println("viewDebugTarget newSizeInput: ${newSizeInput.useContents { "$width, $height" }}")
        println("viewDebugTarget measured: ${measured.useContents { "$width, $height" }}")
        println("viewDebugTarget result: ${result.useContents { "$width, $height" }}")
    }
    return result
}