package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.*
import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.Edges
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



fun UIView.frameLayoutLayoutSubviews(childSizeCache: ArrayList<HashMap<Size, Size>>): Unit {
    val mySize = bounds.useContents { size.local }
    debugPrint { "frameLayoutLayoutSubviews ${mySize}" }
    val padding = (extensionPadding ?: Edges.ZERO).plus(extensionSafeInsetPadding ?: Edges.ZERO)
    subviews.zip(frameLayoutCalcSizes(frame.useContents { size.local }, childSizeCache)) { view, size ->
        view as UIView
        if (view.hidden || view.extensionCollapsed == true) return@zip
        val h = view.extensionHorizontalAlign ?: Align.Stretch
        val v = view.extensionVerticalAlign ?: Align.Stretch
        val offsetH = when (h) {
            Align.Start -> padding.left.value
            Align.Stretch -> padding.left.value
            Align.End -> mySize.width - padding.right.value - size.width
            Align.Center -> (mySize.width - size.width - padding.horizontalSum.value) / 2 + padding.left.value
        }
        val offsetV = when (v) {
            Align.Start -> padding.top.value
            Align.Stretch -> padding.top.value
            Align.End -> mySize.height - padding.bottom.value - size.height
            Align.Center -> (mySize.height - size.height - padding.verticalSum.value) / 2 + padding.top.value
        }
        val widthSize = if (h == Align.Stretch) mySize.width - padding.horizontalSum.value else size.width
        val heightSize = if (v == Align.Stretch) mySize.height - padding.verticalSum.value else size.height
        val oldSize = view.bounds.useContents { this.size.width to this.size.height }

        run {
            debugPrint { "Don't animate the change" }
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
        if (view.hidden || view.extensionCollapsed == true) return@zip
        val anchorPositionInFrameLayout = with(anchor.second) { convertRect(bounds, toView = frameLayout) }.local
        val (offsetH, offsetV) = anchor.first.calculatePopoverOffset(
            anchorPositionInFrameLayout,
            view.bounds.local,
            frameLayout.bounds.local
        )

        run {
            view.setPsuedoframe(
                offsetH,
                offsetV,
                size.width,
                size.height,
            )
//            val oldSize = view.bounds.useContents { this.size.width to this.size.height }
//            if (oldSize.first != size.width || oldSize.second != size.height || view.explicitlyNeedsLayout != false) {
                view.explicitlyNeedsLayout = false
                view.layoutSubviewsAndLayers()
//            }
        }
        Unit
    }
}

private fun UIView.toShortString() = this.toString().substringBefore(';').substringAfter('<')
fun UIView.frameLayoutHitTest(point: CValue<CGPoint>, withEvent: UIEvent?): UIView? {
    if (hidden) return null
    if (extensionCollapsed == true) return null
    if (!pointInside(point, withEvent)) return null
    for (it in subviews.asReversed()) {
        it as UIView
//        println("${this.toShortString()}.frameLayoutHitTest -> ${it.toShortString()}")
        if (it.hidden) {
//            println("${this.toShortString()}.frameLayoutHitTest -> ${it.toShortString()} it.hidden")
            continue
        }
        if (it.extensionCollapsed == true) {
//            println("${this.toShortString()}.frameLayoutHitTest -> ${it.toShortString()} it.extensionCollapsed == true")
            continue
        }
        if (it.alpha < 0.001) {
//            println("${this.toShortString()}.frameLayoutHitTest -> ${it.toShortString()} it.alpha < 0.001")
            continue
        }

        val converted = it.convertPoint(point = point, fromCoordinateSpace = this as UICoordinateSpaceProtocol)
        if (!it.pointInside(converted, withEvent)) {
//            println("${this.toShortString()}.frameLayoutHitTest -> ${it.toShortString()} !it.pointInside(converted, withEvent)")
            continue
        }
        // OK, the point is inside.  We're either going to grant it the touch or return nothing for the touch.
        val hitResult = it.hitTest(
            it.convertPoint(point = point, fromCoordinateSpace = this as UICoordinateSpaceProtocol),
            withEvent
        )
        return when {
            hitResult != null -> {
//                println("${this.toShortString()}.frameLayoutHitTest -> ${it.toShortString()} hitResult != null (hitResult: $hitResult)")
                hitResult
            }
            it.extensionIgnoreInteraction == true -> {
//                println("${this.toShortString()}.frameLayoutHitTest -> ${it.toShortString()} it.extensionIgnoreInteraction == true")
                continue
            }
            userInteractionEnabled -> {
//                println("${this.toShortString()}.frameLayoutHitTest -> ${it.toShortString()} userInteractionEnabled")
                this
            }
            extensionIgnoreInteraction == true -> {
//                println("${this.toShortString()}.frameLayoutHitTest -> ${it.toShortString()} extensionIgnoreInteraction")
                it
            }
            else -> {
//                println("${this.toShortString()}.frameLayoutHitTest -> ${it.toShortString()} else")
                null
            }
        }
    }
//    println("$this give up: $userInteractionEnabled")
    return if (userInteractionEnabled && extensionIgnoreInteraction != true) this else null
}


fun UIView.frameLayoutSizeThatFits(
    size: CValue<CGSize>,
    childSizeCache: ArrayList<HashMap<Size, Size>>
): CValue<CGSize> {
    val inputSize = size.local
    val measuredSize = Size()

    val sizes = frameLayoutCalcSizes(inputSize, childSizeCache)
    val padding = (extensionPadding ?: Edges.ZERO).plus(extensionSafeInsetPadding ?: Edges.ZERO)
    for ((index, size) in sizes.withIndex()) {
        measuredSize.width = max(measuredSize.width, size.width + padding.horizontalSum.value)
        measuredSize.height = max(measuredSize.height, size.height + padding.verticalSum.value)
        debugPrint { "frameLayoutSizeThatFits[$index] ${size} -> ${measuredSize}" }
    }

    debugPrint { "frameLayoutSizeThatFits ${inputSize} -> ${measuredSize}" }
    return measuredSize.objc
}


private fun UIView.frameLayoutCalcSizes(size: Size, childSizeCache: ArrayList<HashMap<Size, Size>>): List<Size> {
    var t = PerformanceInfo.trace("calcSizeFrame")
    val padding = (extensionPadding ?: Edges.ZERO).plus(extensionSafeInsetPadding ?: Edges.ZERO)
    val remaining = size.copy(width = size.width - padding.horizontalSum.value, height = size.height - padding.verticalSum.value)

    return subviews.mapIndexed { index: Int, it: Any? ->
        it as UIView
        if (it.hidden || it.extensionCollapsed == true) return@mapIndexed Size()
        val measureInput = remaining.copy(width = remaining.width, height = remaining.height)
        t.pause()
        val required = childSizeCache[index].getOrPut(measureInput) {
            it.sizeThatFits2(
                measureInput.objc,
                it.extensionSizeConstraints
            ).local
        }
        this.debugPrint { "frameLayoutCalcSizes child[$index] ${size} -> ${required}" }
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


val Size.objc get() = CGSizeMake(width, height)
val CGSize.local get() = Size(width, height)


val CValue<CGSize>.local get() = useContents { local }