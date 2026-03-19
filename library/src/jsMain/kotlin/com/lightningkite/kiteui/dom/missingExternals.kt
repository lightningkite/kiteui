package com.lightningkite.kiteui.dom

external class ResizeObserver(callback: (Array<ResizeObserverEntry>, observer: ResizeObserver)->Unit) {
    fun disconnect()
    fun observe(target: DOMElement, options: ResizeObserverOptions = definedExternally)
    fun unobserve(target: DOMElement)
}
external interface ResizeObserverOptions {
    val box: String
}
external interface ResizeObserverEntry {
    val target: DOMElement
    val contentRect: DOMRectReadOnly
    val contentBoxSize: ResizeObserverEntryBoxSize
    val borderBoxSize: ResizeObserverEntryBoxSize
}
external interface ResizeObserverEntryBoxSize {
    val blockSize: Double
    val inlineSize: Double
}