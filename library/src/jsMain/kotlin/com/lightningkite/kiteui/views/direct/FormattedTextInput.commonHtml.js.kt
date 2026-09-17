package com.lightningkite.kiteui.views.direct

import org.w3c.dom.HTMLInputElement

public actual val FormattedTextInput.selectionStart: Int? get() = (native.element as? HTMLInputElement)?.selectionStart
public actual val FormattedTextInput.selectionEnd: Int? get() = (native.element as? HTMLInputElement)?.selectionEnd
public actual fun FormattedTextInput.setSelectionRange(start: Int, end: Int) { (native.element as? HTMLInputElement)?.setSelectionRange(start, end) }