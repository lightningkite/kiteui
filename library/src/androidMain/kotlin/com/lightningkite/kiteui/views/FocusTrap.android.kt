package com.lightningkite.kiteui.views

import android.view.View
import android.view.ViewGroup
import com.lightningkite.reactive.context.onRemove

/**
 * Android implementation of [trapFocus].
 *
 * Siblings of this element are made non-focusable and hidden from accessibility services so that
 * keyboard/D-pad focus and TalkBack cannot reach the content behind the modal. Focus is moved to
 * the first interactive descendant. All modified values are restored when this element is removed.
 */
public actual fun Element.trapFocus() {
    val siblings = parent?.children?.filter { it !== this }.orEmpty()
    val saved = siblings.map { sibling ->
        val view = sibling.underlyingNativeElement.native
        val state = SavedFocusState(
            view = view,
            importantForAccessibility = view.importantForAccessibility,
            focusable = view.isFocusable,
            descendantFocusability = (view as? ViewGroup)?.descendantFocusability,
        )
        view.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
        view.isFocusable = false
        (view as? ViewGroup)?.descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
        state
    }

    onRemove {
        saved.forEach { state ->
            state.view.importantForAccessibility = state.importantForAccessibility
            state.view.isFocusable = state.focusable
            (state.view as? ViewGroup)?.descendantFocusability =
                state.descendantFocusability ?: ViewGroup.FOCUS_BEFORE_DESCENDANTS
        }
    }

    requestFocusOrDescendant()
}

private class SavedFocusState(
    val view: View,
    val importantForAccessibility: Int,
    val focusable: Boolean,
    val descendantFocusability: Int?,
)
