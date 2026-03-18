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


actual class Select actual constructor(context: ElementContext): RView(context) {
    private var _driverSelectedDisplay: String? = null
    private var _driverSelectSetValue: (suspend (String) -> Unit)? = null
    override val driverValue: String? get() = _driverSelectedDisplay
    override val driverActions get() = super.driverActions + buildMap {
        _driverSelectSetValue?.let { setter -> put("setValue") { args: List<String> -> setter(args.joinToString(" ")); "OK" } }
    }
    override val native = WrapperView()
    val textField = TextFieldInput(this)
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
        val source = object: NSObject(), UIPickerViewDataSourceProtocol, UIPickerViewDelegateProtocol {
            var list: List<T> = listOf()

            init {
                reactiveScope {
                    list = data()
                    picker.reloadAllComponents()
                }
                reactiveScope { textField.text = render(edits()) }
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
        reactiveScope {
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
    override fun applyTheme(theme: ThemeAndBack) { super.applyTheme(theme); val theme = theme.theme
        textField.textColor = theme.foreground.closestColor().toUiColor()
        fontAndStyle = theme.font
    }
    fun updateFont() {
        val alignment = textField.textAlignment
        textField.font = fontAndStyle?.let {
            it.font.get(it.size.value * preferredScaleFactor(), it.weight.toUIFontWeight(), it.italic)
        } ?: UIFont.systemFontOfSize(16.0)
        textField.textAlignment = alignment
    }

    actual var enabled: Boolean
        get() = textField.enabled
        set(value) {
            textField.enabled = value
            refreshTheming()
        }
    init {
        onRemove(textField.observe("highlighted", { refreshTheming() }))
        onRemove(textField.observe("selected", { refreshTheming() }))
        onRemove(textField.observe("enabled", { refreshTheming() }))
    }
    override fun applyState(theme: ThemeAndBack): ThemeAndBack {
        var t = theme[ClickableSemantic]
        if(!enabled) t = t[DisabledSemantic]
        if(textField.highlighted) t = t[DownSemantic]
        if(textField.focused) t = t[FocusSemantic]
        return super.applyState(t)
    }
}
