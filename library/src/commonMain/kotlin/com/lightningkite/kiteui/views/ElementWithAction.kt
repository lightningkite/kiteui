package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.reactive.Action
import com.lightningkite.reactive.core.Release

interface ElementWithAction : Element {
    var action: Action?
}

interface ElementWithSecondaryAction : ElementWithAction {
    var secondaryAction: Action?
}

abstract class NativeElementWithAction(context: ElementContext) : NativeElement(context), ElementWithAction {
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

abstract class NativeElementWithSecondaryAction(context: ElementContext) : NativeElementWithAction(context), ElementWithSecondaryAction {
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

abstract class NativeContainerElementWithAction(context: ElementContext) : NativeContainerElement(context), ElementWithAction {
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

abstract class NativeContainerElementWithSecondaryAction(context: ElementContext) : NativeContainerElementWithAction(context), ElementWithSecondaryAction {
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