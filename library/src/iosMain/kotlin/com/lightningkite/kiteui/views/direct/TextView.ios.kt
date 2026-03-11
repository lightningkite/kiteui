package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.nsdata
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.ref.WeakReference
import platform.Foundation.*
import platform.UIKit.*

@OptIn(ExperimentalNativeApi::class)
actual class TextView actual constructor(context: RContext) : RView(context) {
    override val native = UILabelWithLayerBackground(WeakReference(context))
//    init {
//        native.rContext = context
//        onRemove { native.rContext = null }
//    }
    val label get() = native.label

    init {
        label.numberOfLines = 0
    }

    actual var content: String = ""
        set(value) {
            field = value
            updateFont()
            native.informParentOfSizeChange()
        }
    private var _align: Align? = null
    actual var align: Align?
        get() = _align
        set(value) {
            _align = value
            applyAlign(value ?: fontAndStyle?.align ?: Align.Start)
        }

    private fun applyAlign(value: Align) {
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
            label.lineBreakMode = if (value) NSLineBreakByTruncatingTail else NSLineBreakByClipping
        }
    actual var wraps: Boolean
        get() = label.numberOfLines == 0L
        set(value) {
            label.numberOfLines = if (value) 0 else 1
        }

    var fontAndStyle: FontAndStyle? = null
        set(value) {
            field = value
            updateFont()
            native.informParentOfSizeChange()
        }

    private fun updateFont() {
        if (originalHtml == null) {
            val alignment = label.textAlignment
            label.font = fontAndStyle?.let {
                it.font.get(it.size.value, it.weight.toUIFontWeight(), it.italic)
            } ?: UIFont.systemFontOfSize(12.0)
            label.textAlignment = alignment
            label.attributedText = NSAttributedString.create(content, mapOf(
                NSStrikethroughStyleAttributeName to if(theme.font.strikethrough) NSUnderlineStyleSingle else NSUnderlineStyleNone,
                NSUnderlineStyleAttributeName to if(theme.font.underline) NSUnderlineStyleSingle else NSUnderlineStyleNone,
            ))
        } else {
            val src = NSMutableAttributedString.create(originalHtml!!)

            // There isn't a good way to determine if a color has been explicitly set in the HTML
            // An NSForegroundColorAttribute is set either way
            // For now, we blindly overwrite the base color for the HTML segment to match the KiteUI theme
            src.addAttribute(NSForegroundColorAttributeName, theme.foreground.closestColor().toUiColor(), NSMakeRange(0U, src.length))

            src.enumerateAttribute(
                NSFontAttributeName,
                inRange = NSMakeRange(0U, src.length),
                options = NSAttributedStringEnumerationLongestEffectiveRangeNotRequired
            ) { attr, range, ptr ->
                val attrFont = attr as? UIFont ?: return@enumerateAttribute
                val bold = attrFont.fontDescriptor.symbolicTraits and UIFontDescriptorTraitBold != 0U
                val italic = attrFont.fontDescriptor.symbolicTraits and UIFontDescriptorTraitItalic != 0U
                val traits = attrFont.fontDescriptor.objectForKey(UIFontDescriptorTraitsAttribute) as? NSDictionary
                val weightNum = (traits?.objectForKey(UIFontWeightTrait) as? NSNumber)?.doubleValue
                val sizeRatio = attrFont.pointSize / 12.0
                val scaled = fontAndStyle?.let {
                    it.font.get(
                        (it.size * sizeRatio).value,
                        weightNum ?: if (bold) UIFontWeightBold else UIFontWeightSemibold,
                        italic
                    )
                } ?: UIFont.systemFontOfSize(12.0)
                src.addAttribute(NSFontAttributeName, scaled, range)
            }
            label.attributedText = src
        }
    }

    actual var wordBreak: WordBreak = WordBreak.Normal
        set(value) {
            label.lineBreakMode = when (value) {
                WordBreak.Normal -> NSLineBreakByWordWrapping
                WordBreak.BreakAll -> NSLineBreakByCharWrapping
            }
        }
    actual var lineClamp: Int? = null
        set(value) {
            field = value
            label.numberOfLines = value?.toLong() ?: 0L
            if (value != null && label.lineBreakMode != NSLineBreakByTruncatingTail) {
                label.lineBreakMode = NSLineBreakByTruncatingTail
            }
        }

    override fun applyTheme(theme: ThemeAndBack) {
        super.applyTheme(theme);
        val theme = theme.theme
        native.foreground = theme.foreground
        fontAndStyle = theme.font
        applyAlign(_align ?: theme.font.align)

        //
//        sizeConstraints = SizeConstraints(
//            minWidth = theme.font.size * 0.6,
//            minHeight = theme.font.size * 1.5,
//        )
    }

    private var originalHtml: NSAttributedString? = null
    actual fun setBasicHtmlContent(html: String) {
        val x = NSAttributedString.create(
            data = html.nsdata()!!,
            options = mapOf(
                NSDocumentTypeDocumentAttribute to NSHTMLTextDocumentType,
                NSCharacterEncodingDocumentAttribute to NSUTF8StringEncoding
            ),
            documentAttributes = null,
            error = null
        )
        originalHtml = x
        updateFont()
        native.linkSetup(html.contains("<a"))
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
