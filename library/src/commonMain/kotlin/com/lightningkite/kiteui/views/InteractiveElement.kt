package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.reactive.Action
import com.lightningkite.reactive.core.Release

interface InteractiveElement : Element {
    var enabled: Boolean
}

interface ElementWithAction : InteractiveElement {
    var action: Action?
}

interface ElementWithSecondaryAction : ElementWithAction {
    var secondaryAction: Action?
}



// Native helpers

expect abstract class NativeInteractiveElement(context: ElementContext) : NativeElement, InteractiveElement {
    override var enabled: Boolean
}

expect abstract class NativeInteractiveContainerElement(context: ElementContext) : NativeContainerElement, InteractiveElement {
    override var enabled: Boolean
}

// ElementWithAction

abstract class NativeElementWithAction(context: ElementContext) : ElementWithAction, NativeInteractiveElement(context) {
    protected open fun nativeSetAction(action: Action?) {}

    private var stopWatchingAction: Release? = null

    final override var action: Action? = null
        set(value) {
            field = value
            stopWatchingAction?.invoke()
            stopWatchingAction = value?.let { watchForegroundProcess(it) }
            nativeSetAction(value)
        }
}

abstract class NativeElementWithSecondaryAction(context: ElementContext) : ElementWithSecondaryAction, NativeElementWithAction(context) {
    protected open fun nativeSetSecondaryAction(action: Action?) {}

    private var stopWatchingSecondaryAction: Release? = null

    final override var secondaryAction: Action? = null
        set(value) {
            field = value
            stopWatchingSecondaryAction?.invoke()
            stopWatchingSecondaryAction = value?.let { watchForegroundProcess(it) }
            nativeSetSecondaryAction(value)
        }
}

// ElementWithSecondaryAction

abstract class NativeContainerElementWithAction(context: ElementContext) : ElementWithAction, NativeInteractiveContainerElement(context) {
    protected open fun nativeSetAction(action: Action?) {}

    private var stopWatchingAction: Release? = null

    final override var action: Action? = null
        set(value) {
            field = value
            stopWatchingAction?.invoke()
            stopWatchingAction = value?.let { watchForegroundProcess(it) }
            nativeSetAction(value)
        }
}

abstract class NativeContainerElementWithSecondaryAction(context: ElementContext) : ElementWithSecondaryAction, NativeContainerElementWithAction(context) {
    protected open fun nativeSetSecondaryAction(action: Action?) {}

    private var stopWatchingSecondaryAction: Release? = null

    final override var secondaryAction: Action? = null
        set(value) {
            field = value
            stopWatchingSecondaryAction?.invoke()
            stopWatchingSecondaryAction = value?.let { watchForegroundProcess(it) }
            nativeSetSecondaryAction(value)
        }
}