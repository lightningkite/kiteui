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


public actual class ProgressBar public actual constructor(context: RContext) : RView(context) {
    override val native = ResizeableProgressView(CGRectMake(0.0, 0.0, 0.0, 0.0))
    override fun applyTheme(theme: ThemeAndBack) {
        super.applyTheme(theme);
        native.progressLayer.setMask(backgroundLayer?.getOrInitBackgroundMask())
        native.progressLayer.tintColor = theme[CardSemantic].theme.foreground.closestColor().toUiColor()
    }
    public actual var ratio by native::progress
}

@OptIn(ExperimentalForeignApi::class)
public class ResizeableProgressView(frame: CValue<CGRect>) : UIView(frame) {

    public val progressLayer = ProgressCALayer().also {
        layer.insertSublayer(it, 0u)
    }
    public var progress by progressLayer::progress

    public override fun sizeThatFits(size: CValue<CGSize>): CValue<CGSize> {
        return size.useContents { CGSizeMake(width, 5.0) }
    }

    public override fun layoutSubviews() {
        super.layoutSubviews()
        progressLayer.frame = bounds
    }
}

@OptIn(ExperimentalForeignApi::class)
public class ProgressCALayer : CALayer {
    @OverrideInit
    constructor() : super()

    @OverrideInit
    constructor(coder: platform.Foundation.NSCoder) : super(coder)

    @OverrideInit
    constructor(layer: kotlin.Any) : super(layer)

    public var tintColor: UIColor = UIColor.whiteColor
    public var progress: Float = 0f
        set(value) {
            field = value
            setNeedsDisplay()
        }

    @ExperimentalForeignApi
    public override fun drawInContext(ctx: CGContextRef?) {
        val progressRect = bounds.useContents {
            CGRectMake(origin.x, origin.y, size.width * progress, size.height)
        }
        CGContextSetFillColorWithColor(ctx, tintColor.CGColor)
        CGContextFillRect(ctx, progressRect)
    }
}
