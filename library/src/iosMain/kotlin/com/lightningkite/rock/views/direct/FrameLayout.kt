@file:OptIn(ExperimentalForeignApi::class)

package com.lightningkite.rock.views.direct

import com.lightningkite.rock.models.Align
import com.lightningkite.rock.models.SizeConstraints
import com.lightningkite.rock.views.*
import kotlinx.cinterop.*
import platform.CoreGraphics.*
import platform.UIKit.*
import kotlin.math.max

//private val UIViewLayoutParams = ExtensionProperty<UIView, LayoutParams>()
//val UIView.layoutParams: LayoutParams by UIViewLayoutParams
//
//class LayoutParams()

@OptIn(ExperimentalForeignApi::class)
class FrameLayout: UIView(CGRectZero.readValue()) {
    var padding: CGFloat = 0.0

    data class Size(var width: Double = 0.0, var height: Double = 0.0, var margin: Double = 0.0) {
    }
    val Size.objc get() = CGSizeMake(width, height)
    val CGSize.local get() = Size(width, height)
    val CValue<CGSize>.local get() = useContents { local }

    override fun sizeThatFits(size: CValue<CGSize>): CValue<CGSize> {
        val size = size.local
        val measuredSize = Size()

        val sizes = calcSizes(size)
        for (size in sizes) {
            measuredSize.width = max(measuredSize.width, size.width + padding * 2 + size.margin * 2)
            measuredSize.height = max(measuredSize.height, size.height + padding * 2 + size.margin * 2)
        }

        return measuredSize.objc
    }

    fun calcSizes(size: Size): List<Size> {
//        let size = padding.shrinkSize(size)
        val remaining = size.copy()

        return subviews.map {
            it as UIView
            val required = it.sizeThatFits(remaining.objc).local
            it.extensionSizeConstraints?.let {
                it.maxWidth?.let { required.width = required.width.coerceAtMost(it.value) }
                it.maxHeight?.let { required.height = required.height.coerceAtMost(it.value) }
                it.minWidth?.let { required.width = required.width.coerceAtLeast(it.value) }
                it.minHeight?.let { required.height = required.height.coerceAtLeast(it.value) }
                it.width?.let { required.width = it.value }
                it.height?.let { required.height = it.value }
            }
            if(it.hidden) return@map Size(0.0, 0.0)
            val m = it.extensionMargin ?: 0.0
            required.margin = m
            required.width = required.width.coerceAtLeast(0.0)
            required.height = required.height.coerceAtLeast(0.0)

            remaining.width = remaining.width.coerceAtLeast(required.width + 2 * m)
            remaining.height = remaining.height.coerceAtLeast(required.height + 2 * m)
            required
        }
    }

    override fun layoutSubviews() {
        val mySize = bounds.useContents { size.local }
        var width = padding
        subviews.zip(calcSizes(frame.useContents { size.local })) { view, size ->
            view as UIView
            val m = view.extensionMargin ?: 0.0
            val h = view.extensionHorizontalAlign ?: Align.Stretch
            val v = view.extensionVerticalAlign ?: Align.Stretch
            val offsetH = when(h) {
                Align.Start -> m + padding
                Align.Stretch -> m + padding
                Align.End -> mySize.width - m - padding - size.width
                Align.Center -> (mySize.width - size.width - 2 * m) / 2
            }
            val offsetV = when(v) {
                Align.Start -> m + padding
                Align.Stretch -> m + padding
                Align.End -> mySize.height - m - padding - size.height
                Align.Center -> (mySize.height - size.height - 2 * m) / 2
            }
            val widthSize = if(h == Align.Stretch) mySize.width - m * 2 - padding * 2 else size.width
            val heightSize = if(v == Align.Stretch) mySize.height - m * 2 - padding * 2 else size.height
            view.setFrame(
                CGRectMake(
                    offsetH,
                    offsetV,
                    widthSize,
                    heightSize,
                )
            )
            view.layoutSubviews()
        }
    }
}