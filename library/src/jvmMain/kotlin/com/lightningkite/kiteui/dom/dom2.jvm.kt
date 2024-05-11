package com.lightningkite.kiteui.dom

actual abstract class EventTarget
actual open class Event {
    actual fun preventDefault(){}
    actual open val target: EventTarget? = null
}
actual open class UIEvent: Event() {
}
actual open class KeyboardEvent: UIEvent() {
    actual val code: String = ""
}
actual open class WheelEvent: MouseEvent() {
    actual val deltaX: Double = 0.0
    actual val deltaY: Double = 0.0
    actual val deltaZ: Double = 0.0
}
actual open class MouseEvent: UIEvent() {
    actual val pageX: Double = 0.0
    actual val pageY: Double = 0.0
}
actual open class PointerEvent: MouseEvent() {
    actual val pointerId: Int = 0
}

actual abstract class Node: EventTarget() {
    actual fun replaceChild(node: Node, child: Node): Node = throw NotImplementedError()
    actual fun appendChild(node: Node): Node = throw NotImplementedError()
    actual open var nodeValue: String? = throw NotImplementedError()
}
actual abstract class Element: Node() {
    //    open val tagName: String
//    open var className: String  // class
//    open var id: String  // id
//    open var slot: String  // slot
    actual fun getBoundingClientRect(): DOMRect = throw NotImplementedError()
}
actual open class DOMRectReadOnly {
    actual open val x: Double = 0.0
    actual open val y: Double = 0.0
    actual open val width: Double = 0.0
    actual open val height: Double = 0.0
    actual open val top: Double = 0.0
    actual open val right: Double = 0.0
    actual open val bottom: Double = 0.0
    actual open val left: Double = 0.0
}
actual open class DOMRect : DOMRectReadOnly() {
    actual override var x: Double = 0.0
    actual override var y: Double = 0.0
    actual override var width: Double = 0.0
    actual override var height: Double = 0.0
}