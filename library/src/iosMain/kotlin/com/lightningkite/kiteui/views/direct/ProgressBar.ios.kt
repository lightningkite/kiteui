package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.CardSemantic
import com.lightningkite.kiteui.models.ThemeAndBack
import com.lightningkite.kiteui.views.*
import kotlinx.cinterop.*
import platform.CoreGraphics.*
import platform.QuartzCore.CALayer
import platform.UIKit.*


public actual class ProgressBar actual constructor(context: ElementContext) : NativeElement(context) {
    override val native: ResizeableProgressView = ResizeableProgressView(CGRectMake(0.0, 0.0, 0.0, 0.0))

    override fun nativeApplyTheme(theme: ThemeAndBack) {
        super.nativeApplyTheme(theme)
        native.progressLayer.setMask(backgroundLayer?.getOrInitBackgroundMask())
        native.progressLayer.tintColor = theme[CardSemantic].theme.foreground.closestColor().toUiColor()
    }

    public actual var ratio: Float by native::progress
}

@OptIn(ExperimentalForeignApi::class)
public class ResizeableProgressView(frame: CValue<CGRect>) : UIView(frame) {

    internal val progressLayer: ProgressCALayer = ProgressCALayer().also {
        layer.insertSublayer(it, 0u)
    }
    internal var progress: Float by progressLayer::progress

    override fun sizeThatFits(size: CValue<CGSize>): CValue<CGSize> {
        return size.useContents { CGSizeMake(width, 5.0) }
    }

    override fun layoutSubviews() {
        super.layoutSubviews()
        progressLayer.frame = bounds
    }
}

@OptIn(ExperimentalForeignApi::class)
public class ProgressCALayer : CALayer {
    @OverrideInit
    public constructor() : super()

    @OverrideInit
    public constructor(coder: platform.Foundation.NSCoder) : super(coder)

    @OverrideInit
    public constructor(layer: kotlin.Any) : super(layer)

    internal var tintColor: UIColor = UIColor.whiteColor
    internal var progress: Float = 0f
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
