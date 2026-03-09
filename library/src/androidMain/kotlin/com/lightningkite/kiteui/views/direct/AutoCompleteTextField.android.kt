package com.lightningkite.kiteui.views.direct

import android.content.Context
import android.content.res.ColorStateList
import android.text.Editable
import android.text.TextWatcher
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView as AndroidAutocompleteTextView
import android.widget.ProgressBar
import com.lightningkite.kiteui.reactive.Action
import com.lightningkite.kiteui.models.KeyboardHints
import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*


actual class AutoCompleteTextField actual constructor(context: RContext): RViewWithAction(context) {
    override val native = AndroidAutocompleteTextView(context.activity)
    actual val content: MutableReactiveValue<String> = native.contentProperty()
    actual var keyboardHints: KeyboardHints
        get() {
            return native.keyboardHints
        }
        set(value) {
            native.keyboardHints = value
        }

    private class KiteUiStringAdapter(context: Context, resource: Int, objects: List<String>) :
        ArrayAdapter<String>(context, resource, objects) {
        val items: List<String> = objects
    }

    actual var suggestions: List<String>
        get() {
            return (native.adapter as KiteUiStringAdapter).items
        }
        set(value) {
            native.setAdapter(KiteUiStringAdapter(native.context, AndroidAppContext.autoCompleteLayoutResource, value))
        }

    // by Claude
    override var accessibilityValue: String?
        get() = readStringValue(content)
        set(value) = writeStringValue(content, value)
    override val accessibilityActions get() = CLICK_AND_SET_VALUE_ACTIONS
    override fun performAccessibilityAction(action: String, value: String?) =
        performStringSetValueAction(content, action, value) { a, v -> super.performAccessibilityAction(a, v) }
}
