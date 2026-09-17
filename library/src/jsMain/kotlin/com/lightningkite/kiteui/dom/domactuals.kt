package com.lightningkite.kiteui.dom

import kotlinx.browser.document
import org.w3c.dom.DOMRect
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