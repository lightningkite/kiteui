package com.lightningkite.kiteui.views.direct

import android.text.Editable
import android.text.TextWatcher
import android.widget.CompoundButton
import android.widget.EditText
import com.lightningkite.kiteui.reactive.*

fun EditText.contentProperty(): Writable<String> = object : Writable<String>, BaseListenable(), TextWatcher {
    override fun afterTextChanged(s: Editable?) { invokeAll() }
    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    override val state get() = ReadableState(text?.toString() ?: "")
    override suspend fun set(value: String) { setText(value) }
    init { addTextChangedListener(this) }
}
fun CompoundButton.contentProperty(): Writable<Boolean> = object : Writable<Boolean>, BaseListenable(), CompoundButton.OnCheckedChangeListener {
    override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) { invokeAll() }
    override val state get() = ReadableState(isChecked)
    override suspend fun set(value: Boolean) { isChecked = value }
    init { setOnCheckedChangeListener(this) }
}