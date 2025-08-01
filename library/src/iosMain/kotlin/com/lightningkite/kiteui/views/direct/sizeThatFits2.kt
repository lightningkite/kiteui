package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.InternalKiteUi
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
import com.lightningkite.kiteui.views.debugPrint


@InternalKiteUi
public fun UIView.sizeThatFits2(
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
        it.width?.let { w = it.value.coerceAtMost(w) }
        it.height?.let { h = it.value.coerceAtMost(h) }
        CGSizeMake(w, h)
    } ?: size
    val measured = when (this) {
        is LinearLayout,
        is FrameLayout,
        is FrameLayoutButton,
            -> sizeThatFits(newSizeInput)

        is CanvasView -> PerformanceInfo["CanvasView.sizeThatFits"] { sizeThatFits(newSizeInput) }
        is NDismissBackground -> PerformanceInfo["NDismissBackground.sizeThatFits"] { sizeThatFits(newSizeInput) }
        is NIconView -> PerformanceInfo["NIconView.sizeThatFits"] { sizeThatFits(newSizeInput) }
        is NProgrammaticLayout -> PerformanceInfo["NProgrammaticLayout.sizeThatFits"] { sizeThatFits(newSizeInput) }
        is ResizeableProgressView -> PerformanceInfo["ResizeableProgressView.sizeThatFits"] { sizeThatFits(newSizeInput) }
        is UIImageViewFixedSizing -> PerformanceInfo["UIImageViewFixedSizing.sizeThatFits"] { sizeThatFits(newSizeInput) }
        is ScrollLayout -> PerformanceInfo["ScrollLayout.sizeThatFits"] { sizeThatFits(newSizeInput) }
        is NSpace -> PerformanceInfo["NSpace.sizeThatFits"] { sizeThatFits(newSizeInput) }
        is UILabelWithGradient -> PerformanceInfo["UILabelWithGradient.sizeThatFits"] { sizeThatFits(newSizeInput) }
        is UILabelWithLayerBackground -> PerformanceInfo["UILabelWithLayerBackground.sizeThatFits"] { sizeThatFits(newSizeInput) }
        is WrapperView -> PerformanceInfo["WrapperView.sizeThatFits"] { sizeThatFits(newSizeInput) }

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
    //val result = measured
    val result = sizeConstraints?.let {
        var w = measured.useContents { width }
        var h = measured.useContents { height }
        it.aspectRatio?.let { aspectRatio ->
            if (w / h > aspectRatio) {
                w = h * aspectRatio
            } else {
                h = w / aspectRatio
            }
        }
        CGSizeMake(w, h)
    } ?: measured
    debugPrint {
        buildString {
            appendLine("viewDebugTarget constraints: $sizeConstraints")
            appendLine("viewDebugTarget size: ${size.useContents { "$width, $height" }}")
            appendLine("viewDebugTarget newSizeInput: ${newSizeInput.useContents { "$width, $height" }}")
            appendLine("viewDebugTarget measured: ${measured.useContents { "$width, $height" }}")
            appendLine("viewDebugTarget result: ${result.useContents { "$width, $height" }}")
        }
    }
    return result
}