package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.Color
import com.lightningkite.kiteui.models.LinearGradient
import com.lightningkite.kiteui.models.Paint
import com.lightningkite.kiteui.models.RadialGradient
import com.lightningkite.kiteui.objc.toObjcId
import com.lightningkite.kiteui.views.toUiColor
import kotlinx.cinterop.*
import platform.CoreGraphics.*
import platform.Foundation.NSNumber
import platform.Foundation.numberWithFloat
import platform.QuartzCore.CAGradientLayer
import platform.QuartzCore.CALayer
import platform.QuartzCore.kCAGradientLayerAxial
import platform.QuartzCore.kCAGradientLayerRadial
import platform.UIKit.UILabel
import platform.UIKit.UIView

@OptIn(ExperimentalForeignApi::class)
class UILabelWithGradient : UIView(CGRectZero.readValue()) {

    init {
        userInteractionEnabled = false
    }

    val label = UILabel().also {
        addSubview(it)
        maskView = it
    }

    private var gradientLayer: CALayer? = null
        set(value) {
            field?.removeFromSuperlayer()
            value?.let {
                it.frame = this@UILabelWithGradient.bounds
                layer.insertSublayer(it, atIndex = 0.toUInt())
            }
            field = value
        }
    var foreground: Paint = Color.black
        set(f) {
            field = f
            when (f) {
                is Color -> {
                    gradientLayer = null
                    backgroundColor = f.toUiColor()
                }
                is LinearGradient -> gradientLayer = CAGradientLayer().apply {
                    this.type = kCAGradientLayerAxial
                    this.locations = f.stops.map {
                        NSNumber.numberWithFloat(it.ratio)
                    }
                    this.colors = f.stops.map { it.color.toUiColor().CGColor!!.toObjcId() }
                    this.startPoint = CGPointMake(-f.angle.cos() * .5 + .5, -f.angle.sin() * .5 + .5)
                    this.endPoint = CGPointMake(f.angle.cos() * .5 + .5, f.angle.sin() * .5 + .5)
                }
                is RadialGradient -> gradientLayer = CAGradientLayer().apply {
                    this.type = kCAGradientLayerRadial
                    this.locations = f.stops.map {
                        NSNumber.numberWithFloat(it.ratio)
                    }
                    this.colors = f.stops.map { it.color.toUiColor().CGColor!!.toObjcId() }
                    this.startPoint = CGPointMake(0.5, 0.5)
                    this.endPoint = CGPointMake(0.0, 0.0)
                }
            }
        }

    override fun sizeThatFits(size: CValue<CGSize>): CValue<CGSize> = label.sizeThatFits(size)

    override fun layoutSubviews() {
        super.layoutSubviews()
        gradientLayer?.frame = bounds
        bounds.useContents {
            label.setFrame(cValue<CGRect> {
                origin.x = 0.0
                origin.y = 0.0
                size.width = this@useContents.size.width
                size.height = this@useContents.size.height
            })
        }
    }
}