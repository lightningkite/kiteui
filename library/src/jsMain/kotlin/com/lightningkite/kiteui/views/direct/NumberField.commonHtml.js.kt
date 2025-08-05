package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.InternalKiteUi
import kotlinx.browser.window
import org.w3c.dom.HTMLInputElement

@InternalKiteUi
public actual val NumberInput.selectionStart: Int? get() = (native.element as? HTMLInputElement)?.selectionStart
@InternalKiteUi
public actual val NumberInput.selectionEnd: Int? get() = (native.element as? HTMLInputElement)?.selectionEnd
@InternalKiteUi
public actual fun NumberInput.setSelectionRange(start: Int, end: Int) { (native.element as? HTMLInputElement)?.setSelectionRange(start, end) }

@InternalKiteUi
public actual fun usingWebOnMobile(): Boolean = window.navigator.userAgent.contains("Mobi")