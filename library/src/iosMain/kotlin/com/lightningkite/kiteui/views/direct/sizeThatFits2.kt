package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.SizeConstraints
import com.lightningkite.kiteui.utils.fitInsideBox
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGSize
import platform.CoreGraphics.CGSizeMake
import platform.UIKit.UIView

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
    return newSize
}