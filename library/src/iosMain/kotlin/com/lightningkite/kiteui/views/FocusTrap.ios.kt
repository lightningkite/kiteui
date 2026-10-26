package com.lightningkite.kiteui.views

import platform.UIKit.UIAccessibilityPostNotification
import platform.UIKit.UIAccessibilityScreenChangedNotification
import platform.UIKit.accessibilityViewIsModal

/**
 * iOS implementation of [trapFocus].
 *
 * Marking the element as `accessibilityViewIsModal` makes VoiceOver ignore sibling views, and a
 * screen-changed notification moves VoiceOver focus into the modal. Focus is also moved to the
 * first interactive descendant so hardware keyboards land in the right place.
 */
public actual fun Element.trapFocus() {
    native.accessibilityViewIsModal = true
    requestFocusOrDescendant()
    UIAccessibilityPostNotification(UIAccessibilityScreenChangedNotification, native)
}
