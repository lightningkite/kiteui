package com.lightningkite.kiteui.views.direct


import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.reactive.Action
import com.lightningkite.kiteui.views.*
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import kotlinx.cinterop.ObjCSignatureOverride
import platform.UIKit.*
import platform.darwin.NSInteger
import platform.darwin.NSObject


actual class Select actual constructor(context: ElementContext) : NativeInteractiveElement(context) {
    private var _driverSelectedDisplay: String? = null
    private var _driverSelectSetValue: (suspend (String) -> Unit)? = null
    override val driverValue: String? get() = _driverSelectedDisplay
    override val driverActions
        get() = super.driverActions + buildMap {
            _driverSelectSetValue?.let { setter -> put("setValue") { args: List<String> -> setter(args.joinToString(" ")); "OK" } }
        }
    override val native = WrapperView()
    val textField = TextFieldInput(this)
    override val control: UIControl get() = textField

    init {
        native.addSubview(textField)
        textField.inputView = UIPickerView()
    }

    actual fun <T> bind(
        edits: MutableReactive<T>,
        data: Reactive<List<T>>,
        render: (T) -> String
    ) {
        val picker = (textField.inputView as UIPickerView)
        val source = object : NSObject(), UIPickerViewDataSourceProtocol, UIPickerViewDelegateProtocol {
            var list: List<T> = listOf()

            init {
                reactive {
                    list = data()
                    picker.reloadAllComponents()
                }
                reactive { textField.text = render(edits()) }
            }

            override fun numberOfComponentsInPickerView(pickerView: UIPickerView): NSInteger = 1L

            @ObjCSignatureOverride
            override fun pickerView(pickerView: UIPickerView, numberOfRowsInComponent: NSInteger): NSInteger = list.size.toLong()

            @ObjCSignatureOverride
            override fun pickerView(pickerView: UIPickerView, titleForRow: NSInteger, forComponent: NSInteger): String? {
                return render(list[titleForRow.toInt()])
            }

            var index = 0
            val set = Action("Set Value", Icon.send, frequencyCap = null, ignoreRetryWhileRunning = false) {
                val item = list[index]
                edits set item
            }

            @ObjCSignatureOverride
            override fun pickerView(pickerView: UIPickerView, didSelectRow: NSInteger, inComponent: NSInteger) {
                index = didSelectRow.toInt()
                set.startAction(this@Select)
            }
        }
        picker.setDataSource(source)
        picker.setDelegate(source)
        native.extensionStrongRef = source
        onRemove {
            native.extensionStrongRef = null
            picker.setDataSource(null)
            picker.setDelegate(null)
        }
        // Driver support: track selected display and allow setValue
        reactive {
            _driverSelectedDisplay = render(edits())
        }
        _driverSelectSetValue = { displayText ->
            val item = source.list.firstOrNull { render(it) == displayText }
                ?: throw com.lightningkite.kiteui.views.DriverActionException("No option matching '$displayText'")
            edits.set(item)
        }
    }

    var fontAndStyle: FontAndStyle? = null
        set(value) {
            field = value
            updateFont()
            native.informParentOfSizeChange()
        }

    override fun nativeApplyTheme(theme: ThemeAndBack) {
        super.nativeApplyTheme(theme)
        textField.textColor = theme.theme.foreground.closestColor().toUiColor()
        fontAndStyle = theme.theme.font
    }

    fun updateFont() {
        val alignment = textField.textAlignment
        textField.font = fontAndStyle?.let {
            it.font.get(it.size.value * preferredScaleFactor(), it.weight.toUIFontWeight(), it.italic)
        } ?: UIFont.systemFontOfSize(16.0)
        textField.textAlignment = alignment
    }
}
