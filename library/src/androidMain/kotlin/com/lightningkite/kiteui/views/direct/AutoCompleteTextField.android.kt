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
import com.lightningkite.signal.ImmediateWritable
import com.lightningkite.signal.Property
import com.lightningkite.signal.Writable
import com.lightningkite.kiteui.views.*


public actual class AutoCompleteTextField public actual constructor(context: RContext): RViewWithAction(context) {
    override val native = AndroidAutocompleteTextView(context.activity)
    public actual val content: ImmediateWritable<String> = native.contentProperty()
    public actual var keyboardHints: KeyboardHints
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

    public actual var suggestions: List<String>
        get() {
            return (native.adapter as KiteUiStringAdapter).items
        }
        set(value) {
            native.setAdapter(KiteUiStringAdapter(native.context, AndroidAppContext.autoCompleteLayoutResource, value))
        }

}
