package com.lightningkite.kiteui.views.direct

import android.widget.EditText
import android.widget.TextView
import com.lightningkite.kiteui.models.KeyboardHints
import com.lightningkite.kiteui.reactive.Writable
import com.lightningkite.kiteui.views.ViewDsl
import com.lightningkite.kiteui.views.ViewWriter

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual typealias NTextArea = EditText

actual val TextArea.content: Writable<String>
    get() {
        return this@content.native.content
    }
actual var TextArea.keyboardHints: KeyboardHints
    get() {
        return native.keyboardHints
    }
    set(value) {
        native.keyboardHints = value
    }
actual var TextArea.hint: String
    get() {
        return this@hint.native.hint.toString()
    }
    set(value) {
        this@hint.native.hint = value
    }

@ViewDsl
actual inline fun ViewWriter.textAreaActual(crossinline setup: TextArea.() -> Unit) {
    return viewElement(factory = ::EditText, wrapper = ::TextArea, setup = {
        handleTheme<TextView>(native, foreground = applyTextColorFromTheme, viewLoads = true) {
            setup(this)
        }
    })
}