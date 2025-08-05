package com.lightningkite.kiteui.dom

import com.lightningkite.kiteui.InternalKiteUi
import kotlinx.browser.document
import org.w3c.dom.DOMRect
import org.w3c.dom.get
import org.w3c.dom.pointerevents.PointerEvent

@InternalKiteUi
public actual typealias EventTarget = org.w3c.dom.events.EventTarget
@InternalKiteUi
public actual typealias Event = org.w3c.dom.events.Event
@InternalKiteUi
public actual typealias UIEvent = org.w3c.dom.events.UIEvent
@InternalKiteUi
public actual typealias KeyboardEvent = org.w3c.dom.events.KeyboardEvent
@InternalKiteUi
public actual typealias WheelEvent = org.w3c.dom.events.WheelEvent
@InternalKiteUi
public actual typealias PointerEvent = org.w3c.dom.pointerevents.PointerEvent
@InternalKiteUi
public actual typealias MouseEvent = org.w3c.dom.events.MouseEvent
@InternalKiteUi
public actual typealias Node = org.w3c.dom.Node
@InternalKiteUi
public actual typealias Element = org.w3c.dom.Element
@InternalKiteUi
public actual typealias DOMRectReadOnly = org.w3c.dom.DOMRectReadOnly
@InternalKiteUi
public actual typealias DOMRect = org.w3c.dom.DOMRect