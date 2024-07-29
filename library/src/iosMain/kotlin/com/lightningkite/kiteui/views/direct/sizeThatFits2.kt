package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.PerformanceInfo
import com.lightningkite.kiteui.models.SizeConstraints
import com.lightningkite.kiteui.utils.fitInsideBox
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGSize
import platform.CoreGraphics.CGSizeMake
import platform.UIKit.UIImageView
import platform.UIKit.UIScreen
import platform.UIKit.UIView

@OptIn(ExperimentalForeignApi::class)
fun UIView.sizeThatFits2(
    size: CValue<CGSize>,
    sizeConstraints: SizeConstraints?,
    debug: Boolean = false
): CValue<CGSize> {
    val newSize = sizeConstraints?.let {
        var w = size.useContents { width }
        var h = size.useContents { height }
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
    } ?: size
    return when (this) {
        is LinearLayout,
        is FrameLayout,
        is FrameLayoutButton -> sizeThatFits(newSize)

        else -> PerformanceInfo["nativeSizeThatFits"]{ sizeThatFits(newSize) }
    }
}