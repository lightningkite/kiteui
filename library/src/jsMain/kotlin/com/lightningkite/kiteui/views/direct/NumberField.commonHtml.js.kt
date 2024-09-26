package com.lightningkite.kiteui.views.direct

import org.w3c.dom.HTMLInputElement

actual val NumberInput.selectionStart: Int? get() = (native.element as? HTMLInputElement)?.selectionStart
actual val NumberInput.selectionEnd: Int? get() = (native.element as? HTMLInputElement)?.selectionEnd
actual fun NumberInput.setSelectionRange(start: Int, end: Int) { (native.element as? HTMLInputElement)?.setSelectionRange(start, end) }