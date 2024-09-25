package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.CardSemantic
import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.views.*
import kotlinx.cinterop.*
import platform.CoreGraphics.*
import platform.QuartzCore.CALayer
import platform.UIKit.*

@OptIn(ExperimentalForeignApi::class)
actual class ProgressBar actual constructor(context: RContext) : RView(context) {
    override val native = ResizeableProgressView(CGRectMake(0.0, 0.0, 0.0, 0.0))
    override fun applyForeground(theme: Theme) {
        super.applyForeground(theme)
        native.progressLayer.tintColor = theme[CardSemantic].theme.foreground.closestColor().toUiColor()
    }

    override fun applyBackground(theme: Theme, fullyApply: Boolean) {
        super.applyBackground(theme, fullyApply)
        native.progressLayer.setMask(backgroundLayer?.getOrInitBackgroundMask())
    }
    actual var ratio by native::progress
}

@OptIn(ExperimentalForeignApi::class)
class ResizeableProgressView(frame: CValue<CGRect>) : UIView(frame) {

    val progressLayer = ProgressCALayer().also {
        layer.insertSublayer(it, 0u)
    }
    var progress by progressLayer::progress

    override fun sizeThatFits(size: CValue<CGSize>): CValue<CGSize> {
        return size.useContents { CGSizeMake(width, 5.0) }
    }

    override fun layoutSubviews() {
        super.layoutSubviews()
        progressLayer.frame = bounds
    }
}

@OptIn(ExperimentalForeignApi::class)
class ProgressCALayer : CALayer() {

    var tintColor: UIColor = UIColor.whiteColor
    var progress: Float = 0f
        set(value) {
            field = value
            setNeedsDisplay()
        }

    @ExperimentalForeignApi
    override fun drawInContext(ctx: CGContextRef?) {
        val progressRect = bounds.useContents {
            CGRectMake(origin.x, origin.y, size.width * progress, size.height)
        }
        CGContextSetFillColorWithColor(ctx, tintColor.CGColor)
        CGContextFillRect(ctx, progressRect)
    }
}
