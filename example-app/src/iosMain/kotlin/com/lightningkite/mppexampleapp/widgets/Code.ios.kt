package com.lightningkite.mppexampleapp.widgets

import com.lightningkite.kiteui.WeakReference
import com.lightningkite.kiteui.models.FontAndStyle
import com.lightningkite.kiteui.models.ThemeAndBack
import com.lightningkite.kiteui.models.times
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.RView
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.direct.UILabelWithLayerBackground
import com.lightningkite.kiteui.views.informParentOfSizeChange
import com.lightningkite.kiteui.views.toUIFontWeight
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*
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
import platform.UIKit.NSFontAttributeName
import platform.UIKit.NSStrikethroughStyleAttributeName
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
import platform.UIKit.UIView

@OptIn(ExperimentalForeignApi::class)
actual class Code actual constructor(context: ElementContext) : RView(context) {
    val actualNative = UILabelWithLayerBackground(WeakReference(context))
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

    override fun applyTheme(theme: ThemeAndBack) { super.applyTheme(theme); val theme = theme.theme
        actualNative.foreground = theme.foreground
        fontAndStyle = theme.font

        //
//        sizeConstraints = SizeConstraints(
//            minWidth = theme.font.size * 0.6,
//            minHeight = theme.font.size * 1.5,
//        )
    }

    private var originalHtml: NSAttributedString? = null
}