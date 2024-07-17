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
actual class TextView actual constructor(context: RContext): RView(context) {
    override val native = WrapperView()
    val withGradient = UILabelWithGradient()
    val label get() = withGradient.label
    init {
        label.numberOfLines = 0
        native.addSubview(withGradient)
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
        val alignment = label.textAlignment
        label.font = fontAndStyle?.let {
            it.font.get(it.size.value, it.weight.toUIFontWeight(), it.italic)
        } ?: UIFont.systemFontOfSize(12.0)
        label.textAlignment = alignment
    }

    actual var wordBreak: WordBreak = WordBreak.Normal
        set(value) {
            label.lineBreakMode = when(value) {
                WordBreak.Normal -> NSLineBreakByWordWrapping
                WordBreak.BreakAll -> NSLineBreakByCharWrapping
            }
        }

    override fun applyForeground(theme: Theme) {
        fontAndStyle = theme.font
        withGradient.foreground = theme.foreground
        sizeConstraints = SizeConstraints(
            minWidth = theme.font.size * 0.6,
            minHeight = theme.font.size * 1.5,
        )
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
const val ENABLE_DYNAMIC_TYPE = false
fun preferredScaleFactor() = if (ENABLE_DYNAMIC_TYPE) {
    dynamicTypeScaleFactors[UIApplication.sharedApplication.preferredContentSizeCategory] ?: 1.0
} else {
    1.0
}
fun UILabel.setContentSizeCategoryChangeListener() {
    NSNotificationCenter.defaultCenter.addObserverForName(UIContentSizeCategoryDidChangeNotification, null, NSOperationQueue.mainQueue) {
        updateFont()
        informParentOfSizeChange()
    }
}
fun UILabel.updateFont() {
    val textSize = extensionTextSize ?: return
    val alignment = textAlignment
    font = extensionFontAndStyle?.let {
        it.font.get(textSize * preferredScaleFactor(), it.weight.toUIFontWeight(), it.italic)
    } ?: UIFont.systemFontOfSize(textSize)
    textAlignment = alignment
}