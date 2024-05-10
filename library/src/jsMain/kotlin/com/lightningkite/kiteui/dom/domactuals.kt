package com.lightningkite.kiteui.dom

import kotlinx.browser.document
import org.w3c.dom.DOMRect
import org.w3c.dom.get

actual typealias CSSStyleDeclaration = org.w3c.dom.css.CSSStyleDeclaration
actual typealias Node = org.w3c.dom.Node
actual typealias CharacterData = org.w3c.dom.CharacterData
actual typealias Text = org.w3c.dom.Text
actual typealias Element = org.w3c.dom.Element
actual typealias DOMRect = org.w3c.dom.DOMRect
actual typealias DOMRectReadOnly = org.w3c.dom.DOMRectReadOnly
actual typealias HTMLElement = org.w3c.dom.HTMLElement
actual typealias HTMLAnchorElement = org.w3c.dom.HTMLAnchorElement
actual typealias HTMLAppletElement = org.w3c.dom.HTMLAppletElement
actual typealias HTMLAreaElement = org.w3c.dom.HTMLAreaElement
actual typealias HTMLMediaElement = org.w3c.dom.HTMLMediaElement
actual typealias HTMLAudioElement = org.w3c.dom.HTMLAudioElement
actual typealias HTMLBaseElement = org.w3c.dom.HTMLBaseElement
actual typealias HTMLQuoteElement = org.w3c.dom.HTMLQuoteElement
actual typealias HTMLBodyElement = org.w3c.dom.HTMLBodyElement
actual typealias HTMLButtonElement = org.w3c.dom.HTMLButtonElement
actual typealias HTMLCanvasElement = org.w3c.dom.HTMLCanvasElement
actual typealias HTMLTableCaptionElement = org.w3c.dom.HTMLTableCaptionElement
actual typealias HTMLTableColElement = org.w3c.dom.HTMLTableColElement
actual typealias HTMLDataElement = org.w3c.dom.HTMLDataElement
actual typealias HTMLModElement = org.w3c.dom.HTMLModElement
actual typealias HTMLDetailsElement = org.w3c.dom.HTMLDetailsElement
actual typealias HTMLDialogElement = org.w3c.dom.HTMLDialogElement
actual typealias HTMLEmbedElement = org.w3c.dom.HTMLEmbedElement
actual typealias HTMLFieldSetElement = org.w3c.dom.HTMLFieldSetElement
actual typealias HTMLFontElement = org.w3c.dom.HTMLFontElement
actual typealias HTMLFormElement = org.w3c.dom.HTMLFormElement
actual typealias HTMLHRElement = org.w3c.dom.HTMLHRElement
actual typealias HTMLIFrameElement = org.w3c.dom.HTMLIFrameElement
actual typealias HTMLImageElement = org.w3c.dom.HTMLImageElement
actual typealias HTMLInputElement = org.w3c.dom.HTMLInputElement
actual typealias HTMLKeygenElement = org.w3c.dom.HTMLKeygenElement
actual typealias HTMLLabelElement = org.w3c.dom.HTMLLabelElement
actual typealias HTMLLIElement = org.w3c.dom.HTMLLIElement
actual typealias HTMLLinkElement = org.w3c.dom.HTMLLinkElement
actual typealias HTMLMapElement = org.w3c.dom.HTMLMapElement
actual typealias HTMLMarqueeElement = org.w3c.dom.HTMLMarqueeElement
actual typealias HTMLMenuElement = org.w3c.dom.HTMLMenuElement
actual typealias HTMLMetaElement = org.w3c.dom.HTMLMetaElement
actual typealias HTMLMeterElement = org.w3c.dom.HTMLMeterElement
actual typealias HTMLObjectElement = org.w3c.dom.HTMLObjectElement
actual typealias HTMLOListElement = org.w3c.dom.HTMLOListElement
actual typealias HTMLOptGroupElement = org.w3c.dom.HTMLOptGroupElement
actual typealias HTMLOptionElement = org.w3c.dom.HTMLOptionElement
actual typealias HTMLOutputElement = org.w3c.dom.HTMLOutputElement
actual typealias HTMLParamElement = org.w3c.dom.HTMLParamElement
actual typealias HTMLProgressElement = org.w3c.dom.HTMLProgressElement
actual typealias HTMLScriptElement = org.w3c.dom.HTMLScriptElement
actual typealias HTMLSelectElement = org.w3c.dom.HTMLSelectElement
actual typealias HTMLSourceElement = org.w3c.dom.HTMLSourceElement
actual typealias HTMLStyleElement = org.w3c.dom.HTMLStyleElement
actual typealias HTMLTableElement = org.w3c.dom.HTMLTableElement
actual typealias HTMLTableSectionElement = org.w3c.dom.HTMLTableSectionElement
actual typealias HTMLTableCellElement = org.w3c.dom.HTMLTableCellElement
actual typealias HTMLTextAreaElement = org.w3c.dom.HTMLTextAreaElement
actual typealias HTMLTimeElement = org.w3c.dom.HTMLTimeElement
actual typealias HTMLTableRowElement = org.w3c.dom.HTMLTableRowElement
actual typealias HTMLTrackElement = org.w3c.dom.HTMLTrackElement
actual typealias HTMLVideoElement = org.w3c.dom.HTMLVideoElement

actual inline fun createElement(tagName: String): HTMLElement = document.createElement(tagName) as HTMLElement
actual inline fun HTMLElement.getChild(index: Int): Node? = children.get(index)

actual inline fun HTMLElement.addOnClick(crossinline action: () -> Unit) {
    this.addEventListener("click", { action() })
}
actual inline fun HTMLInputElement.addOnValue(crossinline action: (string: String) -> Unit) {
    this.addEventListener("input", { action(value) })
}
actual inline fun HTMLInputElement.addOnChange(crossinline action: (on: Boolean) -> Unit) {
    this.addEventListener("change", { action(checked) })
}

actual inline fun HTMLTextAreaElement.addOnValue(crossinline action: (string: String) -> Unit) {
    this.addEventListener("input", { action(value) })
}
