package com.lightningkite.kiteui.views.direct

import android.content.Context
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView as AndroidAutocompleteTextView
import com.lightningkite.kiteui.models.KeyboardHints
import com.lightningkite.kiteui.views.*
import com.lightningkite.reactive.core.*


actual class AutoCompleteTextField actual constructor(context: ElementContext): NativeElementWithAction(context) {
    override val driverValue: String? get() = autoCompleteDriverValue()
    override val driverActions get() = super.driverActions + autoCompleteDriverActions()
    override val native = AndroidAutocompleteTextView(context.activity)
    actual val content: MutableReactiveValue<String> = native.contentProperty()
    actual var keyboardHints: KeyboardHints
        get() {
            return native.keyboardHints
        }
        set(value) {
            native.keyboardHints = value
        }

    private class KiteUiStringAdapter(context: Context, resource: Int, objects: List<String>) : ArrayAdapter<String>(context, resource, objects) {
        val items: List<String> = objects
    }

    actual var suggestions: List<String>
        get() {
            return (native.adapter as KiteUiStringAdapter).items
        }
        set(value) {
            native.setAdapter(KiteUiStringAdapter(native.context, AndroidAppContext.autoCompleteLayoutResource, value))
        }

}
