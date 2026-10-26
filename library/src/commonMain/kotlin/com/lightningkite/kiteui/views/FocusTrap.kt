package com.lightningkite.kiteui.views

/**
 * Makes this element behave as a modal focus scope.
 *
 * Focus is moved into this element (or its first interactive descendant), and the rest of the
 * view tree is made unreachable to keyboard focus and assistive technology until this element is
 * removed. When this element is removed, the previous state is restored - including returning
 * focus to the element that was focused before (on web).
 *
 * This is applied automatically to modal [overlays][ElementContext.overlay], such as
 * [dialogs][com.lightningkite.kiteui.views.l2.dialog] and
 * [popovers][com.lightningkite.kiteui.views.l2.rawPopover].
 *
 * The exact behavior is platform specific:
 * - **Web**: siblings are marked `inert`, the first focusable descendant receives focus, and focus
 *   is restored on close.
 * - **Android**: siblings have their descendant focus and accessibility hidden.
 * - **iOS**: the element is marked `accessibilityViewIsModal` and a screen-changed notification is
 *   posted so VoiceOver moves into it.
 */
public expect fun Element.trapFocus()
