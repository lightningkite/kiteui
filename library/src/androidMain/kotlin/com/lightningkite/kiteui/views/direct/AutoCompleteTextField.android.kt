package com.lightningkite.kiteui.views.direct

import android.content.Context
import android.content.res.ColorStateList
import android.text.Editable
import android.text.TextWatcher
import android.widget.ArrayAdapter
import android.widget.ProgressBar
import android.widget.AutoCompleteTextView as AndroidAutocompleteTextView
import com.lightningkite.kiteui.models.Action
import com.lightningkite.kiteui.models.KeyboardHints
import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.reactive.ImmediateWritable
import com.lightningkite.kiteui.reactive.Property
import com.lightningkite.kiteui.reactive.Writable
import com.lightningkite.kiteui.views.*


actual class AutoCompleteTextField actual constructor(context: RContext): RView(context) {
    override val native = AndroidAutocompleteTextView(context.activity)
    actual val content: ImmediateWritable<String> = native.contentProperty()
    actual var keyboardHints: KeyboardHints
        get() {
            return native.keyboardHints
        }
        set(value) {
            native.keyboardHints = value
        }
    actual var action: Action?
        get() {
            return native.tag as? Action
        }
        set(value) {
            native.tag = value
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

    override fun applyForeground(theme: Theme) {
    }

    override fun applyBackground(theme: Theme, fullyApply: Boolean) {

    }
}
