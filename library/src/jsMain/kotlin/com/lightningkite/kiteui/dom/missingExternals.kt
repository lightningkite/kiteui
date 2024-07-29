package com.lightningkite.kiteui.dom

external class ResizeObserver(callback: (Array<ResizeObserverEntry>, observer: ResizeObserver)->Unit) {
    fun disconnect()
    fun observe(target: Element, options: ResizeObserverOptions = definedExternally)
    fun unobserve(target: Element)
}
external interface ResizeObserverOptions {
    val box: String
}
external interface ResizeObserverEntry {
    val target: Element
    val contentRect: DOMRectReadOnly
    val contentBoxSize: ResizeObserverEntryBoxSize
    val borderBoxSize: ResizeObserverEntryBoxSize
}
external interface ResizeObserverEntryBoxSize {
    val blockSize: Double
    val inlineSize: Double
}