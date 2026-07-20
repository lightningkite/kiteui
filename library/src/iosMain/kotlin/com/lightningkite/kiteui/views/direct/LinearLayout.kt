

package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.*
import com.lightningkite.kiteui.debugPrint
import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.Dimension
import com.lightningkite.kiteui.models.Edges
import com.lightningkite.kiteui.models.SizeConstraints
import com.lightningkite.kiteui.objc.UIViewWithSizeOverridesProtocol
import com.lightningkite.kiteui.objc.UIViewWithSpacingRulesProtocol
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*
import kotlin.math.max
import kotlinx.cinterop.*
import platform.CoreGraphics.*
import platform.QuartzCore.CALayer
import platform.UIKit.*
import platform.darwin.NSInteger

//private val UIViewLayoutParams = ExtensionProperty<UIView, LayoutParams>()
//val UIView.layoutParams: LayoutParams by UIViewLayoutParams
//
//class LayoutParams()


public class LinearLayout : UIView(CGRectZero.readValue()), UIViewWithSizeOverridesProtocol, UIViewWithSpacingRulesProtocol {
    public var horizontal: Boolean = true
    public var gap: Double = 0.0
        set(value) {
            if (field == value) return
            field = value
            debugDescriptionInfo2 = "(gap=$field)"
            setNeedsLayout()
            informParentOfSizeChange()
        }
    public var ignoreWeights: Boolean = false
        set(value) {
            field = value
            setNeedsLayout()
            informParentOfSizeChange()
        }
    public val spacingOverride: Signal<Dimension?> = Signal<Dimension?>(null).also {
        it.addListener { it.value?.let { gap = it.value } }
    }

    override fun getSpacingOverrideProperty() = spacingOverride

//    init { setUserInteractionEnabled(false) }

    //    val debugLayer = CATextLayer().apply {
//        layer.addSublayer(this)
//        frame = CGRectMake(0.0, 0.0, 200.0, 20.0)
//        fontSize = 8.0
//        foregroundColor = UIColor.redColor.CGColor
//    }
    public var debugDescriptionInfo: String = ""
    public var debugDescriptionInfo2: String = ""
    override fun debugDescription(): String? =
        "${super.debugDescription()} $debugDescriptionInfo $debugDescriptionInfo2"

    override fun forceRemeasures() {
        lastLaidOutSize = null
        childSizeCache.forEach { it.clear() }
    }
    override fun subviewDidChangeSizing(view: UIView?) {
        val view = view ?: return
        val index = arrangedSubviews.indexOf(view)
        if (index != -1) childSizeCache[index].clear()
        else {
            Log.warn("WARN: Child $view not found inside $this")
        }
        lastLaidOutSize = null
        informParentOfSizeChangeDueToChild()
    }

    public data class Size(var primary: Double = 0.0, var secondary: Double = 0.0) {
    }

    public val Size.objc get() = CGSizeMake(if (horizontal) primary else secondary, if (horizontal) secondary else primary)
    public val CGSize.local get() = Size(if (horizontal) width else height, if (horizontal) height else width)
    public val CValue<CGSize>.local get() = useContents { local }
    public val SizeConstraints.primaryMax get() = if (horizontal) maxWidth else maxHeight
    public val SizeConstraints.secondaryMax get() = if (horizontal) maxHeight else maxWidth
    public val SizeConstraints.primaryMin get() = if (horizontal) minWidth else minHeight
    public val SizeConstraints.secondaryMin get() = if (horizontal) minHeight else minWidth
    public val SizeConstraints.primary get() = if (horizontal) width else height
    public val SizeConstraints.secondary get() = if (horizontal) height else width
    public val UIView.secondaryAlign get() = if (horizontal) extensionVerticalAlign else extensionHorizontalAlign
    public val Edges.primarySum get() = if(horizontal) horizontalSum.value else verticalSum.value
    public val Edges.secondarySum get() = if(!horizontal) horizontalSum.value else verticalSum.value
    public val Edges.primaryStart get() = if(horizontal) left.value else top.value
    public val Edges.primaryEnd get() = if(horizontal) right.value else bottom.value
    public val Edges.secondaryStart get() = if(!horizontal) left.value else top.value
    public val Edges.secondaryEnd get() = if(!horizontal) right.value else bottom.value

