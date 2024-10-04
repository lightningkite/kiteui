package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.ExternalServices
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.objc.toObjcId
import com.lightningkite.kiteui.views.extensionPadding
import com.lightningkite.kiteui.views.toUiColor
import kotlinx.cinterop.*
import platform.CoreGraphics.*
import platform.Foundation.*
import platform.QuartzCore.CAGradientLayer
import platform.QuartzCore.CALayer
import platform.QuartzCore.kCAGradientLayerAxial
import platform.QuartzCore.kCAGradientLayerRadial
import platform.UIKit.*
import platform.objc.sel_registerName


class UILabelWithGradient : UIView(CGRectZero.readValue()) {

    init {
        userInteractionEnabled = false
    }

    val uiViewWithLabelMask = UIView(bounds).apply {
        backgroundColor = UIColor.grayColor
    }.also(::addSubview)

    val label = UILabel().also {
        uiViewWithLabelMask.addSubview(it)
        uiViewWithLabelMask.maskView = it
//        addSubview(it)
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
                is FadingColor -> {
                    gradientLayer = null
                    uiViewWithLabelMask.backgroundColor = f.base.toUiColor()
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

    override fun sizeThatFits(size: CValue<CGSize>): CValue<CGSize> {
        val padding = extensionPadding ?: 0.0
        val smallerSize = size.useContents {
            CGSizeMake(
                width = width - padding * 2,
                height = height - padding * 2
            )
        }
        return label.sizeThatFits(smallerSize).useContents {
            CGSizeMake(
                width = width + padding * 2,
                height = height.coerceAtLeast(label.font.lineHeight) + padding * 2,
            )
        }
    }

    override fun layoutSubviews() {
        super.layoutSubviews()
        val padding = extensionPadding ?: 0.0
        gradientLayer?.frame = bounds
        bounds.useContents {
            val insetWidth = this@useContents.size.width - 2 * padding
            val insetHeight = this@useContents.size.height - 2 * padding
            uiViewWithLabelMask.setFrame(CGRectMake(
                padding,
                padding,
                insetWidth,
                insetHeight
            ))
            label.setFrame(CGRectMake(
                0.0,
                0.0,
                insetWidth,
                insetHeight
            ))
        }
    }



    @ObjCAction
    fun handleLink() {
        val text = label.attributedText ?: return
        val locationOfTouchInLabel = recognizer.locationInView(uiViewWithLabelMask)
        val layoutManager = NSLayoutManager()
        val textContainer = NSTextContainer(CGSizeMake(0.0, 0.0))
        val storage = NSTextStorage.create(attributedString = text)
        layoutManager.addTextContainer(textContainer)
        storage.addLayoutManager(layoutManager)
        textContainer.lineFragmentPadding = 0.0
        textContainer.lineBreakMode = label.lineBreakMode
        textContainer.maximumNumberOfLines = label.numberOfLines.toULong()
        val labelSize = label.bounds.useContents { CGSizeMake(size.width, size.height) }
        textContainer.size = labelSize
        val textBoundingBox = layoutManager.usedRectForTextContainer(textContainer)
        val textContainerOffset = CGPointMake(
            x = (labelSize.useContents { width } - textBoundingBox.useContents { size.width }) * 0.5 - textBoundingBox.useContents { origin.x },
            y = (labelSize.useContents { height } - textBoundingBox.useContents { size.height }) * 0.5 - textBoundingBox.useContents { origin.y }
        )
        val locationOfTouchInTextContainer = CGPointMake(
            x = locationOfTouchInLabel.useContents { x } - textContainerOffset.useContents { x },
            y = locationOfTouchInLabel.useContents { y } - textContainerOffset.useContents { y }
        )
        val indexOfCharacter = layoutManager.characterIndexForPoint(
            point = locationOfTouchInTextContainer,
            inTextContainer = textContainer,
            fractionOfDistanceBetweenInsertionPoints = null
        )

        val link = label.attributedText?.attribute(NSLinkAttributeName, indexOfCharacter, effectiveRange = null)

        when(link) {
            is NSURL -> ExternalServices.openTab(link.toString())
            is NSString -> ExternalServices.openTab(link as String)
        }

    }

    val recognizer = UITapGestureRecognizer(this, sel_registerName("handleLink"))
    internal fun linkSetup() {
        userInteractionEnabled = true
        uiViewWithLabelMask.userInteractionEnabled = true
        uiViewWithLabelMask.addGestureRecognizer(recognizer)
    }
}