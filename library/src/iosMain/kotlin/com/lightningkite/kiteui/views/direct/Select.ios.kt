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


public actual class Select actual constructor(context: ElementContext) : NativeInteractiveElement(context) {
    private var _driverSelectedDisplay: String? = null
    private var _driverSelectSetValue: (suspend (String) -> Unit)? = null
    override val driverValue: String? get() = _driverSelectedDisplay
    override val driverActions: Map<String, suspend (List<String>) -> String>
        get() = super.driverActions + buildMap {
            _driverSelectSetValue?.let { setter -> put("setValue") { args: List<String> -> setter(args.joinToString(" ")); "OK" } }
        }
    override val native: WrapperView = WrapperView()
    internal val textField: TextFieldInput = TextFieldInput(this)
    override val control: UIControl get() = textField

    init {
        setupControl()
        native.addSubview(textField)
        textField.inputView = UIPickerView()
    }

    public actual fun <T> bind(
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
                reactive {
                    val current = edits()
                    textField.text = render(current)
                    // Keep the wheel's highlighted row in sync so opening the picker
                    // shows the actual current selection instead of a stale row.
                    val idx = list.indexOf(current)
                    if (idx >= 0) picker.selectRow(idx.toLong(), inComponent = 0L, animated = false)
                }
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

    internal var fontAndStyle: FontAndStyle? = null
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

    internal fun updateFont() {
        val alignment = textField.textAlignment
        textField.font = fontAndStyle?.let {
            it.font.get(it.size.value * preferredScaleFactor(), it.weight.toUIFontWeight(), it.italic)
        } ?: UIFont.systemFontOfSize(16.0)
        textField.textAlignment = alignment
    }
}
