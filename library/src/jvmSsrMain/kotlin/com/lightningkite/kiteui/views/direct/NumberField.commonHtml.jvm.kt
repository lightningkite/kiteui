package com.lightningkite.kiteui.views.direct

public actual val NumberInput.selectionStart: Int? get() = null
public actual val NumberInput.selectionEnd: Int? get() = null
public actual fun NumberInput.setSelectionRange(start: Int, end: Int) {}

public actual fun usingWebOnMobile(): Boolean = false