package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.models.*
import com.lightningkite.reactive.context.StatusListener
import kotlinx.coroutines.CoroutineScope

fun ElementWriter.split(): ElementWriter = ElementWriter.Split(this)

fun ElementWriter.beforeNextElementSetup(setup: Element.() -> Unit): ElementWriter = ElementWriter.BeforeSetup(this, setup)

inline fun Element.withoutLoadingAnimations(block: CoroutineScope.() -> Unit) {
    CoroutineScope(coroutineContext.minusKey(StatusListener.Key)).run(block)
}

val Element.theme: Theme get() = themeAndBack.theme

var Element.padding: Dimension?
    get() = paddingByEdge?.left
    set(value) { paddingByEdge = value?.let(::Edges) }




internal fun Element.defaultDriverActions(): Map<String, suspend (List<String>) -> String> = buildMap {
    put("scrollIntoView") {
        scrollIntoView(Align.Center, Align.Center, animate = false)
        "OK"
    }
    if (dragData != null) put("getDragData") {
        val data = dragData ?: throw DriverActionException("no dragData on this view")
        val serialized = buildString {
            append(data.label)
            for ((mime, value) in data.typeToData) {
                append('\u0000'); append(mime); append('\u0000'); append(value)
            }
        }
        kotlin.io.encoding.Base64.encode(serialized.encodeToByteArray())
    }
    if (dropTargetDelegate != null) put("drop") { args ->
        val encoded = args.firstOrNull() ?: throw DriverActionException("drop requires base64 drag data argument")
        val decoded = kotlin.io.encoding.Base64.decode(encoded).decodeToString()
        val parts = decoded.split('\u0000')
        if(parts.size % 2 == 0) throw DriverActionException("invalid drag data format: expected label followed by mime/value pairs")
        val label = parts[0]
        val typeToData = (1 until parts.size step 2).associate { parts[it] to parts[it + 1] }
        val dragData = DragData(label, typeToData)
        val delegate = dropTargetDelegate ?: throw DriverActionException("no drop target found on this view or ancestors")
        val event = DragEvent(dragData, 0.0, 0.0)
        delegate.enter(event)
        val result = delegate.drop(event)
        delegate.end(event)
        if (result) "OK" else throw DriverActionException("drop was rejected by the target")
    }
}