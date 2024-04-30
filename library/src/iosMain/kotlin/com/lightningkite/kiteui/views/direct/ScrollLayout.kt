@file:OptIn(ExperimentalForeignApi::class)

package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.SizeConstraints
import com.lightningkite.kiteui.objc.UIViewWithSizeOverridesProtocol
import com.lightningkite.kiteui.views.*
import kotlinx.cinterop.*
import platform.CoreGraphics.*
import platform.UIKit.*
import kotlin.math.max

//private val UIViewLayoutParams = ExtensionProperty<UIView, LayoutParams>()
//val UIView.layoutParams: LayoutParams by UIViewLayoutParams
//
//class LayoutParams()

@OptIn(ExperimentalForeignApi::class)
class ScrollLayout : UIScrollView(CGRectZero.readValue()), UIViewWithSizeOverridesProtocol {
    var horizontal: Boolean = true
    var padding: Double
        get() = extensionPadding ?: 0.0
        set(value) {
            extensionPadding = value
        }

    override fun subviewDidChangeSizing(view: UIView?) {
        setNeedsLayout()
    }

    data class Size(var primary: Double = 0.0, var secondary: Double = 0.0) {
    }

    val Size.objc get() = CGSizeMake(if (horizontal) primary else secondary, if (horizontal) secondary else primary)
    val CGSize.local get() = Size(if (horizontal) width else height, if (horizontal) height else width)
    val CValue<CGSize>.local get() = useContents { local }
    val SizeConstraints.primaryMax get() = if (horizontal) maxWidth else maxHeight
    val SizeConstraints.secondaryMax get() = if (horizontal) maxHeight else maxWidth
    val SizeConstraints.primaryMin get() = if (horizontal) minWidth else minHeight
    val SizeConstraints.secondaryMin get() = if (horizontal) minHeight else minWidth
    val SizeConstraints.primary get() = if (horizontal) width else height
    val SizeConstraints.secondary get() = if (horizontal) height else width
    val UIView.secondaryAlign get() = if (horizontal) extensionVerticalAlign else extensionHorizontalAlign

    val mainSubview get() = subviews.filterIsInstance<UIView>().firstOrNull { !it.hidden }

    override fun sizeThatFits(size: CValue<CGSize>): CValue<CGSize> {
        val size = size.local
        val measuredSize = Size()

        val subsize = calcSizes(size, true)
        measuredSize.primary += padding
        measuredSize.primary += subsize.primary
        measuredSize.secondary = max(measuredSize.secondary, subsize.secondary + padding * 2)
        measuredSize.primary += padding

        return measuredSize.objc
    }

    fun calcSizes(size: Size, unbound: Boolean): Size {
        val remaining = size.copy()
        remaining.primary -= padding * 2
        remaining.secondary -= padding * 2

        var totalWeight = 0f
        return mainSubview?.let {
            val remainingPrimary = if (unbound) 10000.0 else remaining.primary
            val required = it.sizeThatFits2(
                CGSizeMake(
                    if (horizontal) remainingPrimary else remaining.secondary,
                    if (horizontal) remaining.secondary else remainingPrimary
                ),
//                null,
                it.extensionSizeConstraints,
            ).local
            it.extensionSizeConstraints?.let {
                it.primaryMax?.let { required.primary = required.primary.coerceAtMost(it.value) }
                it.secondaryMax?.let { required.secondary = required.secondary.coerceAtMost(it.value) }
                it.primaryMin?.let { required.primary = required.primary.coerceAtLeast(it.value) }
                it.secondaryMin?.let { required.secondary = required.secondary.coerceAtLeast(it.value) }
                it.primary?.let { required.primary = it.value }
                it.secondary?.let { required.secondary = it.value }
            }
            required.primary = required.primary.coerceAtLeast(0.0)
            required.secondary = required.secondary.coerceAtLeast(0.0)

            remaining.secondary = remaining.secondary.coerceAtLeast(required.secondary)
            it.extensionWeight?.let { w ->
                totalWeight += w
                required.primary = (-w).toDouble()
            } ?: run {
                remaining.primary -= required.primary
            }
            required
        } ?: size
    }

    override fun layoutSubviews() {
        val mySize = bounds.useContents { size.local }
        var primary = padding
        val view = mainSubview ?: run {
            return
        }
        var size = calcSizes(frame.useContents { this.size.local }, true)
        if (size.primary >= 9999.0) {
            size = calcSizes(frame.useContents { this.size.local }, false)
        }
        val ps = primary
        val a = view.secondaryAlign ?: Align.Stretch
        val offset = when (a) {
            Align.Start -> padding
            Align.Stretch -> padding
            Align.End -> mySize.secondary - padding - size.secondary
            Align.Center -> (mySize.secondary - size.secondary) / 2
        }
        val secondarySize = if (a == Align.Stretch) mySize.secondary - padding * 2 else size.secondary
        val oldSize = view.bounds.useContents { this.size.width to this.size.height }
        val widthSize = if (horizontal) size.primary else secondarySize
        val heightSize = if (horizontal) secondarySize else size.primary
        view.setPsuedoframe(
            if (horizontal) ps else offset,
            if (horizontal) offset else ps,
            widthSize,
            heightSize,
        )
        if (oldSize.first != widthSize || oldSize.second != heightSize) {
            view.layoutSubviewsAndLayers()
        }
        primary += size.primary
        primary += padding
        setContentSize(
            CGSizeMake(
                if (horizontal) primary else 0.0,
                if (!horizontal) primary else 0.0,
            )
        )
    }
}