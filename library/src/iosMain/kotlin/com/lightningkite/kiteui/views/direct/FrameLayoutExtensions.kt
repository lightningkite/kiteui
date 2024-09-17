package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.*
import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.PopoverPreferredDirection
import com.lightningkite.kiteui.utils.*
import com.lightningkite.kiteui.views.*
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGPoint
import platform.CoreGraphics.CGSize
import platform.CoreGraphics.CGSizeMake
import platform.UIKit.UICoordinateSpaceProtocol
import platform.UIKit.UIEvent
import platform.UIKit.UIView
import kotlin.math.max


@OptIn(ExperimentalForeignApi::class)
fun UIView.frameLayoutLayoutSubviews(childSizeCache: ArrayList<HashMap<Size, Size>>): Unit {
    val mySize = bounds.useContents { size.local }
    if(viewDebugTarget?.native == this) println("frameLayoutLayoutSubviews ${mySize}")
    var padding = extensionPadding ?: 0.0
    subviews.zip(frameLayoutCalcSizes(frame.useContents { size.local }, childSizeCache)) { view, size ->
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
        val heightSize = if (v == Align.Stretch) mySize.height - padding * 2 else size.height
        val oldSize = view.bounds.useContents { this.size.width to this.size.height }

        run {
            if(viewDebugTarget?.native == this) {
                println("Don't animate the change")
            }
            view.setPsuedoframe(
                offsetH,
                offsetV,
                widthSize,
                heightSize,
            )
            if (oldSize.first != widthSize || oldSize.second != heightSize) {
                view.layoutSubviewsAndLayers()
            }
        }
        Unit
    }
}

@OptIn(ExperimentalForeignApi::class)
fun UIView.frameLayoutLayoutAnchoredSubviews(childSizeCache: ArrayList<HashMap<Size, Size>>, anchor: Pair<PopoverPreferredDirection, UIView>) {
    val frameLayout = this
    subviews.zip(frameLayoutCalcSizes(frame.useContents { size.local }, childSizeCache)) { view, size ->
        view as UIView
        if (view.hidden) return@zip
        val anchorPositionInFrameLayout = with(anchor.second) { convertRect(bounds, toView = frameLayout) }.local
        val (offsetH, offsetV) = anchor.first.calculatePopoverPosition(
            anchorPositionInFrameLayout,
            view.bounds.local,
            frameLayout.bounds.local
        )
        val oldSize = view.bounds.useContents { this.size.width to this.size.height }

        run {
            view.setPsuedoframe(
                offsetH,
                offsetV,
                size.width,
                size.height,
            )
            if (oldSize.first != size.width || oldSize.second != size.height) {
                view.layoutSubviewsAndLayers()
            }
        }
        Unit
    }
}

@OptIn(ExperimentalForeignApi::class)
fun UIView.frameLayoutHitTest(point: CValue<CGPoint>, withEvent: UIEvent?): UIView? {
    if (hidden) return null
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
            if (it.hidden) return@forEach
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
fun UIView.frameLayoutSizeThatFits(
    size: CValue<CGSize>,
    childSizeCache: ArrayList<HashMap<Size, Size>>
): CValue<CGSize> {
    val inputSize = size.local
    val measuredSize = Size()

    val sizes = frameLayoutCalcSizes(inputSize, childSizeCache)
    val padding = extensionPadding ?: 0.0
    for ((index, size) in sizes.withIndex()) {
        measuredSize.width = max(measuredSize.width, size.width + padding * 2)
        measuredSize.height = max(measuredSize.height, size.height + padding * 2)
        if(viewDebugTarget?.native == this) println("frameLayoutSizeThatFits[$index] ${size} -> ${measuredSize}")
    }

    if(viewDebugTarget?.native == this) println("frameLayoutSizeThatFits ${inputSize} -> ${measuredSize}")
    return measuredSize.objc
}

@OptIn(ExperimentalForeignApi::class)
private fun UIView.frameLayoutCalcSizes(size: Size, childSizeCache: ArrayList<HashMap<Size, Size>>): List<Size> {
    var t = PerformanceInfo.trace("calcSizeFrame")
    val padding = extensionPadding ?: 0.0
    val remaining = size.copy(width = size.width - 2 * padding, height = size.height - 2 * padding)

    return subviews.mapIndexed { index: Int, it: Any? ->
        it as UIView
        if (it.hidden) return@mapIndexed Size()
        val measureInput = remaining.copy(width = remaining.width, height = remaining.height)
        t.pause()
        val required = childSizeCache[index].getOrPut(measureInput) {
            it.sizeThatFits2(
                measureInput.objc,
                it.extensionSizeConstraints
            ).local
        }
        if(viewDebugTarget?.native == this) println("frameLayoutCalcSizes child[$index] ${size} -> ${required}")
        t.resume()
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
    }.also { t.cancel() }
}

fun UIView.frameLayoutSubviewDidChangeSizing(child: UIView?, childSizeCache: ArrayList<HashMap<Size, Size>>) {
    val it = child ?: return
    val index = subviews.indexOf(child)
    if (index != -1) childSizeCache[index].clear()
    informParentOfSizeChangeDueToChild()
}

fun UIView.frameLayoutDidAddSubview(subview: UIView, childSizeCache: ArrayList<HashMap<Size, Size>>) {
    val index = subviews.indexOf(subview).also { if (it == -1) throw Exception() }
    childSizeCache.add(index, HashMap())
}

fun UIView.frameLayoutWillRemoveSubview(subview: UIView, childSizeCache: ArrayList<HashMap<Size, Size>>) {
    val index = subviews.indexOf(subview).also { if (it == -1) throw Exception() }
    childSizeCache.removeAt(index)
}

data class Size(var width: Double = 0.0, var height: Double = 0.0) {
}

@OptIn(ExperimentalForeignApi::class)
val Size.objc get() = CGSizeMake(width, height)
val CGSize.local get() = Size(width, height)

@OptIn(ExperimentalForeignApi::class)
val CValue<CGSize>.local get() = useContents { local }