package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.*
import com.lightningkite.kiteui.models.Dimension
import com.lightningkite.kiteui.models.Edges
import com.lightningkite.kiteui.models.ThemeAndBack
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
import platform.UIKit.*
import platform.darwin.NSInteger

/**
 * A layout that arranges its children in rows and wraps to the next row when there's not enough space.
 * Similar to flexbox in web development.
 */
class FlexLayout : UIView(CGRectZero.readValue()), UIViewWithSizeOverridesProtocol, UIViewWithSpacingRulesProtocol {
    var gap: Double = 0.0
        set(value) {
            field = value
            setNeedsLayout()
            informParentOfSizeChange()
        }

    var lineGap: Double = 0.0
        set(value) {
            field = value
            setNeedsLayout()
            informParentOfSizeChange()
        }

    val spacingOverride = Signal<Dimension?>(null).also {
        it.addListener { it.value?.let { gap = it.value } }
    }

    override fun getSpacingOverrideProperty() = spacingOverride

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

    data class Size(var width: Double = 0.0, var height: Double = 0.0)

    val Size.objc get() = CGSizeMake(width, height)
    val CGSize.local get() = Size(width, height)
    val CValue<CGSize>.local get() = useContents { local }

    val arrangedSubviews = ArrayList<UIView>()
    fun addArrangedSubview(view: UIView) {
        childSizeCache.add(arrangedSubviews.size, HashMap())
        arrangedSubviews.add(view)
        addSubview(view)
        lastLaidOutSize = null
        informParentOfSizeChangeDueToChild()
    }

    fun insertArrangedSubview(view: UIView, atIndex: NSInteger) {
        childSizeCache.add(atIndex.toInt(), HashMap())
        arrangedSubviews.add(atIndex.toInt(), view)
        insertSubview(view, atIndex)
        lastLaidOutSize = null
        informParentOfSizeChangeDueToChild()
    }

