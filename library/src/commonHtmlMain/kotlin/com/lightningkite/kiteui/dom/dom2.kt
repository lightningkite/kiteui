package com.lightningkite.kiteui.dom

public expect abstract class EventTarget
public expect open class Event {
    public fun preventDefault()
    public fun stopPropagation()
    public fun stopImmediatePropagation()
    public open val target: EventTarget?
}
public expect open class UIEvent: Event {
}
public expect open class KeyboardEvent: UIEvent {
    public open val code: String
    public open val ctrlKey: Boolean
    public open val shiftKey: Boolean
    public open val altKey: Boolean
    public open val metaKey: Boolean
}
public expect open class WheelEvent: MouseEvent {
    public val deltaX: Double
    public val deltaY: Double
    public val deltaZ: Double
}
public expect open class MouseEvent: UIEvent {
    public val pageX: Double
    public val pageY: Double
}
public expect open class PointerEvent: MouseEvent {
    public val pointerId: Int
}

public expect abstract class Node: EventTarget {
    public fun replaceChild(node: Node, child: Node): Node
    public fun appendChild(node: Node): Node
    public open var nodeValue: String?
}
public expect abstract class Element: Node {
//    open val tagName: String
//    open var className: String  // class
//    open var id: String  // id
//    open var slot: String  // slot
    public fun getBoundingClientRect(): DOMRect
}
public expect open class DOMRectReadOnly {
    public open val x: Double
    public open val y: Double
    public open val width: Double
    public open val height: Double
    public open val top: Double
    public open val right: Double
    public open val bottom: Double
    public open val left: Double
}
public expect open class DOMRect : DOMRectReadOnly {
    public override var x: Double
    public override var y: Double
    public override var width: Double
    public override var height: Double
}