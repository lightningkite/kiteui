package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.reactive.Action
import com.lightningkite.reactive.core.Release

/**
 * An element that is meant to be interacted with. Can be enabled or disabled.
 */
public interface InteractiveElement : Element {
    public var enabled: Boolean
}

/**
 * An interactive element with a primary action (e.g., button click, form submit).
 *
 * Elements inheriting this interface are responsible for displaying working animations when the
 * action is in progress (e.g. manage [StatusListener.watchForegroundProcess][com.lightningkite.reactive.context.StatusListener.watchForegroundProcess]
 * on the action when set)
 */
public interface ElementWithAction : InteractiveElement {
    public var action: Action?
}

/**
 * An interactive element with both primary and secondary actions (e.g., swipe actions, `onNavigate` actions, context menus).
 *
 * Elements inheriting this interface are responsible for displaying working animations when the
 * action is in progress (e.g. manage [StatusListener.watchForegroundProcess][com.lightningkite.reactive.context.StatusListener.watchForegroundProcess]
 * on the action when set)
 */
public interface ElementWithSecondaryAction : ElementWithAction {
    public var secondaryAction: Action?
}


/**
 * Rewatches an [Action] as a foreground process, releasing whatever was previously watched first.
 * Shared by the `action`/`secondaryAction` setters below (container and non-container variants
 * alike) so the stop-old/start-new logic exists in exactly one place.
 */
private fun NativeElement.rewatchAction(previousRelease: Release?, action: Action?): Release? {
    previousRelease?.invoke()
    return action?.let { watchForegroundProcess(it) }
}

// Native helpers

/**
 * Platform-specific base class for interactive elements without children.
 * Implements [enabled] by delegating to the native view's enabled state.
 */
public expect abstract class NativeInteractiveElement(context: ElementContext) : NativeElement, InteractiveElement {
    override var enabled: Boolean
}

/**
 * Platform-specific base class for interactive container elements with children.
 * Implements [enabled] by delegating to the native view's enabled state.
 */
public expect abstract class NativeInteractiveContainerElement(context: ElementContext) : NativeContainerElement, InteractiveElement {
    override var enabled: Boolean
}

// ElementWithAction

/**
 * Base class for non-container elements with a primary action.
 * Automatically watches action for foreground process state.
 * Override [nativeSetAction] to handle platform-specific action configuration.
 */
public abstract class NativeElementWithAction(context: ElementContext) : ElementWithAction, NativeInteractiveElement(context) {
    protected open fun nativeSetAction(action: Action?) {}

    private var stopWatchingAction: Release? = null

    final override var action: Action? = null
        set(value) {
            field = value
            stopWatchingAction = rewatchAction(stopWatchingAction, value)
            nativeSetAction(value)
        }
}

/**
 * Base class for non-container elements with primary and secondary actions.
 * Override [nativeSetSecondaryAction] to handle platform-specific secondary action configuration.
 */
public abstract class NativeElementWithSecondaryAction(context: ElementContext) : ElementWithSecondaryAction, NativeElementWithAction(context) {
    protected open fun nativeSetSecondaryAction(action: Action?) {}

    private var stopWatchingSecondaryAction: Release? = null

    final override var secondaryAction: Action? = null
        set(value) {
            field = value
            stopWatchingSecondaryAction = rewatchAction(stopWatchingSecondaryAction, value)
            nativeSetSecondaryAction(value)
        }
}

// Container variants

/**
 * Base class for container elements with a primary action.
 * Automatically watches action for foreground process state.
 * Override [nativeSetAction] to handle platform-specific action configuration.
 */
public abstract class NativeContainerElementWithAction(context: ElementContext) : ElementWithAction, NativeInteractiveContainerElement(context) {
    protected open fun nativeSetAction(action: Action?) {}

    private var stopWatchingAction: Release? = null

    final override var action: Action? = null
        set(value) {
            field = value
            stopWatchingAction = rewatchAction(stopWatchingAction, value)
            nativeSetAction(value)
        }
}

/**
 * Base class for container elements with primary and secondary actions.
 * Override [nativeSetSecondaryAction] to handle platform-specific secondary action configuration.
 */
public abstract class NativeContainerElementWithSecondaryAction(context: ElementContext) : ElementWithSecondaryAction, NativeContainerElementWithAction(context) {
    protected open fun nativeSetSecondaryAction(action: Action?) {}

    private var stopWatchingSecondaryAction: Release? = null

    final override var secondaryAction: Action? = null
        set(value) {
            field = value
            stopWatchingSecondaryAction = rewatchAction(stopWatchingSecondaryAction, value)
            nativeSetSecondaryAction(value)
        }
}