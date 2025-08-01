package com.lightningkite.kiteui.dom

import com.lightningkite.kiteui.InternalKiteUi

@InternalKiteUi
public external class ResizeObserver(callback: (Array<ResizeObserverEntry>, observer: ResizeObserver)->Unit) {
    public fun disconnect()
    public fun observe(target: Element, options: ResizeObserverOptions = definedExternally)
    public fun unobserve(target: Element)
}

@InternalKiteUi
public external interface ResizeObserverOptions {
    public val box: String
}
@InternalKiteUi
public external interface ResizeObserverEntry {
    public val target: Element
    public val contentRect: DOMRectReadOnly
    public val contentBoxSize: ResizeObserverEntryBoxSize
    public val borderBoxSize: ResizeObserverEntryBoxSize
}
@InternalKiteUi
public external interface ResizeObserverEntryBoxSize {
    public val blockSize: Double
    public val inlineSize: Double
}