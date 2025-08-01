package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.ExternalServices
import com.lightningkite.kiteui.WeakReference
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.openTab
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.debugPrint
import com.lightningkite.kiteui.views.extensionPadding
import com.lightningkite.kiteui.views.extensionSafeInsetPadding
import com.lightningkite.kiteui.views.toUiColor
import kotlinx.cinterop.*
import platform.CoreGraphics.*
import platform.Foundation.*
import platform.UIKit.*
import platform.objc.sel_registerName
import kotlin.experimental.ExperimentalNativeApi

@OptIn(ExperimentalNativeApi::class)
class UILabelWithLayerBackground : UIView(CGRectZero.readValue()) {
    init {
        userInteractionEnabled = false
    }

    public val label = UILabel().also {
        userInteractionEnabled = false
    }.also(::addSubview)

    public var foreground: Paint = Color.black
        set(f) {
            field = f
            label.textColor = when (f) {
                is Color -> f.toUiColor()
                is FadingColor -> f.base.toUiColor()
                is LinearGradient -> f.stops.first().color.toUiColor()
                is RadialGradient -> f.stops.first().color.toUiColor()
            }
        }

    public override fun sizeThatFits(size: CValue<CGSize>): CValue<CGSize> {
        val padding = (extensionPadding ?: Edges.ZERO).plus(extensionSafeInsetPadding ?: Edges.ZERO)
        val smallerSize = size.useContents {
            CGSizeMake(
                width = width - padding.horizontalSum.value,
                height = height - padding.verticalSum.value
            )
        }
        return label.sizeThatFits(smallerSize).useContents {
            debugPrint { "Size that fits on text: $width, $height" }
            CGSizeMake(
                width = width + padding.horizontalSum.value,
                height = height.coerceAtLeast(label.font.lineHeight) + padding.verticalSum.value,
            )
        }
    }

    public override fun layoutSubviews() {
        super.layoutSubviews()
        val padding = (extensionPadding ?: Edges.ZERO).plus(extensionSafeInsetPadding ?: Edges.ZERO)
        bounds.useContents {
            val insetWidth = this@useContents.size.width - padding.horizontalSum.value
            val insetHeight = this@useContents.size.height - padding.verticalSum.value
            label.setFrame(CGRectMake(
                padding.left.value,
                padding.top.value,
                insetWidth,
                insetHeight
            ))
        }
    }



    @ObjCAction
    public fun handleLink() {
        val text = label.attributedText ?: return
        val locationOfTouchInLabel = recognizer.locationInView(this)
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

    public val recognizer = UITapGestureRecognizer(this, sel_registerName("handleLink"))
    internal fun linkSetup(active: Boolean) {
        userInteractionEnabled = active
        label.userInteractionEnabled = active
        if(active) {
            label.addGestureRecognizer(recognizer)
        } else {
            if(recognizer.view != null) {
                label.removeGestureRecognizer(recognizer)
            }
        }
    }
}