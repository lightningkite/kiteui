package com.lightningkite.kiteui.views.direct

import kotlinx.browser.window
import org.w3c.dom.HTMLInputElement

public actual val NumberInput.selectionStart: Int? get() = (native.element as? HTMLInputElement)?.selectionStart
public actual val NumberInput.selectionEnd: Int? get() = (native.element as? HTMLInputElement)?.selectionEnd
public actual fun NumberInput.setSelectionRange(start: Int, end: Int) { (native.element as? HTMLInputElement)?.setSelectionRange(start, end) }

public actual fun usingWebOnMobile(): Boolean = window.navigator.userAgent.contains("Mobi")