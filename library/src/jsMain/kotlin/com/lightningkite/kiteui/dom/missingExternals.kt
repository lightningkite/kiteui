package com.lightningkite.kiteui.dom

public external class ResizeObserver(callback: (Array<ResizeObserverEntry>, observer: ResizeObserver)->Unit) {
    public fun disconnect()
    public fun observe(target: DOMElement, options: ResizeObserverOptions = definedExternally)
    public fun unobserve(target: DOMElement)
}
public external interface ResizeObserverOptions {
    public val box: String
}
public external interface ResizeObserverEntry {
    public val target: DOMElement
    public val contentRect: DOMRectReadOnly
    public val contentBoxSize: ResizeObserverEntryBoxSize
    public val borderBoxSize: ResizeObserverEntryBoxSize
}
public external interface ResizeObserverEntryBoxSize {
    public val blockSize: Double
    public val inlineSize: Double
}