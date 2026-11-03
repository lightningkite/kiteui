package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.views.HtmlElementLike
import org.w3c.dom.DOMRectReadOnly
import org.w3c.dom.Element
import org.w3c.dom.HTMLElement
import org.w3c.dom.MutationObserver

public external class ResizeObserver(callback: (Array<ResizeObserverEntry>, observer: ResizeObserver) -> Unit) {
    public fun disconnect()
    public fun observe(target: Element, options: ResizeObserverOptions = definedExternally)
    public fun unobserve(target: Element)
}

public external interface ResizeObserverOptions {
    public val box: String
}

public external interface ResizeObserverEntry {
    public val target: Element
    public val contentRect: DOMRectReadOnly
    public val contentBoxSize: ResizeObserverEntryBoxSize
    public val borderBoxSize: ResizeObserverEntryBoxSize
}

public external interface ResizeObserverEntryBoxSize {
    public val blockSize: Double
    public val inlineSize: Double
}
