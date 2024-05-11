package com.lightningkite.kiteui.views.direct

import org.w3c.dom.HTMLInputElement

actual val NumberField.selectionStart: Int? get() = (native.element as? HTMLInputElement)?.selectionStart
actual val NumberField.selectionEnd: Int? get() = (native.element as? HTMLInputElement)?.selectionEnd
actual fun NumberField.setSelectionRange(start: Int, end: Int) { (native.element as? HTMLInputElement)?.setSelectionRange(start, end) }