package com.lightningkite.kiteui.dom

import com.lightningkite.kiteui.navigation.UrlLikePath
import com.lightningkite.kiteui.navigation.urlLike
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.DOMRect
import org.w3c.dom.HTMLAnchorElement
import org.w3c.dom.get
import org.w3c.dom.pointerevents.PointerEvent

public actual typealias EventTarget = org.w3c.dom.events.EventTarget
public actual typealias Event = org.w3c.dom.events.Event
public actual typealias UIEvent = org.w3c.dom.events.UIEvent
public actual typealias KeyboardEvent = org.w3c.dom.events.KeyboardEvent
public actual typealias WheelEvent = org.w3c.dom.events.WheelEvent
public actual typealias PointerEvent = org.w3c.dom.pointerevents.PointerEvent
public actual typealias MouseEvent = org.w3c.dom.events.MouseEvent
public actual typealias Node = org.w3c.dom.Node
public actual typealias DOMElement = org.w3c.dom.Element
public actual typealias DOMRectReadOnly = org.w3c.dom.DOMRectReadOnly
public actual typealias DOMRect = org.w3c.dom.DOMRect

internal actual fun Event.clickedSameOriginLink(): UrlLikePath? {
    // Modifier clicks ask the browser for a new tab or window.
    if (this is MouseEvent && (ctrlKey || metaKey || shiftKey || altKey)) return null
    val link = (target as? DOMElement)?.closest("a") as? HTMLAnchorElement ?: return null
    if (link.target == "_blank") return null
    return appPathOf(link.href)
}

internal actual fun appPathOf(href: String): UrlLikePath? {
    // An anchor resolves href exactly as the rendered link will, and unlike URL() doesn't throw on a malformed one.
    val link = document.createElement("a") as HTMLAnchorElement
    link.href = href
    if (link.origin != window.location.origin) return null
    return urlLike(link.pathname, link.search)
}