package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.InternalKiteUi

@InternalKiteUi
public actual val NumberInput.selectionStart: Int? get() = null
@InternalKiteUi
public actual val NumberInput.selectionEnd: Int? get() = null
@InternalKiteUi
public actual fun NumberInput.setSelectionRange(start: Int, end: Int) {}

@InternalKiteUi
public actual fun usingWebOnMobile(): Boolean = false