    override fun willRemoveSubview(subview: UIView) {
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

    val childSizeCache = ArrayList<HashMap<Size, Size>>()

    override fun sizeThatFits(size: CValue<CGSize>): CValue<CGSize> {
        val sizeLocal = size.local
        val measuredSize = Size()

        val padding = (extensionPadding ?: Edges.ZERO).plus(extensionSafeInsetPadding ?: Edges.ZERO)
        val availableWidth = sizeLocal.width - padding.horizontalSum.value

        var currentLineWidth = 0.0
        var currentLineHeight = 0.0
        var totalHeight = padding.top.value
        var maxWidth = 0.0

        var isFirstInLine = true

        arrangedSubviews.forEachIndexed { index, view ->
            if (view.hidden || view.extensionCollapsed == true) return@forEachIndexed

            val measureInput = Size(availableWidth, sizeLocal.height)
            val childSize = childSizeCache[index].getOrPut(measureInput) {
                view.sizeThatFits2(
                    measureInput.objc,
                    view.extensionSizeConstraints
                ).local
            }

            // Check if this view needs to wrap to the next line
            if (!isFirstInLine && currentLineWidth + childSize.width > availableWidth) {
                // Move to next line
                totalHeight += currentLineHeight + lineGap
                maxWidth = max(maxWidth, currentLineWidth)
                currentLineWidth = 0.0
                currentLineHeight = 0.0
                isFirstInLine = true
            }

            // Add view to current line
            if (isFirstInLine) {
                isFirstInLine = false
            } else {
                currentLineWidth += gap
            }

            currentLineWidth += childSize.width
            currentLineHeight = max(currentLineHeight, childSize.height)
        }

        // Add the last line's height
        if (currentLineHeight > 0) {
            totalHeight += currentLineHeight
            maxWidth = max(maxWidth, currentLineWidth)
        }

        totalHeight += padding.bottom.value
        maxWidth += padding.horizontalSum.value

        measuredSize.width = maxWidth
        measuredSize.height = totalHeight
        return measuredSize.objc
    }

    var lastLaidOutSize: Size? = null
    var lastLaidOutPadding: Edges? = null

    override fun layoutSubviews() {
        val padding = (extensionPadding ?: Edges.ZERO).plus(extensionSafeInsetPadding ?: Edges.ZERO)
        val mySize = bounds.useContents { size.local }

        if (lastLaidOutSize == mySize && lastLaidOutPadding == padding) return

        lastLaidOutSize = mySize
        lastLaidOutPadding = padding

        val availableWidth = mySize.width - padding.horizontalSum.value

        var currentX = padding.left.value
        var currentY = padding.top.value
        var currentLineHeight = 0.0

        var isFirstInLine = true

        arrangedSubviews.forEachIndexed { index, view ->
            if (view.hidden || view.extensionCollapsed == true) return@forEachIndexed

            val measureInput = Size(availableWidth, mySize.height)
            val childSize = childSizeCache[index].getOrPut(measureInput) {
                view.sizeThatFits2(
                    measureInput.objc,
                    view.extensionSizeConstraints
                ).local
            }

            // Check if this view needs to wrap to the next line
            if (!isFirstInLine && (currentX + childSize.width > availableWidth + padding.left.value)) {
                // Move to next line
                currentY += currentLineHeight + lineGap
                currentX = padding.left.value
                currentLineHeight = 0.0
                isFirstInLine = true
            }

            // Add view to current line
            if (isFirstInLine) {
                isFirstInLine = false
            } else {
                currentX += gap
            }

            // Position the view
            view.setPsuedoframe(
                currentX,
                currentY,
                childSize.width,
                childSize.height
            )

            view.layoutSubviewsAndLayers()

            currentX += childSize.width
            currentLineHeight = max(currentLineHeight, childSize.height)
        }
    }

    init { userInteractionEnabled = false }

    override fun hitTest(point: CValue<CGPoint>, withEvent: UIEvent?): UIView? {
        return frameLayoutHitTest(point, withEvent)
    }
}

actual class RowWrappingOld actual constructor(context: ElementContext) : RView(context) {
    override val native = FlexLayout()

    override var gap: Dimension?
        get() = super.gap
        set(value) {
            super.gap = value
            native.gap = (value ?: theme.gap).value
            native.lineGap = (value ?: theme.gap).value
        }

    override fun applyTheme(theme: ThemeAndBack) {
        super.applyTheme(theme)
        native.gap = (gap ?: theme.theme.gap).value
        native.lineGap = (gap ?: theme.theme.gap).value
    }

    override fun internalAddChild(index: Int, view: RView) {
        // Apply parent's default alignment if child doesn't have explicit alignment set
        if (view.lastSetHorizontalAlign == com.lightningkite.kiteui.models.Align.Stretch && newChildHorizontalAlign != null) {
            view.lastSetHorizontalAlign = newChildHorizontalAlign!!
            view.native.extensionHorizontalAlign = newChildHorizontalAlign
        }
        if (view.lastSetVerticalAlign == com.lightningkite.kiteui.models.Align.Stretch && newChildVerticalAlign != null) {
            view.lastSetVerticalAlign = newChildVerticalAlign!!
            view.native.extensionVerticalAlign = newChildVerticalAlign
        }

        if (index == native.arrangedSubviews.size)
            native.addArrangedSubview(view.native)
        else
            native.insertArrangedSubview(view.native, index.toLong())
        if (children[index].native != native.arrangedSubviews.get(index)) throw IllegalStateException("Children mismatch! ${children.map { it.native }} vs ${native.arrangedSubviews}")
    }

    override fun internalRemoveChild(index: Int) {
        if (children[index].native != native.arrangedSubviews.get(index)) throw IllegalStateException("Children mismatch! ${children.map { it.native }} vs ${native.arrangedSubviews}")
        if (index >= native.arrangedSubviews.size || index < 0) {
            throw IllegalStateException("Index $index not in 0..<${native.arrangedSubviews.size}")
        }
        native.arrangedSubviews[index].removeFromSuperview()
    }

    override fun internalClearChildren() {
        native.arrangedSubviews.toList().forEach {
            it.removeFromSuperview()
        }
    }
}