    override fun sizeThatFits(size: CValue<CGSize>): CValue<CGSize> {
        val sizeLocal = size.local
        val measuredSize = Size()

        val padding = (extensionPadding ?: Edges.ZERO).plus(extensionSafeInsetPadding ?: Edges.ZERO)
        val sizes = calcSizes(sizeLocal, sizeLocal.primary == ScrollLayoutMeta.unboundSize)
        measuredSize.primary += padding.primaryStart
        var first = true
        arrangedSubviews.zip(sizes) { view, size ->
            if (view.hidden || view.extensionCollapsed == true) return@zip
            if (first) {
                first = false
            } else {
                measuredSize.primary += view.extensionSpacingBeforeOverride?.value ?: gap
            }
            measuredSize.primary += size.primary
            measuredSize.secondary = max(measuredSize.secondary, size.secondary + padding.secondarySum)
            view.debugPrint {
                "size: $size\nmeasuredSize: $measuredSize"
            }
        }
        measuredSize.primary += padding.primaryEnd
        return measuredSize.objc
    }

    public val arrangedSubviews = ArrayList<UIView>()
    public fun addArrangedSubview(view: UIView) {
        childSizeCache.add(arrangedSubviews.size, HashMap())
        arrangedSubviews.add(view)
        addSubview(view)
        lastLaidOutSize = null
        informParentOfSizeChangeDueToChild()
    }
    public fun insertArrangedSubview(view: UIView, atIndex: NSInteger) {
        childSizeCache.add(atIndex.toInt(), HashMap())
        arrangedSubviews.add(atIndex.toInt(), view)
        insertSubview(view, atIndex)
        lastLaidOutSize = null
        informParentOfSizeChangeDueToChild()
    }

    override fun willRemoveSubview(subview: UIView) {
        // Fixes a really cursed crash where "this" is null due to GC interactions
        @Suppress("SENSELESS_COMPARISON")
        if (this != null) {
            lastLaidOutSize = null
            val index = arrangedSubviews.indexOf(subview)
            if(index != -1) {
                arrangedSubviews.removeAt(index)
                childSizeCache.removeAt(index)
                informParentOfSizeChangeDueToChild()
            }
        }
        super.willRemoveSubview(subview)
    }

    public val childSizeCache = ArrayList<HashMap<Size, Size>>()

    public fun calcSizes(size: Size, includeWeighted: Boolean): Array<Size> {
        var t = PerformanceInfo.trace("calcSizeLinear")
//        let size = padding.shrinkSize(size)
        val remaining = size.copy()
        val padding = (extensionPadding ?: Edges.ZERO).plus(extensionSafeInsetPadding ?: Edges.ZERO)
        remaining.primary -= padding.primarySum
        remaining.secondary -= padding.secondarySum

        var totalWeight = 0f

        val out = arrayOfNulls<Size?>(arrangedSubviews.size)

        var first = true
        arrangedSubviews.forEachIndexed { index, it ->
            if (it.hidden || it.extensionCollapsed == true) {
                out[index] = Size(0.0, 0.0)
                return@forEachIndexed
            }
            if (first) {
                first = false
            } else {
                remaining.primary -= it.extensionSpacingBeforeOverride?.value ?: gap
            }
            it.extensionWeight?.takeUnless { ignoreWeights }?.let {
                totalWeight += it
                return@forEachIndexed
            }
            val measureInput = Size(remaining.primary, remaining.secondary)
            t.pause()
            val required = childSizeCache[index].getOrPut(measureInput) {
                it.sizeThatFits2(
                    measureInput.objc,
                    it.extensionSizeConstraints
                ).local
            }
            debugPrint {
                ("Loaded size ${required} based on $measureInput")
            }
            t.resume()
            it.extensionSizeConstraints?.let {
                it.primaryMax?.let { required.primary = required.primary.coerceAtMost(it.value) }
                it.secondaryMax?.let { required.secondary = required.secondary.coerceAtMost(it.value) }
                it.primaryMin?.let { required.primary = required.primary.coerceAtLeast(it.value) }
                it.secondaryMin?.let { required.secondary = required.secondary.coerceAtLeast(it.value) }
                it.primary?.let { required.primary = it.value }
                it.secondary?.let { required.secondary = it.value.coerceAtMost(remaining.secondary) }
            }
            required.primary = required.primary.coerceAtLeast(0.0)
            required.secondary = required.secondary.coerceAtLeast(0.0)
            remaining.primary -= required.primary
            out[index] = required
        }

        arrangedSubviews.forEachIndexed { index, it ->
            if (out[index] != null) return@forEachIndexed
            if (it.hidden || it.extensionCollapsed == true) return@forEachIndexed
            val w = it.extensionWeight?.takeUnless { ignoreWeights }?.toDouble() ?: 1.0
            val available = ((w / totalWeight) * remaining.primary).coerceAtLeast(0.0)
            t.pause()
            val required =
                it.sizeThatFits2(Size(available, remaining.secondary).objc, it.extensionSizeConstraints).local
            t.resume()
//            val required = it.sizeThatFits2(Size(1000.0, remaining.secondary - m * 2).objc, it.extensionSizeConstraints).local
            it.extensionSizeConstraints?.let {
                it.secondaryMax?.let { required.secondary = required.secondary.coerceAtMost(it.value) }
                it.secondaryMin?.let { required.secondary = required.secondary.coerceAtLeast(it.value) }
                it.secondary?.let { required.secondary = it.value }
            }
//            required.primary = if(includeWeighted) available else 0.0
            required.primary = if (includeWeighted) available else required.primary
            required.secondary = required.secondary.coerceAtLeast(0.0)
            out[index] = required
        }
        debugPrint { "Sizes of children: ${out.indices.joinToString("\n") { "${subviews}" }}" }
        t.cancel()
        @Suppress("UNCHECKED_CAST")
        return out as Array<Size>
    }

