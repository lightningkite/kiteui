package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.OverrideOnly
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.views.l2.findFirstInteractiveDescendant
import com.lightningkite.reactive.context.StatusListener
import com.lightningkite.reactive.core.Reactive
import com.lightningkite.reactive.core.ReactiveState
import com.lightningkite.reactive.core.Release
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


private class FalseWhenSuccessful(val status: Reactive<*>): Reactive<Boolean> {
    override val state: ReactiveState<Boolean> get() =
        ReactiveState(!status.state.success)

    override fun addListener(listener: () -> Unit): Release = status.addListener(listener)
}
@Suppress("UNCHECKED_CAST")
val NativeElement.working: Reactive<Boolean> get() =
    context.addons.local.getOrPut("NativeElement.working") { FalseWhenSuccessful(foregroundProcesses) } as Reactive<Boolean>

@Suppress("UNCHECKED_CAST")
val NativeElement.loading: Reactive<Boolean> get() =
    context.addons.local.getOrPut("NativeElement.loading") { FalseWhenSuccessful(backgroundProcesses) } as Reactive<Boolean>

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

inline fun Element.withoutLoadingAnimations(block: KiteUiCoroutineScopeHelpers.() -> Unit) {
    object : KiteUiCoroutineScopeHelpers, CoroutineScope by CoroutineScope(coroutineContext.minusKey(StatusListener.Key)) {}.run(block)
}

/**
 * Requests focus on this element, or on its first interactive descendant if this element
 * isn't interactive. Used by navigation to move focus to new page content.
 */
fun Element.requestFocusOrDescendant() {
    if (this is InteractiveElement) {
        requestFocus()
    } else if (this is ElementWithChildren) {
        findFirstInteractiveDescendant()?.requestFocus() ?: requestFocus()
    } else {
        requestFocus()
    }
}

internal inline fun ContainerElement.beforeSetupContainer(crossinline action: Element.() -> Unit): ContainerElement =
    object : ContainerElement by this {
        @OverrideOnly
        override fun willAddChild(element: Element) {
            this@beforeSetupContainer.willAddChild(element)
            action(element)
        }
    }