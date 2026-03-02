package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.CardSemantic
import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.models.ThemeAndBack
import com.lightningkite.kiteui.views.*
import kotlinx.cinterop.*
import platform.CoreGraphics.*
import platform.QuartzCore.CAGradientLayer
import platform.QuartzCore.CALayer
import platform.UIKit.*


actual class ProgressBar actual constructor(context: RContext) : RView(context) {
    override val native = ResizeableProgressView(CGRectMake(0.0, 0.0, 0.0, 0.0))
    override fun applyTheme(theme: ThemeAndBack) {
        super.applyTheme(theme);
        native.progressLayer.setMask(backgroundLayer?.getOrInitBackgroundMask())
        native.progressLayer.tintColor = theme[CardSemantic].theme.foreground.closestColor().toUiColor()
    }
    actual var ratio by native::progress

    // by Claude
    override var accessibilityValue: String?
        get() = ratio.toString()
        set(value) { super.accessibilityValue = value }

    // by Claude
    override val accessibilityType: String get() = "ProgressBar"
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
class ProgressCALayer : CALayer {
    @OverrideInit
    constructor() : super()

    @OverrideInit
    constructor(coder: platform.Foundation.NSCoder) : super(coder)

    @OverrideInit
    constructor(layer: kotlin.Any) : super(layer)

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
