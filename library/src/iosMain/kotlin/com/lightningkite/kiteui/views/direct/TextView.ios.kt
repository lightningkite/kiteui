package com.lightningkite.kiteui.views.direct


import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.objc.toObjcId
import com.lightningkite.kiteui.views.*
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreGraphics.CGPointMake
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSNumber
import platform.Foundation.NSOperationQueue
import platform.Foundation.numberWithFloat
import platform.QuartzCore.CAGradientLayer
import platform.QuartzCore.CALayer
import platform.QuartzCore.kCAGradientLayerAxial
import platform.QuartzCore.kCAGradientLayerRadial
import platform.UIKit.*


@OptIn(ExperimentalForeignApi::class)
actual abstract class TextView actual constructor(context: RContext): RView(context) {
    override val native = WrapperView()
    val withGradient = UILabelWithGradient()
    val label get() = withGradient.label
    init {
        label.numberOfLines = 0
        native.addSubview(label)
    }

    init {
        NSNotificationCenter.defaultCenter.addObserverForName(UIContentSizeCategoryDidChangeNotification, label, NSOperationQueue.mainQueue) {
            updateFont()
            native.informParentOfSizeChange()
        }
    }

    actual inline var content: String
        get() = label.text ?: ""
        set(value) {
            label.text = value
            native.informParentOfSizeChange()
        }
    actual inline var align: Align
        get() = when (label.textAlignment) {
            NSTextAlignmentLeft -> Align.Start
            NSTextAlignmentCenter -> Align.Center
            NSTextAlignmentRight -> Align.End
            NSTextAlignmentJustified -> Align.Stretch
            else -> Align.Start
        }
        set(value) {
            native.contentMode = when (value) {
                Align.Start -> UIViewContentMode.UIViewContentModeLeft
                Align.Center -> UIViewContentMode.UIViewContentModeCenter
                Align.End -> UIViewContentMode.UIViewContentModeRight
                Align.Stretch -> UIViewContentMode.UIViewContentModeScaleAspectFit
            }
            label.textAlignment = when (value) {
                Align.Start -> NSTextAlignmentLeft
                Align.Center -> NSTextAlignmentCenter
                Align.End -> NSTextAlignmentRight
                Align.Stretch -> NSTextAlignmentJustified
            }
        }

    actual var textSize: Dimension = 1.rem
        set(value) {
            field = value
            updateFont()
            native.informParentOfSizeChange()
        }
    actual var ellipsis: Boolean
        get() = label.lineBreakMode == NSLineBreakByTruncatingTail
        set(value) {
            label.lineBreakMode = if(value) NSLineBreakByTruncatingTail else NSLineBreakByClipping
        }
    actual var wraps: Boolean
        get() = label.numberOfLines == 0L
        set(value) {
            label.numberOfLines = if(value) 0 else 1
        }

    var fontAndStyle: FontAndStyle? = null
        set(value) {
            field = value
            updateFont()
            native.informParentOfSizeChange()
        }

    private fun updateFont() {
        val textSize = textSize ?: return
        val alignment = label.textAlignment
        label.font = fontAndStyle?.let {
            it.font.get(textSize.value * preferredScaleFactor(), it.weight.toUIFontWeight(), it.italic)
        } ?: UIFont.systemFontOfSize(textSize.value)
        label.textAlignment = alignment
    }
}


object TextSizes {
    val h1 get() = 2.rem
    val h2 get() = 1.6.rem
    val h3 get() = 1.4.rem
    val h4 get() = 1.3.rem
    val h5 get() = 1.2.rem
    val h6 get() = 1.1.rem
    val h = arrayOf(
        h1,
        h2,
        h3,
        h4,
        h5,
        h6,
    )
    val body get() = 1.rem
    val subtext get() = 0.8.rem
}

actual class HeaderView actual constructor(context: RContext, level: Int) : TextView(context) {
    init {
        textSize = TextSizes.h[level - 1]
        sizeConstraints = SizeConstraints(
            minWidth = textSize * 0.6,
            minHeight = textSize * 1.5,
        )
    }
    override fun applyForeground(theme: Theme) {
        fontAndStyle = theme.title
        withGradient.foreground = theme.foreground
    }
}

actual class BodyTextView actual constructor(context: RContext) : TextView(context) {
    init {
        textSize = TextSizes.body
        sizeConstraints = SizeConstraints(
            minWidth = textSize * 0.6,
            minHeight = textSize * 1.5,
        )
    }
    override fun applyForeground(theme: Theme) {
        fontAndStyle = theme.body
        withGradient.foreground = theme.foreground
    }
}

actual class SubTextView actual constructor(context: RContext) : TextView(context) {
    init {
        textSize = TextSizes.subtext
        sizeConstraints = SizeConstraints(
            minWidth = textSize * 0.6,
            minHeight = textSize * 1.5,
        )
    }
    override fun applyForeground(theme: Theme) {
        fontAndStyle = theme.body
        withGradient.foreground = theme.foreground.applyAlpha(0.5f)
    }
}

// Calculated from font sizes shown at https://developer.apple.com/design/human-interface-guidelines/typography#Specifications
private val dynamicTypeScaleFactors = mapOf(
    UIContentSizeCategoryUnspecified to 1.0,
    UIContentSizeCategoryExtraSmall to 0.87,
    UIContentSizeCategorySmall to 0.91,
    UIContentSizeCategoryMedium to 0.95,
    UIContentSizeCategoryLarge to 1.0,
    UIContentSizeCategoryExtraLarge to 1.21,
    UIContentSizeCategoryExtraExtraLarge to 1.31,
    UIContentSizeCategoryExtraExtraExtraLarge to 1.42,
)
fun preferredScaleFactor() = dynamicTypeScaleFactors[UIApplication.sharedApplication.preferredContentSizeCategory] ?: 1.0
