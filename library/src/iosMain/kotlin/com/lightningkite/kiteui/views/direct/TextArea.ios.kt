package com.lightningkite.kiteui.views.direct


import com.lightningkite.kiteui.ExperimentalKiteUi
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.reactive.Action
import com.lightningkite.kiteui.views.*
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import kotlinx.cinterop.ObjCAction
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGRectMake
import platform.UIKit.*
import platform.darwin.NSObject
import platform.objc.sel_registerName

@OptIn(ExperimentalKiteUi::class)
actual class TextArea actual constructor(context: ElementContext) : NativeElement(context), ElementWithAction {
    override val driverValue: String? get() = textAreaDriverValue()
    override val driverActions get() = super<NativeElement>.driverActions + textAreaDriverActions()
    override val native = WrapperView()
    private val delegate = TextAreaDelegate()

    val trigger: NSObject = object : NSObject() {
        @ObjCAction
        fun done() {
            action?.let {
                textField.endEditing(true)
                textField.resignFirstResponder()
                it.startAction(this@TextArea)
            }
        }
    }

    val textField = UITextView().apply {
        smartDashesType = UITextSmartDashesType.UITextSmartDashesTypeNo
        smartQuotesType = UITextSmartQuotesType.UITextSmartQuotesTypeNo
        backgroundColor = UIColor.clearColor
        this.delegate = this@TextArea.delegate
        this.textContainerInset = UIEdgeInsetsMake(0.0, 0.0, 0.0, 0.0)
        this.textContainer.lineFragmentPadding = 0.0

        // Explicit frame prevents UnsatisfiableConstraints error when automatic constraints are set by the system
        // https://stackoverflow.com/questions/54284029/uitoolbar-with-uibarbuttonitem-layoutconstraint-issue
        inputAccessoryView = UIToolbar(CGRectMake(0.0, 0.0, UIScreen.mainScreen.bounds.useContents { size.width }, 35.0)).apply {
            barStyle = UIBarStyleDefault
            setTranslucent(true)
            sizeToFit()
            setItems(
                listOf(
                    UIBarButtonItem(barButtonSystemItem = UIBarButtonSystemItem.UIBarButtonSystemItemFlexibleSpace, target = null, action = null),
                    UIBarButtonItem(title = "Done", style = UIBarButtonItemStyle.UIBarButtonItemStylePlain, target = trigger, action = sel_registerName("done")),
                ), animated = false
            )
        }
    }

    init {
        native.addSubview(textField)
    }

    init {
        delegate.listeners.add {
            textField.informParentOfSizeChange()
        }
    }

    override fun nativeApplyTheme(theme: ThemeAndBack) {
        super.nativeApplyTheme(theme)
        textField.textColor = theme.theme.foreground.closestColor().toUiColor()
        fontAndStyle = theme.theme.font
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

    actual val content: MutableReactiveValue<String> = object : MutableReactiveValue<String> {
        override var value: String
            get() = textField.text
            set(value) {
                if (textField.text != value) {
                    textField.text = value
                    // fire change event so reactive listeners are notified on programmatic updates
                    delegate.listeners.invokeAllSafe()
                }
            }

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
            textField.autocapitalizationType = value.case.ios
            textField.keyboardType = value.type.ios
            textField.textContentType = value.autocomplete.iosTextContentType
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

    private var releaseAction: Release? = null
    actual override var action: Action? = null
        set(value) {
            field = value
            releaseAction?.invoke()
            releaseAction = value?.let { working(it) }
        }

    actual override var enabled: Boolean
        get() = textField.editable
        set(value) {
            textField.setEditable(value)
            refreshTheming()
        }

    init {
        themePipeline.add(ThemePipeline.Step.elementStyling, ClickableSemantic)
        themePipeline.add(ThemePipeline.Step.elementStatus) {
            var t: ThemeDerivation = ThemeDerivation.None
            if (!enabled) t += DisabledSemantic
            if (textField.focused) t += FocusSemantic
            t
        }
    }

    init {
        onRemove(textField.observe("selected") { refreshTheming() })
        onRemove(textField.observe("enabled") { refreshTheming() })
    }
}

private class TextAreaDelegate() : NSObject(), UITextViewDelegateProtocol {
    val listeners = ArrayList<() -> Unit>()
    override fun textViewDidChange(textView: UITextView) {
        listeners.invokeAllSafe()
    }
}