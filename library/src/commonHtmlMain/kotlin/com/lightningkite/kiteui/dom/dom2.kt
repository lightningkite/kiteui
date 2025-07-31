package com.lightningkite.kiteui.dom

public expect abstract class EventTarget
public expect open class Event {
    fun preventDefault()
    fun stopPropagation()
    fun stopImmediatePropagation()
    open val target: EventTarget?
}
public expect open class UIEvent: Event {
}
public expect open class KeyboardEvent: UIEvent {
    open val code: String
    open val ctrlKey: Boolean
    open val shiftKey: Boolean
    open val altKey: Boolean
    open val metaKey: Boolean
}
public expect open class WheelEvent: MouseEvent {
    val deltaX: Double
    val deltaY: Double
    val deltaZ: Double
}
public expect open class MouseEvent: UIEvent {
    val pageX: Double
    val pageY: Double
}
public expect open class PointerEvent: MouseEvent {
    val pointerId: Int
}

public expect abstract class Node: EventTarget {
    fun replaceChild(node: Node, child: Node): Node
    fun appendChild(node: Node): Node
    open var nodeValue: String?
}
public expect abstract class Element: Node {
//    open val tagName: String
//    open var className: String  // class
//    open var id: String  // id
//    open var slot: String  // slot
    fun getBoundingClientRect(): DOMRect
}
public expect open class DOMRectReadOnly {
    open val x: Double
    open val y: Double
    open val width: Double
    open val height: Double
    open val top: Double
    open val right: Double
    open val bottom: Double
    open val left: Double
}
public expect open class DOMRect : DOMRectReadOnly {
    override var x: Double
    override var y: Double
    override var width: Double
    override var height: Double
}