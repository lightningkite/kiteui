package com.lightningkite.kiteui.dom

import kotlinx.browser.document
import org.w3c.dom.DOMRect
import org.w3c.dom.get
import org.w3c.dom.pointerevents.PointerEvent

actual typealias EventTarget = org.w3c.dom.events.EventTarget
actual typealias Event = org.w3c.dom.events.Event
actual typealias UIEvent = org.w3c.dom.events.UIEvent
actual typealias KeyboardEvent = org.w3c.dom.events.KeyboardEvent
actual typealias WheelEvent = org.w3c.dom.events.WheelEvent
actual typealias PointerEvent = org.w3c.dom.pointerevents.PointerEvent
actual typealias MouseEvent = org.w3c.dom.events.MouseEvent
actual typealias Node = org.w3c.dom.Node
actual typealias Element = org.w3c.dom.Element
actual typealias DOMRectReadOnly = org.w3c.dom.DOMRectReadOnly
actual typealias DOMRect = org.w3c.dom.DOMRect