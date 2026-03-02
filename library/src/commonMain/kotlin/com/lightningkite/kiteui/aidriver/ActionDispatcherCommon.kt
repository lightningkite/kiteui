// by Claude - shared action dispatch implementation, available to all platforms including SSR.
// Uses resolveAiPath + performAccessibilityAction to delegate actions to view subclasses.
package com.lightningkite.kiteui.aidriver

import com.lightningkite.kiteui.navigation.PageNavigator
import com.lightningkite.kiteui.views.RView
import com.lightningkite.kiteui.views.RViewHelper
import com.lightningkite.kiteui.views.resolveAiPath
import com.lightningkite.kiteui.views.ariaDescription
import com.lightningkite.kiteui.views.toAiId

/**
 * Returns the path segment to use for this view within a slash-separated AI path.
 *
 * Priority:
 * 1. [RView.debugName] if set
 * 2. [RViewHelper.ariaDescription] converted to camelCase via [toAiId]
 * 3. The numeric index among siblings (provided by the caller)
 */
// by Claude - moved from commonInteractiveMain to commonMain so SSR can use it
fun RView.toPathSegment(indexAmongSiblings: Int): String =
    debugName
        ?: (this as? RViewHelper)?.ariaDescription?.toAiId()
        ?: indexAmongSiblings.toString()

/**
 * Resolves [path] within [this] view's subtree and returns the target [RView],
 * or wraps the "not found" case into an [ActionDispatchResult] failure.
 */
// by Claude - moved from commonInteractiveMain to commonMain so SSR can use it
fun RView.resolveOrFail(path: String): Pair<RView?, ActionDispatchResult?> {
    val view = resolveAiPath(path)
    return if (view != null) {
        Pair(view, null)
    } else {
        Pair(null, ActionDispatchResult(false, "View not found: $path"))
    }
}

/**
 * Shared action dispatch implementation. Resolves views by path and delegates
 * to [RViewHelper.performAccessibilityAction]. All platforms delegate to this.
 */
// by Claude - moved from commonInteractiveMain to commonMain so SSR can use it
fun dispatchActionImpl(
    action: UiAction,
    root: RView,
    navigator: PageNavigator?
): ActionDispatchResult = try {
    when (action) {
        is UiAction.Click -> {
            val (view, err) = root.resolveOrFail(action.targetId)
            if (err != null) err
            else {
                val result = (view as RViewHelper).performAccessibilityAction("click")
                ActionDispatchResult(result == null, result)
            }
        }

        is UiAction.LongClick -> {
            val (view, err) = root.resolveOrFail(action.targetId)
            if (err != null) err
            else {
                val result = (view as RViewHelper).performAccessibilityAction("longClick")
                ActionDispatchResult(result == null, result)
            }
        }

        is UiAction.SetValue -> {
            val (view, err) = root.resolveOrFail(action.targetId)
            if (err != null) err
            else {
                val result = (view as RViewHelper).performAccessibilityAction("setValue", action.value)
                ActionDispatchResult(result == null, result)
            }
        }

        is UiAction.Scroll -> {
            val (view, err) = root.resolveOrFail(action.targetId)
            if (err != null) err
            else {
                val result = (view as RViewHelper).performAccessibilityAction("scroll", "${action.dx},${action.dy}")
                ActionDispatchResult(result == null, result)
            }
        }

        is UiAction.Navigate -> {
            if (navigator == null) ActionDispatchResult(false, "No navigator available")
            else {
                navigator.navigateUrlLikePath(action.route)
                ActionDispatchResult(true)
            }
        }

        is UiAction.Back -> {
            if (navigator == null) ActionDispatchResult(false, "No navigator available")
            else {
                val went = navigator.goBack()
                ActionDispatchResult(went, if (!went) "Nothing to go back to" else null)
            }
        }

        is UiAction.Forward -> {
            ActionDispatchResult(false, "Forward not supported")
        }

        is UiAction.Screenshot -> {
            ActionDispatchResult(false, "Use requestScreenshot() instead")
        }
    }
} catch (e: Exception) {
    ActionDispatchResult(false, e.message)
}
