package com.lightningkite.mppexampleapp.widgets

import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.FontAndStyle
import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.models.WordBreak
import com.lightningkite.kiteui.models.times
import com.lightningkite.kiteui.nsdata
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView
import com.lightningkite.kiteui.views.direct.TextView
import com.lightningkite.kiteui.views.direct.UILabelWithGradient
import com.lightningkite.kiteui.views.informParentOfSizeChange
import com.lightningkite.kiteui.views.toUIFontWeight
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSAttributedString
import platform.Foundation.NSAttributedStringEnumerationLongestEffectiveRangeNotRequired
import platform.Foundation.NSDictionary
import platform.Foundation.NSMakeRange
import platform.Foundation.NSMutableAttributedString
import platform.Foundation.NSNumber
import platform.Foundation.addAttribute
import platform.Foundation.create
import platform.Foundation.enumerateAttribute
import platform.Foundation.length
import platform.UIKit.NSDocumentTypeDocumentAttribute
import platform.UIKit.NSFontAttributeName
import platform.UIKit.NSHTMLTextDocumentType
import platform.UIKit.NSLineBreakByCharWrapping
import platform.UIKit.NSLineBreakByClipping
import platform.UIKit.NSLineBreakByTruncatingTail
import platform.UIKit.NSLineBreakByWordWrapping
import platform.UIKit.NSStrikethroughStyleAttributeName
import platform.UIKit.NSTextAlignmentCenter
import platform.UIKit.NSTextAlignmentJustified
import platform.UIKit.NSTextAlignmentLeft
import platform.UIKit.NSTextAlignmentRight
import platform.UIKit.NSUnderlineStyleAttributeName
import platform.UIKit.NSUnderlineStyleNone
import platform.UIKit.NSUnderlineStyleSingle
import platform.UIKit.UIFont
import platform.UIKit.UIFontDescriptorTraitBold
import platform.UIKit.UIFontDescriptorTraitItalic
import platform.UIKit.UIFontDescriptorTraitsAttribute
import platform.UIKit.UIFontWeightBold
import platform.UIKit.UIFontWeightSemibold
import platform.UIKit.UIFontWeightTrait
import platform.UIKit.UIViewContentMode
import platform.UIKit.UIView
import platform.UIKit.create

@OptIn(ExperimentalForeignApi::class)
actual class Code actual constructor(context: RContext) : RView(context) {
    val actualNative = UILabelWithGradient()
    override val native: UIView = actualNative
    val label get() = actualNative.label

    init {
        label.numberOfLines = 0
    }

    actual var content: String = ""
        set(value) {
            field = value
            updateFont()
            native.informParentOfSizeChange()
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

    override fun applyForeground(theme: Theme) {
        fontAndStyle = theme.font
        actualNative.foreground = theme.foreground

        //
//        sizeConstraints = SizeConstraints(
//            minWidth = theme.font.size * 0.6,
//            minHeight = theme.font.size * 1.5,
//        )
    }

    private var originalHtml: NSAttributedString? = null
}