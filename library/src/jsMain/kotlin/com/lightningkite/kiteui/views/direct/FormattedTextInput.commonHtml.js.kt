package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.InternalKiteUi
import org.w3c.dom.HTMLInputElement

@InternalKiteUi
public actual val FormattedTextInput.selectionStart: Int? get() = (native.element as? HTMLInputElement)?.selectionStart
@InternalKiteUi
public actual val FormattedTextInput.selectionEnd: Int? get() = (native.element as? HTMLInputElement)?.selectionEnd
@InternalKiteUi
public actual fun FormattedTextInput.setSelectionRange(start: Int, end: Int) { (native.element as? HTMLInputElement)?.setSelectionRange(start, end) }