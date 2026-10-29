@file:MustUseReturnValues

package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.models.ScreenTransitions
import com.lightningkite.reactive.core.Release

// Stack of dismiss lambdas for currently-open dismissable modal dialogs (topmost is last). Shared
// app-wide because a lazyContextAddon's default is created once and stored on the ROOT context, so
// every context in the tree resolves to the same list. This lets back handlers (Android system back,
// browser back) dismiss the top dialog before navigating pages.
private val ElementContext.dismissableDialogStack by contextAddon(mutableListOf<() -> Unit>())

/** Registers [dismiss] as the topmost open dialog; returns a lambda that unregisters it on close. */
internal fun ElementContext.pushDismissableDialog(dismiss: () -> Unit): Release {
    val stack = dismissableDialogStack
    stack.add(dismiss)
    return { stack.remove(dismiss) }
}

/** If a dismissable dialog is open, dismisses the topmost one and returns true; otherwise false. */
internal fun ElementContext.dismissTopDialog(): Boolean {
    val top = dismissableDialogStack.removeLastOrNull() ?: return false
    top()
    return true
}

/**
 * Opens a ContainerElement context that can be used to render overlays. Note that on some platforms, this will spawn a new
 * view tree in the underlying view system. For example, on iOS modal overlays are rendered in a new ViewController,
 * which can be useful when overlaying over a bottom sheet. A side effect of this behavior is that non-modal overlays
 * will appear under bottom sheets on iOS.
 *
 * @param modal `true` if this overlay is intended as a modal, meaning that it covers and _may_ prevent interaction with
 * the UI under the modal. Note that setting this value to true does not enforce modality, but it may opt the layout in
 * to a more appropriate presentation strategy used by the native view system. (This behavior could be enforced using
 * `dismissBackground`, for example.) Setting this value to `false` guarantees that the presentation strategy
 * *will not* prevent interaction with views below the overlay.
 *
 * @param navClosable if `true` this overlay will be closable with the platform's 'back' action.
 */
public expect fun ElementContext.overlay(
    modal: Boolean = true,
    navClosable: Boolean = modal,
    transition: ScreenTransitions = ScreenTransitions.Fade,
    body: ContainerElement.(remove: () -> Unit) -> Unit
)
