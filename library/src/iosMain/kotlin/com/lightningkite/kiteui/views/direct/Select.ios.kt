package com.lightningkite.kiteui.views.direct


import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.reactive.Action
import com.lightningkite.kiteui.views.*
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCSignatureOverride
import kotlinx.coroutines.launch
import platform.CoreGraphics.CGRectMake
import platform.UIKit.*
import platform.darwin.NSInteger
import platform.darwin.NSObject


actual class Select actual constructor(context: RContext): RView(context) {
    override val native = WrapperView()
    val textField = TextFieldInput(this)
    init {
        native.addSubview(textField)
        textField.inputView = UIPickerView()
    }

    private var _accessibilitySetter: ((String) -> Unit)? = null  // by Claude

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
        _accessibilitySetter = { text ->  // by Claude
            @Suppress("UNCHECKED_CAST")
            val item = source.list.firstOrNull { render(it) == text }
                ?: throw IllegalArgumentException("No option matching '$text'")
            launch { edits set item }
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

    // by Claude
    override var accessibilityValue: String?
        get() = textField.text
        set(value) {
            val setter = _accessibilitySetter ?: throw IllegalStateException("Select not bound")
            setter(value ?: throw IllegalArgumentException("Cannot set null on Select"))
        }

    // by Claude - select supports click and setValue
    override val accessibilityActions: Set<String> get() = setOf("click", "setValue")
    override fun performAccessibilityAction(action: String, value: String?): String? = when (action) {
        "click" -> null  // click is handled at the native level
        "setValue" -> { accessibilityValue = value; null }
        else -> super.performAccessibilityAction(action, value)
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
