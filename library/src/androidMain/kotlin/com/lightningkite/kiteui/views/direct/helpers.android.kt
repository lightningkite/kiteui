package com.lightningkite.kiteui.views.direct

import android.app.Activity
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.CompoundButton
import android.widget.EditText
import androidx.core.content.getSystemService
import com.lightningkite.kiteui.reactive.*

fun EditText.contentProperty(): ImmediateWritable<String> = object : ImmediateWritable<String>, BaseListenable(), TextWatcher {
    override fun afterTextChanged(s: Editable?) { invokeAllListeners() }
    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    override var value: String
        get() = text?.toString() ?: ""
        set(value) {  setText(value) }
    override suspend fun set(value: String) { this.value = value }
    init { addTextChangedListener(this) }
}
fun CompoundButton.contentProperty(): ImmediateWritable<Boolean> = object : ImmediateWritable<Boolean>, BaseListenable(), CompoundButton.OnCheckedChangeListener {
    override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) { invokeAllListeners() }
    override var value: Boolean
        get() = isChecked
        set(value) { isChecked = value }
    override suspend fun set(value: Boolean) { isChecked = value }
    init { setOnCheckedChangeListener(this) }
}
fun EditText.focusIsKeyboard(): EditText {
    onFocusChangeListener = object: View.OnFocusChangeListener {
        override fun onFocusChange(v: View, hasFocus: Boolean) {
            if (hasFocus)
                v.context.getSystemService<InputMethodManager>()?.showSoftInput(v, InputMethodManager.SHOW_IMPLICIT)
            else if((v.context as? Activity)?.currentFocus == null) {
                v.context.getSystemService<InputMethodManager>()?.hideSoftInputFromWindow(v.windowToken, InputMethodManager.HIDE_IMPLICIT_ONLY)
            }
        }
    }
    return this
}