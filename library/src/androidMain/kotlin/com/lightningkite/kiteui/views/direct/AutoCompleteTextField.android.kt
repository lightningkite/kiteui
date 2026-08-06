package com.lightningkite.kiteui.views.direct

import android.util.TypedValue
import android.widget.ArrayAdapter
import androidx.core.graphics.TypefaceCompat
import android.widget.AutoCompleteTextView as AndroidAutocompleteTextView
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.reactive.core.*


public actual class AutoCompleteTextField actual constructor(context: ElementContext): NativeElementWithAction(context) {
    override val driverValue: String? get() = autoCompleteDriverValue()
    override val driverActions: Map<String, suspend (List<String>) -> String> get() = super.driverActions + autoCompleteDriverActions()
    override val native: AndroidAutocompleteTextView = AndroidAutocompleteTextView(context.activity)
    public actual val content: MutableReactiveValue<String> = native.contentProperty()

    /**
     * Mirrors [TextInput]'s theming. `AutoCompleteTextView` is an `EditText`, but this class does
     * not extend [TextInput], so none of that was inherited - the field was left with the platform
     * defaults, which is why hints (and text) rendered black whatever the theme said.
     */
    override fun nativeApplyTheme(theme: ThemeAndBack) {
        super.nativeApplyTheme(theme)
        val t = theme.theme
        native.setTextColor(t.foreground.colorInt())
        // Half-strength foreground, the same relationship TextInput uses, so a hint stays legible
        // on a dark theme instead of falling back to the platform's fixed grey.
        native.setHintTextColor(t.foreground.closestColor().withAlpha(0.5f).colorInt())
        native.setTypeface(
            TypefaceCompat.create(native.context, t.font.font.toTypeface(), t.font.weight, t.font.italic)
        )
        native.setTextSize(TypedValue.COMPLEX_UNIT_PX, t.font.size.value)
    }
    /** Matches [TextField.hint]; `AutoCompleteTextView` is an `EditText`, so this is native. */
    public actual var hint: String
        get() = native.hint?.toString() ?: ""
        set(value) {
            native.hint = value
        }

    public actual var keyboardHints: KeyboardHints
        get() {
            return native.keyboardHints
        }
        set(value) {
            native.keyboardHints = value
        }

    // Backed by a plain field rather than read back from `native.adapter`: reading the adapter
    // back requires it to have been set at least once, and a `suggestions` read before any write
    // (e.g. a caller inspecting the current list before deciding whether to replace it) would
    // otherwise throw - `native.adapter` is null until the first `setAdapter` call.
    public actual var suggestions: List<String> = listOf()
        set(value) {
            field = value
            native.setAdapter(ArrayAdapter(native.context, AndroidAppContext.autoCompleteLayoutResource, value))
        }

}
