package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.Log
import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.Dimension
import com.lightningkite.kiteui.models.Edges
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


public object ScrollLayoutMeta {
    public val unboundSize: Double = 10_000.0
}


public class ScrollLayout : UIScrollView(CGRectZero.readValue()), UIViewWithSizeOverridesProtocol {
    public var horizontal: Boolean = true

    override fun forceRemeasures() {
        setNeedsLayout()
        informParentOfSizeChangeDueToChild()
    }

    override fun subviewDidChangeSizing(view: UIView?) {
        setNeedsLayout()
        informParentOfSizeChangeDueToChild()
    }

    public var onSizeChange: () -> Unit = {}

    public data class Size(var primary: Double = 0.0, var secondary: Double = 0.0) {
    }

    public val Size.objc: CValue<CGSize> get() = CGSizeMake(if (horizontal) primary else secondary, if (horizontal) secondary else primary)
    public val CGSize.local: Size get() = Size(if (horizontal) width else height, if (horizontal) height else width)
    public val CValue<CGSize>.local: Size get() = useContents { local }
    public val SizeConstraints.primaryMax: Dimension? get() = if (horizontal) maxWidth else maxHeight
    public val SizeConstraints.secondaryMax: Dimension? get() = if (horizontal) maxHeight else maxWidth
    public val SizeConstraints.primaryMin: Dimension? get() = if (horizontal) minWidth else minHeight
    public val SizeConstraints.secondaryMin: Dimension? get() = if (horizontal) minHeight else minWidth
    public val SizeConstraints.primary: Dimension? get() = if (horizontal) width else height
    public val SizeConstraints.secondary: Dimension? get() = if (horizontal) height else width
    public val UIView.secondaryAlign: Align? get() = if (horizontal) extensionVerticalAlign else extensionHorizontalAlign
    public val Edges.primarySum: Double get() = if (horizontal) horizontalSum.value else verticalSum.value
    public val Edges.secondarySum: Double get() = if (!horizontal) horizontalSum.value else verticalSum.value
    public val Edges.primaryStart: Double get() = if (horizontal) left.value else top.value
    public val Edges.primaryEnd: Double get() = if (horizontal) right.value else bottom.value
    public val Edges.secondaryStart: Double get() = if (!horizontal) left.value else top.value
    public val Edges.secondaryEnd: Double get() = if (!horizontal) right.value else bottom.value

    public val mainSubview: UIView? get() = subviews.filterIsInstance<UIView>().firstOrNull { !it.hidden && it !is UIRefreshControl }

    override fun sizeThatFits(size: CValue<CGSize>): CValue<CGSize> {
        val mySizeWithoutPadding = bounds.useContents { size.local }
        val padding = (extensionPadding ?: Edges.ZERO).plus(extensionSafeInsetPadding ?: Edges.ZERO)
        mySizeWithoutPadding.primary -= padding.primarySum
        mySizeWithoutPadding.secondary -= padding.secondarySum

        val subsize = calcSizes(mySizeWithoutPadding, true)

        debugPrint { "Total sizeThatFits $subsize" }

        // subsize represents the size of the ScrollView content as returned by calcSizes(); the dimensions of the
        // ScrollView itself should never be bigger than the size passed to sizeThatFits() or the ScrollView will never
        // scroll
        subsize.primary = subsize.primary.coerceAtMost(mySizeWithoutPadding.primary)

        subsize.primary += padding.primarySum + 0.00001
        subsize.secondary += padding.secondarySum + 0.00001

        return subsize.objc
    }

    public fun calcSizes(sizeWithoutPadding: Size, unbound: Boolean): Size {
        val remaining = sizeWithoutPadding.copy()

        return mainSubview?.let {
            val remainingPrimary = if (unbound) ScrollLayoutMeta.unboundSize else remaining.primary
            val sizeInput = Size(remainingPrimary, remaining.secondary)
            val required = it.sizeThatFits2(
                sizeInput.objc,
//                null,
                it.extensionSizeConstraints,
            ).local
            debugPrint { "Scroll child measured with $sizeInput, got $required" }
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
            debugPrint { "Scroll child result is $required" }
            required
        } ?: sizeWithoutPadding
    }

    private var lastReportedSize: Size? = null
    private var lastContentWidth: Double = -1.0
    private var lastContentHeight: Double = -1.0
    override fun layoutSubviews() {
        val mySizeWithoutPadding = bounds.useContents { size.local }
        val padding = (extensionPadding ?: Edges.ZERO).plus(extensionSafeInsetPadding ?: Edges.ZERO)
        mySizeWithoutPadding.primary -= padding.primarySum
        mySizeWithoutPadding.secondary -= padding.secondarySum
        (subviews.firstOrNull() as? UIView)?.debugPrint { "Parent ScrollLayout Laying out within $mySizeWithoutPadding" }
        debugPrint { "Laying out within $mySizeWithoutPadding" }
        var primary = padding.primaryStart
        val view = mainSubview ?: run {
            Log.warn("ScrollLayout without children???")
            return
        }
        var size = calcSizes(mySizeWithoutPadding, true)
        debugPrint { "Initial scroll size calc: $size" }
        if (size.primary >= 9999.0) {
            size = calcSizes(mySizeWithoutPadding, false)
            size.primary = size.primary.coerceAtLeast(mySizeWithoutPadding.primary)
            debugPrint { "Constrained scroll size calc: $size" }
        }
        val ps = primary
        val a = view.secondaryAlign ?: Align.Stretch
        val offset = when (a) {
            Align.Start -> padding.secondaryStart
            Align.Stretch -> padding.secondaryStart
            Align.End -> padding.secondaryStart + mySizeWithoutPadding.secondary - size.secondary
            Align.Center -> padding.secondaryStart + (mySizeWithoutPadding.secondary - size.secondary) / 2
        }
        val secondarySize = (if (a == Align.Stretch) mySizeWithoutPadding.secondary else size.secondary.coerceAtMost(
            mySizeWithoutPadding.secondary - padding.secondarySum
        ))
        val widthSize = if (horizontal) size.primary else secondarySize
        val heightSize = if (horizontal) secondarySize else size.primary
        primary += size.primary
        primary += padding.primaryEnd
        // Set this first so that layoutSubviews has the new content size
        val newContentWidth = if (horizontal) primary else 0.0
        val newContentHeight = if (!horizontal) primary else 0.0
        if (newContentWidth != lastContentWidth || newContentHeight != lastContentHeight) {
            lastContentWidth = newContentWidth
            lastContentHeight = newContentHeight
            setContentSize(CGSizeMake(newContentWidth, newContentHeight))
        }
        (subviews.firstOrNull() as? UIView)?.debugPrint { "Parent ScrollLayout setting psuedoframe of child" }
        view.setPsuedoframe(
            if (horizontal) ps else offset,
            if (horizontal) offset else ps,
            widthSize,
            heightSize,
        )
        val oldSize = view.bounds.useContents { this.size.width to this.size.height }
        if (oldSize.first != widthSize || oldSize.second != heightSize || view.explicitlyNeedsLayout != false) {
            view.explicitlyNeedsLayout = false
            (subviews.firstOrNull() as? UIView)?.debugPrint { "Parent ScrollLayout child layoutSubviewsAndLayers" }
            view.layoutSubviewsAndLayers()
        }
        if (lastReportedSize != mySizeWithoutPadding) {
            onSizeChange()
            lastReportedSize = mySizeWithoutPadding
        }
    }

    override fun hitTest(point: CValue<CGPoint>, withEvent: UIEvent?): UIView? {
        return frameLayoutHitTest(point, withEvent)
    }
}