package com.lightningkite.kiteui.views.direct


import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.reactive.Action
import com.lightningkite.kiteui.reactive.ImmediateWritable
import com.lightningkite.kiteui.reactive.ReadableState
import com.lightningkite.kiteui.reactive.Writable
import com.lightningkite.kiteui.reactive.onRemove
import com.lightningkite.kiteui.views.*
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.*
import platform.UIKit.*
import platform.darwin.NSObject


actual class TextInput actual constructor(context: RContext) : RViewWithAction(context) {
    override val native = WrapperView()
    val textField = UITextField().apply {
        smartDashesType = UITextSmartDashesType.UITextSmartDashesTypeNo
        smartQuotesType = UITextSmartQuotesType.UITextSmartQuotesTypeNo
        backgroundColor = UIColor.clearColor
        delegate = NextFocusDelegateShared
    }

    init {
        native.addSubview(textField)
    }

    override fun applyForeground(theme: Theme) {
        textField.textColor = theme.foreground.closestColor().toUiColor()
        fontAndStyle = theme.font
        updateHint()
    }

    fun updateFont() {
        val alignment = textField.textAlignment
        textField.font = fontAndStyle?.let {
            it.font.get(it.size.value * preferredScaleFactor(), it.weight.toUIFontWeight(), it.italic)
        } ?: UIFont.systemFontOfSize(16.0)
        textField.textAlignment = alignment
    }

    fun updateHint() {
        textField.attributedPlaceholder = NSAttributedString.create(hint, mapOf(NSForegroundColorAttributeName to theme.foreground.closestColor().withAlpha(0.5f).toUiColor()))
    }

    var fontAndStyle: FontAndStyle? = null
        set(value) {
            field = value
            updateFont()
            native.informParentOfSizeChange()
        }

    actual val content: ImmediateWritable<String> = object : ImmediateWritable<String> {
        override fun addListener(listener: () -> Unit): () -> Unit {
            var lastValue = value
            return textField.onEvent(this@TextInput, UIControlEventEditingChanged, listener)
        }

        override var value: String
            get() = textField.text ?: ""
            set(value) {
                if(textField.text == value) return
                textField.text = value
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
            textField.autocorrectionType = when (value.type) {
                KeyboardType.Text -> UITextAutocorrectionType.UITextAutocorrectionTypeDefault
                else -> UITextAutocorrectionType.UITextAutocorrectionTypeNo
            }
            textField.secureTextEntry = value.autocomplete in setOf(AutoComplete.Password, AutoComplete.NewPassword)
        }

    override fun actionSet(value: Action?) {
        super.actionSet(value)
        textField.delegate = value?.let {
            val d = object : NSObject(), UITextFieldDelegateProtocol {
                override fun textFieldShouldReturn(textField: UITextField): Boolean {
                    it.startAction(this@TextInput)
                    return true
                }
            }
            textField.extensionStrongRef = d
            d
        } ?: NextFocusDelegateShared
        textField.returnKeyType = when (value?.title) {
            "Emergency Call" -> UIReturnKeyType.UIReturnKeyEmergencyCall
            "Go" -> UIReturnKeyType.UIReturnKeyGo
            "Next" -> UIReturnKeyType.UIReturnKeyNext
            "Continue" -> UIReturnKeyType.UIReturnKeyContinue
            "Default" -> UIReturnKeyType.UIReturnKeyDefault
            "Join" -> UIReturnKeyType.UIReturnKeyJoin
            "Done" -> UIReturnKeyType.UIReturnKeyDone
            "Yahoo" -> UIReturnKeyType.UIReturnKeyYahoo
            "Send" -> UIReturnKeyType.UIReturnKeySend
            "Google" -> UIReturnKeyType.UIReturnKeyGoogle
            "Route" -> UIReturnKeyType.UIReturnKeyRoute
            "Search" -> UIReturnKeyType.UIReturnKeySearch
            else -> UIReturnKeyType.UIReturnKeyDone
        }
    }

    actual var hint: String = ""
        set(value) {
            field = value
            updateHint()
        }
    actual inline var align: Align
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
        get() = textField.enabled
        set(value) {
            textField.enabled = value
            refreshTheming()
        }

    init {
        onRemove { textField.delegate = null }
        onRemove(textField.observe("highlighted", { refreshTheming() }))
        onRemove(textField.observe("selected", { refreshTheming() }))
        onRemove(textField.observe("enabled", { refreshTheming() }))
    }

    override fun applyState(theme: ThemeAndBack): ThemeAndBack {
        var t = theme
        if (!textField.enabled) t = t[DisabledSemantic]
        if (textField.highlighted) t = t[DownSemantic]
        if (textField.focused) t = t[FocusSemantic]
        return super.applyState(t)
    }
}