    public var lastLaidOutSize: Size? = null
    public var lastLaidOutPadding: Edges? = null
    override fun layoutSubviews() {
        val padding = (extensionPadding ?: Edges.ZERO).plus(extensionSafeInsetPadding ?: Edges.ZERO)
        val mySize = bounds.useContents { size.local }
        if (lastLaidOutSize == mySize && lastLaidOutPadding == padding) return
        var t = PerformanceInfo.trace("layoutLinear")

        lastLaidOutSize = mySize
        lastLaidOutPadding = padding
        var primary = padding.primaryStart
        t.pause()
        val sizes = calcSizes(frame.useContents { size.local }, true)
        t.resume()
        var first = true
        for (index in arrangedSubviews.indices) {
            val view = arrangedSubviews[index]
            val size = sizes[index]
            if (!(view.hidden || view.extensionCollapsed == true)) {
                if (first) {
                    first = false
                } else {
                    primary += gap
                }
            }
            val ps = primary
            val a = view.secondaryAlign ?: Align.Stretch
            val offset = when (a) {
                Align.Start -> padding.secondaryStart
                Align.Stretch -> padding.secondaryStart
                Align.End -> mySize.secondary - padding.secondaryEnd - size.secondary
                Align.Center -> (mySize.secondary - size.secondary) / 2
            }
            val secondarySize = (if (a == Align.Stretch) mySize.secondary - padding.secondarySum else size.secondary.coerceAtMost(mySize.secondary - padding.secondarySum))
            val widthSize = if (horizontal) size.primary else secondarySize
            val heightSize = if (horizontal) secondarySize else size.primary
            t.pause()
            /*maybeWithoutAnimation (view.bounds.useContents { this.size.width == 0.0 && this.size.height == 0.0 })*/ run {
                view.setPsuedoframe(
                    if (horizontal) ps else offset,
                    if (horizontal) offset else ps,
                    widthSize,
                    heightSize,
                )
            }
//            val oldSize = view.bounds.useContents { this.size.width to this.size.height }
//            if (oldSize.first != widthSize || oldSize.second != heightSize || view.explicitlyNeedsLayout != false) {
                view.explicitlyNeedsLayout = false
                view.layoutSubviewsAndLayers()
//            }
            t.resume()
            primary += size.primary
        }
        primary += padding.primaryEnd
        t.cancel()
    }

    init { userInteractionEnabled = false }
    override fun hitTest(point: CValue<CGPoint>, withEvent: UIEvent?): UIView? {
        return frameLayoutHitTest(point, withEvent)
    }
}
