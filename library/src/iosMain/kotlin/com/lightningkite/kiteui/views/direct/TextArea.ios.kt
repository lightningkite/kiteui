package com.lightningkite.kiteui.views.direct


import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.reactive.ImmediateWritable
import com.lightningkite.kiteui.reactive.ReadableState
import com.lightningkite.kiteui.reactive.Writable
import com.lightningkite.kiteui.reactive.invokeAllSafe
import com.lightningkite.kiteui.views.*
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.UIKit.*
import platform.darwin.NSObject

actual class TextArea actual constructor(context: RContext) : RView(context) {
    override val native = WrapperView()
    private val delegate = TextAreaDelegate()
    val textField = UITextView().apply {
        smartDashesType = UITextSmartDashesType.UITextSmartDashesTypeNo
        smartQuotesType = UITextSmartQuotesType.UITextSmartQuotesTypeNo
        backgroundColor = UIColor.clearColor
        this.delegate = delegate
    }

    init {
        native.addSubview(textField)
    }


    init {
        NSNotificationCenter.defaultCenter.addObserverForName(
            UIContentSizeCategoryDidChangeNotification,
            textField,
            NSOperationQueue.mainQueue
        ) {
            updateFont()
            native.informParentOfSizeChange()
        }
    }
    override fun applyForeground(theme: Theme) {
        textField.textColor = theme.foreground.closestColor().toUiColor()
        fontAndStyle = theme.font
    }

    fun updateFont() {
        val textSize = textSize
        val alignment = textField.textAlignment
        textField.font = fontAndStyle?.let {
            it.font.get(textSize.value * preferredScaleFactor(), it.weight.toUIFontWeight(), it.italic)
        } ?: UIFont.systemFontOfSize(textSize.value)
        textField.textAlignment = alignment
    }

    fun updateHint() {
        // TODO: Hint
//        textField.attributedPlaceholder = hint
    }

    var textSize: Dimension = 1.rem
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

    actual val content: ImmediateWritable<String> = object : ImmediateWritable<String> {
        override var value: String
            get() = textField.text
            set(value) { textField.text = value }
        override fun addListener(listener: () -> Unit): () -> Unit {
            delegate.listeners.add(listener)
            return {
                val i = delegate.listeners.indexOf(listener)
                if (i != -1) delegate.listeners.removeAt(i)
            }
        }
    }
    actual var keyboardHints: KeyboardHints = KeyboardHints()
        set(value) {
            field = value
            textField.autocapitalizationType = when (value.case) {
                KeyboardCase.None -> UITextAutocapitalizationType.UITextAutocapitalizationTypeNone
                KeyboardCase.Letters -> UITextAutocapitalizationType.UITextAutocapitalizationTypeAllCharacters
                KeyboardCase.Words -> UITextAutocapitalizationType.UITextAutocapitalizationTypeWords
                KeyboardCase.Sentences -> UITextAutocapitalizationType.UITextAutocapitalizationTypeSentences
            }
            textField.keyboardType = when (value.type) {
                KeyboardType.Text -> UIKeyboardTypeDefault
                KeyboardType.Integer -> UIKeyboardTypeNumberPad
                KeyboardType.Phone -> UIKeyboardTypePhonePad
                KeyboardType.Decimal -> UIKeyboardTypeNumbersAndPunctuation
                KeyboardType.Email -> UIKeyboardTypeEmailAddress
            }
            textField.textContentType = when (value.autocomplete) {
                AutoComplete.Email -> UITextContentTypeUsername
                AutoComplete.Password -> UITextContentTypePassword
                AutoComplete.NewPassword -> UITextContentTypeNewPassword
                else -> null
            }
            textField.secureTextEntry = value.autocomplete in setOf(AutoComplete.Password, AutoComplete.NewPassword)
        }
    actual var hint: String = ""
        set(value) {
            field = value
            updateHint()
        }
    inline var align: Align
        get() = when (textField.textAlignment) {
            NSTextAlignmentLeft -> Align.Start
            NSTextAlignmentCenter -> Align.Center
            NSTextAlignmentRight -> Align.End
            NSTextAlignmentJustified -> Align.Stretch
            else -> Align.Start
        }
        set(value) {
            textField.contentMode = when (value) {
                Align.Start -> UIViewContentMode.UIViewContentModeLeft
                Align.Center -> UIViewContentMode.UIViewContentModeCenter
                Align.End -> UIViewContentMode.UIViewContentModeRight
                Align.Stretch -> UIViewContentMode.UIViewContentModeScaleAspectFit
            }
            textField.textAlignment = when (value) {
                Align.Start -> NSTextAlignmentLeft
                Align.Center -> NSTextAlignmentCenter
                Align.End -> NSTextAlignmentRight
                Align.Stretch -> NSTextAlignmentJustified
            }
        }
    actual var enabled: Boolean
        get() = textField.editable
        set(value) {
            textField.editable = value
            refreshTheming()
        }
    init {
        onRemove(textField.observe("highlighted", { refreshTheming() }))
        onRemove(textField.observe("selected", { refreshTheming() }))
        onRemove(textField.observe("enabled", { refreshTheming() }))
    }
    override fun applyState(theme: ThemeAndBack): ThemeAndBack {
        var t = theme
        if(!textField.editable) t = t[DisabledSemantic]
        if(native.focused) t = t[FocusSemantic]
        return t
    }
}

private class TextAreaDelegate() : NSObject(), UITextViewDelegateProtocol {
    val listeners = ArrayList<() -> Unit>()
    override fun textViewDidChange(textView: UITextView) {
        listeners.invokeAllSafe()
    }
}