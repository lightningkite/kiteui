package com.lightningkite.rock.views.direct

import com.lightningkite.rock.models.Align
import com.lightningkite.rock.views.*
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGPoint
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSize
import platform.CoreGraphics.CGSizeMake
import platform.UIKit.UICoordinateSpaceProtocol
import platform.UIKit.UIEvent
import platform.UIKit.UIView
import kotlin.math.max


@OptIn(ExperimentalForeignApi::class)
fun UIView.frameLayoutLayoutSubviews(sizeCache: MutableMap<Size, List<Size>>) {
    val mySize = bounds.useContents { size.local }
    var padding = extensionPadding ?: 0.0
    subviews.zip(frameLayoutCalcSizes(frame.useContents { size.local }, sizeCache)) { view, size ->
        view as UIView
        if (view.hidden) return@zip
        val h = view.extensionHorizontalAlign ?: Align.Stretch
        val v = view.extensionVerticalAlign ?: Align.Stretch
        val offsetH = when (h) {
            Align.Start -> padding
            Align.Stretch -> padding
            Align.End -> mySize.width - padding - size.width
            Align.Center -> (mySize.width - size.width) / 2
        }
        val offsetV = when (v) {
            Align.Start -> padding
            Align.Stretch -> padding
            Align.End -> mySize.height - padding - size.height
            Align.Center -> (mySize.height - size.height) / 2
        }
        val widthSize = if (h == Align.Stretch) mySize.width - padding * 2 else size.width
        val heightSize =
            if (v == Align.Stretch) mySize.height - padding * 2 else size.height
        val oldSize = view.bounds.useContents { this.size.width to this.size.height }
        view.setFrame(
            CGRectMake(
                offsetH,
                offsetV,
                widthSize,
                heightSize,
            )
        )
        if (oldSize.first != widthSize || oldSize.second != heightSize) {
            view.layoutSubviews()
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
fun UIView.frameLayoutHitTest(point: CValue<CGPoint>, withEvent: UIEvent?): UIView? {
    if(hidden) return null
    if (bounds.useContents {
            val rect = this
            point.useContents {
                val point = this
                point.x >= rect.origin.x &&
                        point.y >= rect.origin.y &&
                        point.x <= rect.origin.x + rect.size.width &&
                        point.y <= rect.origin.y + rect.size.height
            }
        }) {
        subviews.asReversed().forEach {
            it as UIView
            if(it.hidden) return@forEach
            it.hitTest(
                it.convertPoint(point = point, fromCoordinateSpace = this as UICoordinateSpaceProtocol),
                withEvent
            )
                ?.let { return it }
        }
        return this
    } else {
        return null
    }
}

@OptIn(ExperimentalForeignApi::class)
fun UIView.frameLayoutSizeThatFits(size: CValue<CGSize>, sizeCache: MutableMap<Size, List<Size>>): CValue<CGSize> {
    val size = size.local
    val measuredSize = Size()

    val sizes = frameLayoutCalcSizes(size, sizeCache)
    val padding = extensionPadding ?: 0.0
    for (size in sizes) {
        measuredSize.width = max(measuredSize.width, size.width + padding * 2)
        measuredSize.height = max(measuredSize.height, size.height + padding * 2)
    }

    return measuredSize.objc
}

@OptIn(ExperimentalForeignApi::class)
private fun UIView.frameLayoutCalcSizes(size: Size, sizeCache: MutableMap<Size, List<Size>>): List<Size> =
    sizeCache.getOrPut(size) {
        val padding = extensionPadding ?: 0.0
//        let size = padding.shrinkSize(size)
//    val remaining = size.copy()
        val remaining = size.copy(width = size.width - 2 * padding, height = size.height - 2 * padding)

        return subviews.map {
            it as UIView
            if (it.hidden) return@map Size(0.0, 0.0)
//        val required = it.sizeThatFits2(remaining.objc, it.extensionSizeConstraints).local
//         TODO: This line is more accurate
            val required = it.sizeThatFits2(
                remaining.copy(width = remaining.width, height = remaining.height).objc,
                it.extensionSizeConstraints
            ).local
            it.extensionSizeConstraints?.let {
                it.maxWidth?.let { required.width = required.width.coerceAtMost(it.value) }
                it.maxHeight?.let { required.height = required.height.coerceAtMost(it.value) }
                it.minWidth?.let { required.width = required.width.coerceAtLeast(it.value) }
                it.minHeight?.let { required.height = required.height.coerceAtLeast(it.value) }
                it.width?.let { required.width = it.value.coerceAtMost(remaining.width) }
                it.height?.let { required.height = it.value.coerceAtMost(remaining.height) }
            }
            required.width = required.width.coerceAtLeast(0.0)//.coerceAtMost(size.width - 2 * m)
            required.height = required.height.coerceAtLeast(0.0)//.coerceAtMost(size.height - 2 * m)

            remaining.width = remaining.width.coerceAtLeast(required.width)
            remaining.height = remaining.height.coerceAtLeast(required.height)
            required
        }
    }

fun UIView.frameLayoutSubviewDidChangeSizing(child: UIView?, sizeCache: MutableMap<Size, List<Size>>) {
    val it = child ?: return
    sizeCache.clear()
//    if(it.hidden) return
//    if(
//        it.extensionHorizontalAlign.let { it == null || it == Align.Stretch} &&
//        it.extensionVerticalAlign.let { it == null || it == Align.Stretch}
//    ) {
//        return
//    }
//    it.extensionSizeConstraints?.takeIf { it.width != null && it.height != null }?.let {
//        return
//    }
    informParentOfSizeChange()
}

data class Size(var width: Double = 0.0, var height: Double = 0.0) {
}

@OptIn(ExperimentalForeignApi::class)
val Size.objc get() = CGSizeMake(width, height)
val CGSize.local get() = Size(width, height)

@OptIn(ExperimentalForeignApi::class)
val CValue<CGSize>.local get() = useContents { local }