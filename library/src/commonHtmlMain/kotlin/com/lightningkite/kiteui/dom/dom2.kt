package com.lightningkite.kiteui.dom

expect abstract class EventTarget
expect open class Event {
    fun preventDefault()
    open val target: EventTarget?
}
expect open class UIEvent: Event {
}
expect open class KeyboardEvent: UIEvent {
    val code: String
}
expect open class WheelEvent: MouseEvent {
    val deltaX: Double
    val deltaY: Double
    val deltaZ: Double
}
expect open class MouseEvent: UIEvent {
    val pageX: Double
    val pageY: Double
}
expect open class PointerEvent: MouseEvent {
    val pointerId: Int
}

expect abstract class Node: EventTarget {
    fun replaceChild(node: Node, child: Node): Node
    fun appendChild(node: Node): Node
    open var nodeValue: String?
}
expect abstract class Element: Node {
//    open val tagName: String
//    open var className: String  // class
//    open var id: String  // id
//    open var slot: String  // slot
    fun getBoundingClientRect(): DOMRect
}
expect open class DOMRectReadOnly {
    open val x: Double
    open val y: Double
    open val width: Double
    open val height: Double
    open val top: Double
    open val right: Double
    open val bottom: Double
    open val left: Double
}
expect open class DOMRect : DOMRectReadOnly {
    override var x: Double
    override var y: Double
    override var width: Double
    override var height: Double
}