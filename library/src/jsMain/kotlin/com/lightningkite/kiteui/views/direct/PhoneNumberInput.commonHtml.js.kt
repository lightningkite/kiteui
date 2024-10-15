package com.lightningkite.kiteui.views.direct

import org.w3c.dom.HTMLInputElement

actual val PhoneNumberInput.selectionStart: Int? get() = (native.element as? HTMLInputElement)?.selectionStart
actual val PhoneNumberInput.selectionEnd: Int? get() = (native.element as? HTMLInputElement)?.selectionEnd
actual fun PhoneNumberInput.setSelectionRange(start: Int, end: Int) { (native.element as? HTMLInputElement)?.setSelectionRange(start, end) }