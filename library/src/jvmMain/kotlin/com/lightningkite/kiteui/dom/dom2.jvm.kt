package com.lightningkite.kiteui.dom

public actual abstract class EventTarget
public actual open class Event {
    public actual fun preventDefault(){}
    public actual fun stopPropagation(){}
    public actual fun stopImmediatePropagation(){}
    public actual open val target: EventTarget? = null
}
public actual open class UIEvent: Event() {
}
public actual open class KeyboardEvent: UIEvent() {
    public actual open val code: String = ""
    public actual open val ctrlKey: Boolean = false
    public actual open val shiftKey: Boolean = false
    public actual open val altKey: Boolean = false
    public actual open val metaKey: Boolean = false
}
public actual open class WheelEvent: MouseEvent() {
    public actual val deltaX: Double = 0.0
    public actual val deltaY: Double = 0.0
    public actual val deltaZ: Double = 0.0
}
public actual open class MouseEvent: UIEvent() {
    public actual val pageX: Double = 0.0
    public actual val pageY: Double = 0.0
}
public actual open class PointerEvent: MouseEvent() {
    public actual val pointerId: Int = 0
}

public actual abstract class Node: EventTarget() {
    public actual fun replaceChild(node: Node, child: Node): Node = throw NotImplementedError()
    public actual fun appendChild(node: Node): Node = throw NotImplementedError()
    public actual open var nodeValue: String? = throw NotImplementedError()
}
public actual abstract class Element: Node() {
    //    open val tagName: String
//    open var className: String  // class
//    open var id: String  // id
//    open var slot: String  // slot
    public actual fun getBoundingClientRect(): DOMRect = throw NotImplementedError()
}
public actual open class DOMRectReadOnly {
    public actual open val x: Double = 0.0
    public actual open val y: Double = 0.0
    public actual open val width: Double = 0.0
    public actual open val height: Double = 0.0
    public actual open val top: Double = 0.0
    public actual open val right: Double = 0.0
    public actual open val bottom: Double = 0.0
    public actual open val left: Double = 0.0
}
public actual open class DOMRect : DOMRectReadOnly() {
    public actual override var x: Double = 0.0
    public actual override var y: Double = 0.0
    public actual override var width: Double = 0.0
    public actual override var height: Double = 0.0
}