

package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.SizeConstraints
import com.lightningkite.kiteui.objc.UIViewWithSizeOverridesProtocol
import com.lightningkite.kiteui.viewDebugTarget
import com.lightningkite.kiteui.views.*
import kotlinx.cinterop.*
import platform.CoreGraphics.*
import platform.UIKit.*

//private val UIViewLayoutParams = ExtensionProperty<UIView, LayoutParams>()
//val UIView.layoutParams: LayoutParams by UIViewLayoutParams
//
//class LayoutParams()


object ScrollLayoutMeta {
    val unboundSize = 10_000.0
}


class ScrollLayout : UIScrollView(CGRectZero.readValue()), UIViewWithSizeOverridesProtocol {
    var horizontal: Boolean = true
    var padding: Double
        get() = extensionPadding ?: 0.0
        set(value) {
            extensionPadding = value
        }

    override fun subviewDidChangeSizing(view: UIView?) {
        setNeedsLayout()
        informParentOfSizeChangeDueToChild()
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
        val mySizeWithoutPadding = bounds.useContents { size.local }
        mySizeWithoutPadding.primary -= padding * 2
        mySizeWithoutPadding.secondary -= padding * 2

        val subsize = calcSizes(mySizeWithoutPadding, true)

        if (viewDebugTarget?.native === this) println("Total sizeThatFits $subsize")

        subsize.primary += padding * 2 + 0.00001
        subsize.secondary += padding * 2 + 0.00001

        return subsize.objc
    }

    fun calcSizes(sizeWithoutPadding: Size, unbound: Boolean): Size {
        val remaining = sizeWithoutPadding.copy()

        return mainSubview?.let {
            val remainingPrimary = if (unbound) ScrollLayoutMeta.unboundSize else remaining.primary
            val sizeInput = Size(remainingPrimary, remaining.secondary)
            val required = it.sizeThatFits2(
                sizeInput.objc,
//                null,
                it.extensionSizeConstraints,
            ).local
            if (viewDebugTarget?.native === this) println("Scroll child measured with $sizeInput, got $required")
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
            if (viewDebugTarget?.native === this) println("Scroll child result is $required")
            required
        } ?: sizeWithoutPadding
    }

    override fun layoutSubviews() {
        val mySizeWithoutPadding = bounds.useContents { size.local }
        mySizeWithoutPadding.primary -= padding * 2
        mySizeWithoutPadding.secondary -= padding * 2
        if (viewDebugTarget?.native === this) println("Laying out within $mySizeWithoutPadding")
        var primary = padding
        val view = mainSubview ?: run {
            return
        }
        var size = calcSizes(mySizeWithoutPadding, true)
        if (viewDebugTarget?.native === this) println("Initial scroll size calc: $size")
        if (size.primary >= 9999.0) {
            size = calcSizes(mySizeWithoutPadding, false)
            size.primary = size.primary.coerceAtLeast(mySizeWithoutPadding.primary)
            if (viewDebugTarget?.native === this) println("Constrained scroll size calc: $size")
        }
        val ps = primary
        val a = view.secondaryAlign ?: Align.Stretch
        val offset = when (a) {
            Align.Start -> padding
            Align.Stretch -> padding
            Align.End -> padding + mySizeWithoutPadding.secondary - size.secondary
            Align.Center -> padding + (mySizeWithoutPadding.secondary - size.secondary) / 2
        }
        val secondarySize = (if (a == Align.Stretch) mySizeWithoutPadding.secondary else size.secondary.coerceAtMost(mySizeWithoutPadding.secondary - padding * 2))
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