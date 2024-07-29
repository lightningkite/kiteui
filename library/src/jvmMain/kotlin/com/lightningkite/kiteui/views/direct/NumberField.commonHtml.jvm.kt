package com.lightningkite.kiteui.views.direct

actual val NumberField.selectionStart: Int? get() = null
actual val NumberField.selectionEnd: Int? get() = null
actual fun NumberField.setSelectionRange(start: Int, end: Int) {}