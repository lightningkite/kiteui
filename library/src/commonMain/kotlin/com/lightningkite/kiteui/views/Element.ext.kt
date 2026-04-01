package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.OverrideOnly
import com.lightningkite.kiteui.models.*
import com.lightningkite.reactive.context.StatusListener
import kotlinx.coroutines.CoroutineScope


val Element.theme: Theme get() = themeAndBack.theme

var Element.padding: Dimension?
    get() = paddingByEdge?.left
    set(value) { paddingByEdge = value?.let(::Edges) }

fun Element.closestElementWriter(): ElementWriter? = (this as? ElementWriter) ?: this.parent

fun Element.viewPath(): String = generateSequence(this) { it.parent }
    .map { element ->
        val id = element.debugName ?: element.parent?.children?.indexOf(element)?.toString() ?: ""
        val type = element::class.simpleName ?: "anonymous"
        "$id:$type"
    }
    .toList()
    .reversed()
    .joinToString("/")

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

internal inline fun ContainerElement.beforeSetupContainer(crossinline action: Element.() -> Unit): ContainerElement =
    object : ContainerElement by this {
        @OverrideOnly
        override fun willAddChild(element: Element) {
            this@beforeSetupContainer.willAddChild(element)
            action(element)
        }
    }