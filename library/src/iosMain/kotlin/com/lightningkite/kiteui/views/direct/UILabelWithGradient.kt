package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.Color
import com.lightningkite.kiteui.models.LinearGradient
import com.lightningkite.kiteui.models.Paint
import com.lightningkite.kiteui.models.RadialGradient
import com.lightningkite.kiteui.objc.toObjcId
import com.lightningkite.kiteui.views.extensionPadding
import com.lightningkite.kiteui.views.toUiColor
import kotlinx.cinterop.*
import platform.CoreGraphics.*
import platform.Foundation.NSNumber
import platform.Foundation.numberWithFloat
import platform.QuartzCore.CAGradientLayer
import platform.QuartzCore.CALayer
import platform.QuartzCore.kCAGradientLayerAxial
import platform.QuartzCore.kCAGradientLayerRadial
import platform.UIKit.UIColor
import platform.UIKit.UILabel
import platform.UIKit.UIView

@OptIn(ExperimentalForeignApi::class)
class UILabelWithGradient : UIView(CGRectZero.readValue()) {

    init {
        userInteractionEnabled = false
    }

    private val uiViewWithLabelMask = UIView(bounds).apply {
        backgroundColor = UIColor.grayColor
    }.also(::addSubview)

    val label = UILabel().also {
        uiViewWithLabelMask.addSubview(it)
        uiViewWithLabelMask.maskView = it
    }

    private var gradientLayer: CALayer? = null
        set(value) {
            field?.removeFromSuperlayer()
            value?.let {
                it.frame = this@UILabelWithGradient.bounds
                uiViewWithLabelMask.layer.insertSublayer(it, atIndex = 0.toUInt())
            }
            field = value
        }
    var foreground: Paint = Color.black
        set(f) {
            field = f
            when (f) {
                is Color -> {
                    gradientLayer = null
                    uiViewWithLabelMask.backgroundColor = f.toUiColor()
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
        val padding = extensionPadding ?: 0.0
        gradientLayer?.frame = bounds
        bounds.useContents {
            val childFrame = cValue<CGRect> {
                origin.x = padding
                origin.y = padding
                size.width = this@useContents.size.width - 2 * padding
                size.height = this@useContents.size.height - 2 * padding
            }
            uiViewWithLabelMask.setFrame(childFrame)
            label.setFrame(childFrame)
        }
    }
}