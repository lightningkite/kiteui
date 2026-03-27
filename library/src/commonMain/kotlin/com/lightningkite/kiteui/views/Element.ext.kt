package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.OverrideOnly
import com.lightningkite.kiteui.models.*
import com.lightningkite.reactive.context.StatusListener
import kotlinx.coroutines.CoroutineScope


val Element.theme: Theme get() = themeAndBack.theme

var Element.padding: Dimension?
    get() = paddingByEdge?.left
    set(value) { paddingByEdge = value?.let(::Edges) }


/**
 * Returns whether animations are currently enabled for this element.
 * This is a platform-specific property that respects system-wide animation settings.
 */
expect val Element.areAnimationsEnabled: Boolean

/**
 * Executes the given action with animations temporarily disabled.
 *
 * This is useful when you need to make immediate visual changes without transitions,
 * such as during initial setup or when responding to rapid state changes.
 *
 * @param action The code to execute without animations.
 */
expect inline fun Element.withoutAnimation(action: () -> Unit)

inline fun Element.withoutLoadingAnimations(block: CoroutineScope.() -> Unit) {
    CoroutineScope(coroutineContext.minusKey(StatusListener.Key)).run(block)
}

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

internal fun ContainerElement.beforeSetupContainer(action: Element.() -> Unit): ContainerElement =
    object : ContainerElement by this {
        @OptIn(OverrideOnly::class)
        override fun willAddChild(element: Element) {
            this@beforeSetupContainer.willAddChild(element)
            action(element)
        }
    }