package com.lightningkite.kiteui.views.direct


import com.lightningkite.kiteui.models.ThemeAndBack
import com.lightningkite.kiteui.views.*
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGPointMake
import platform.CoreGraphics.CGRect
import platform.CoreGraphics.CGRectMake
import platform.QuartzCore.CAShapeLayer
import platform.QuartzCore.kCALineCapRound
import platform.UIKit.UIBezierPath
import platform.UIKit.UIColor
import platform.UIKit.UIView
import kotlin.math.PI

// File scope rather than a companion: Kotlin/Native does not permit fields on the companion
// object of a subclass of an Objective-C type, which CircularProgressView is.
private const val LINE_WIDTH = 3.0

public actual class CircularProgress actual constructor(context: ElementContext) : NativeElement(context) {
    override val native: CircularProgressView = CircularProgressView(CGRectMake(0.0, 0.0, 0.0, 0.0))

    override fun nativeApplyTheme(theme: ThemeAndBack) {
        super.nativeApplyTheme(theme)
        native.trackColor = theme.theme.background.closestColor().toUiColor()
        native.progressColor = theme.theme.foreground.closestColor().toUiColor()
    }

    public actual var ratio: Float by native::ratio
}

/**
 * Draws a circular progress indicator by stroking a circular [UIBezierPath] with a
 * [CAShapeLayer], animating [CAShapeLayer.strokeEnd] as the ratio changes, matching the
 * arc/track split used by the web (SVG) and Android implementations.
 */
@OptIn(ExperimentalForeignApi::class)
public class CircularProgressView(frame: CValue<CGRect>) : UIView(frame) {

    private val trackLayer = CAShapeLayer().apply {
        fillColor = null
        lineWidth = LINE_WIDTH
    }
    private val progressLayer = CAShapeLayer().apply {
        fillColor = null
        lineWidth = LINE_WIDTH
        lineCap = kCALineCapRound
        strokeEnd = 0.0
    }

    internal var trackColor: UIColor = UIColor.clearColor
        set(value) {
            field = value
            trackLayer.strokeColor = value.CGColor
        }

    internal var progressColor: UIColor = UIColor.clearColor
        set(value) {
            field = value
            progressLayer.strokeColor = value.CGColor
        }

    internal var ratio: Float = 0f
        set(value) {
            field = value.coerceIn(0f, 1f)
            progressLayer.strokeEnd = field.toDouble()
        }

    init {
        layer.addSublayer(trackLayer)
        layer.addSublayer(progressLayer)
    }

    override fun layoutSubviews() {
        super.layoutSubviews()
        trackLayer.frame = bounds
        progressLayer.frame = bounds
        val path = circularPath()
        trackLayer.path = path
        progressLayer.path = path
    }

    private fun circularPath() = bounds.useContents {
        val radius = (minOf(size.width, size.height) / 2.0) - LINE_WIDTH / 2.0
        val center = CGPointMake(size.width / 2.0, size.height / 2.0)
        UIBezierPath.bezierPathWithArcCenter(
            center = center,
            radius = radius,
            startAngle = -PI / 2.0,
            endAngle = 1.5 * PI,
            clockwise = true
        ).CGPath
    }
}
