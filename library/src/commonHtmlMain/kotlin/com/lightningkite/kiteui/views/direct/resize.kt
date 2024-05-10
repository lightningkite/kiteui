package com.lightningkite.kiteui.views.direct

//external class ResizeObserver(callback: (Array<ResizeObserverEntry>, observer: ResizeObserver)->Unit) {
//    fun disconnect()
//    fun observe(target: Element, options: ResizeObserverOptions = definedExternally)
//    fun unobserve(target: Element)
//}
//external interface ResizeObserverOptions {
//    val box: String
//}
//external interface ResizeObserverEntry {
//    val target: Element
//    val contentRect: DOMRectReadOnly
//    val contentBoxSize: ResizeObserverEntryBoxSize
//    val borderBoxSize: ResizeObserverEntryBoxSize
//}
//external interface ResizeObserverEntryBoxSize {
//    val blockSize: Double
//    val inlineSize: Double
//}
//
//data class SizeReader(val native: HTMLElement, val key: String): Readable<Double> {
//    override val state: ReadableState<Double>
//        get() = ReadableState(native.asDynamic()[key].unsafeCast<Int>().toDouble())
//    override fun addListener(listener: () -> Unit): () -> Unit {
//        val o = ResizeObserver { _, _ ->
//            listener()
//        }
//        o.observe(native)
//        return { o.disconnect() }
//    }
//}