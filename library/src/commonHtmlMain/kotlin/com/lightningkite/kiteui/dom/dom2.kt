package com.lightningkite.kiteui.dom

abstract class HtmlElement {
    val attributes = HashMap<String, String>()
    val events = HashMap<String, (Event)->Unit>()
    val classes = ArrayList<String>()
    val id: String? = null
    val children = ArrayList<HTMLElement>()
    val content: String? = null
}

expect interface Event {
    fun preventDefault()
}
expect interface KeyboardEvent: Event {
    val code: String
}
expect interface WheelEvent: Event {
    val deltaX: Double
    val deltaY: Double
    val deltaZ: Double
}
expect interface PointerEvent: Event {
    val pointerId: Int
    val pageX: Double
    val pageY: Double
}
