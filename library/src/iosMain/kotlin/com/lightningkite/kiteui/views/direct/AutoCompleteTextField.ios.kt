package com.lightningkite.kiteui.views.direct


import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.*
import platform.UIKit.*
import platform.darwin.NSObject



actual class AutoCompleteTextField actual constructor(context: RContext) : RViewWithAction(context) {
    override val native = WrapperView()
    val textField = UITextField().apply {
        smartDashesType = UITextSmartDashesType.UITextSmartDashesTypeNo
        smartQuotesType = UITextSmartQuotesType.UITextSmartQuotesTypeNo
        backgroundColor = UIColor.clearColor
    }

    init {
        native.addSubview(textField)
    }

    override fun applyTheme(theme: ThemeAndBack) { super.applyTheme(theme); val theme = theme.theme
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
        textField.placeholder = hint
        // TODO: Colored hint
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

    actual val content: MutableReactiveValue<String> = object : MutableReactiveValue<String> {
        override var value: String
            get() = textField.text ?: ""
            set(value) { textField.text = value }
        override fun addListener(listener: () -> Unit): () -> Unit {
            return textField.onEvent(this@AutoCompleteTextField, UIControlEventEditingChanged, listener)
        }
    }
    actual var keyboardHints: KeyboardHints = KeyboardHints()
        set(value) {
            field = value
            textField.autocapitalizationType = value.case.ios
            textField.keyboardType = value.type.ios
            textField.textContentType = value.autocomplete.iosTextContentType
            textField.secureTextEntry = value.autocomplete in setOf(AutoComplete.Password, AutoComplete.NewPassword)
        }
    @OptIn(kotlin.experimental.ExperimentalNativeApi::class)
    override fun actionSet(value: Action?) {
        super.actionSet(value)
            textField.delegate = value?.let { action ->
                // Use weak reference to avoid retain cycle
                val weakSelf = kotlin.native.ref.WeakReference(this)
                val d = object : NSObject(), UITextFieldDelegateProtocol {
                    override fun textFieldShouldReturn(textField: UITextField): Boolean {
                        weakSelf.get()?.let { action.startAction(it) }
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
    var hint: String = ""
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
    actual var suggestions: List<String> = listOf()
}

