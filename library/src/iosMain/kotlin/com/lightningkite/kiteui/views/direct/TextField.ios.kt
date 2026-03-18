package com.lightningkite.kiteui.views.direct


import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.reactive.Action
import com.lightningkite.kiteui.views.*
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import kotlinx.cinterop.ObjCAction
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGRectMake
import platform.Foundation.*
import platform.UIKit.*
import platform.darwin.NSObject
import platform.objc.sel_registerName


actual class TextInput actual constructor(context: ElementContext) : RViewWithAction(context) {
    override val driverValue: String? get() = textInputDriverValue()
    override val driverActions get() = super.driverActions + textInputDriverActions()
    companion object {
        var alwaysToolbar = false
    }

    val trigger: NSObject = object : NSObject() {
        @ObjCAction
        fun done() {
            action?.let {
                textField.resignFirstResponder()
                it.startAction(this@TextInput)
            } ?: NextFocusDelegateShared.textFieldShouldReturn(textField)
        }
    }
    override val native = WrapperView()
    val textField = UITextField().apply {
        smartDashesType = UITextSmartDashesType.UITextSmartDashesTypeNo
        smartQuotesType = UITextSmartQuotesType.UITextSmartQuotesTypeNo
        backgroundColor = UIColor.clearColor
        delegate = NextFocusDelegateShared
        if (alwaysToolbar) {
            // Explicit frame prevents UnsatisfiableConstraints error when automatic constraints are set by the system
            // https://stackoverflow.com/questions/54284029/uitoolbar-with-uibarbuttonitem-layoutconstraint-issue
            inputAccessoryView = UIToolbar(CGRectMake(0.0, 0.0, UIScreen.mainScreen.bounds.useContents { size.width }, 35.0)).apply {
                barStyle = UIBarStyleDefault
                setTranslucent(true)
                sizeToFit()
                setItems(
                    listOf(
                        UIBarButtonItem(
                            barButtonSystemItem = UIBarButtonSystemItem.UIBarButtonSystemItemFlexibleSpace,
                            target = null,
                            action = null
                        ),
                        UIBarButtonItem(
                            title = "Done",
                            style = UIBarButtonItemStyle.UIBarButtonItemStylePlain,
                            target = trigger,
                            action = sel_registerName("done")
                        ),
                    ), animated = false
                )
            }
        }
    }

    init {
        native.addSubview(textField)


        dropTargetDelegate = object : DropTargetDelegate {
            override fun drop(event: DragEvent): Boolean {
                // Prioritize official text mime types
                for ((mimeType, data) in event.data.typeToData) {
                    if (mimeType.startsWith("text/")) {
                        (data as? String)?.let {
                            content.value = it
                            return true // Successfully handled the drop
                        }
                    }
                }

                // Fallback: accept the first string value found, regardless of mime type
                for ((_, data) in event.data.typeToData) {
                    (data as? String)?.let {
                        content.value = it
                        return true // Successfully handled the drop
                    }
                }

                // No compatible data was found
                return false
            }
        }
    }

    override fun applyTheme(theme: ThemeAndBack) { super.applyTheme(theme); val theme = theme.theme
        textField.textColor = theme.foreground.closestColor().toUiColor()
        fontAndStyle = theme.font
        updateHint()
        applyAlign(_align ?: theme.font.align)
    }

    fun updateFont() {
        val alignment = textField.textAlignment
        textField.font = fontAndStyle?.let {
            it.font.get(it.size.value * preferredScaleFactor(), it.weight.toUIFontWeight(), it.italic)
        } ?: UIFont.systemFontOfSize(16.0)
        textField.textAlignment = alignment
    }

    fun updateHint() {
        textField.attributedPlaceholder = NSAttributedString.create(
            hint,
            mapOf(NSForegroundColorAttributeName to theme.foreground.closestColor().withAlpha(0.5f).toUiColor())
        )
    }

    var fontAndStyle: FontAndStyle? = null
        set(value) {
            field = value
            updateFont()
            native.informParentOfSizeChange()
        }

    actual val content: MutableReactiveValue<String> = object : MutableReactiveValue<String> {
        override fun addListener(listener: () -> Unit): () -> Unit {
            var lastValue = value
            return textField.onEvent(this@TextInput, UIControlEventEditingChanged, listener)
        }

        override var value: String
            get() = textField.text ?: ""
            set(value) {
                if (textField.text == value) return
                textField.text = value
            }
    }
    actual var keyboardHints: KeyboardHints = KeyboardHints()
        set(value) {
            field = value
            textField.autocapitalizationType = value.case.ios
            textField.keyboardType = value.type.ios
            textField.textContentType = value.autocomplete.iosTextContentType
            textField.secureTextEntry = value.autocomplete in setOf(AutoComplete.Password, AutoComplete.NewPassword)
            textField.autocorrectionType = when {
                value.autocorrect && value.type == KeyboardType.Text -> UITextAutocorrectionType.UITextAutocorrectionTypeDefault
                else -> UITextAutocorrectionType.UITextAutocorrectionTypeNo
            }
        }

    @OptIn(kotlin.experimental.ExperimentalNativeApi::class)
    override fun actionSet(value: Action?) {
        super.actionSet(value)
        textField.delegate = value?.let { action ->
            // Use weak reference to avoid retain cycle
            val weakSelf = kotlin.native.ref.WeakReference(this)
            val d = object : NSObject(), UITextFieldDelegateProtocol {
                override fun textFieldShouldReturn(textField: UITextField): Boolean {
                    textField.resignFirstResponder()
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

    actual var hint: String = ""
        set(value) {
            field = value
            updateHint()
        }
    private var _align: Align? = null
    actual var align: Align?
        get() = _align
        set(value) {
            _align = value
            applyAlign(value ?: fontAndStyle?.align ?: Align.Start)
        }

    private fun applyAlign(value: Align) {
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

