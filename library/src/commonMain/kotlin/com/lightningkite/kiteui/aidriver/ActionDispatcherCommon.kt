// by Claude - shared action dispatch implementation, available to all platforms including SSR.
// Uses resolveAiPath + performAccessibilityAction to delegate actions to view subclasses.
package com.lightningkite.kiteui.aidriver

import com.lightningkite.kiteui.models.DragData
import com.lightningkite.kiteui.models.DragEvent
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

// by Claude - safe cast with descriptive error message
private fun RView.asHelper(): RViewHelper =
    this as? RViewHelper ?: error("View ${this::class.simpleName} at path is not an RViewHelper")

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
 * Walks up from [view] to find the nearest view (including itself) with a [DropTargetDelegate].
 */
// by Claude - finds drop target delegate on view or ancestors for drag-and-drop dispatch
private fun findDropDelegate(view: RView): com.lightningkite.kiteui.views.DropTargetDelegate? {
    var current: RView? = view
    while (current != null) {
        val delegate = (current as? RViewHelper)?.dropTargetDelegate
        if (delegate != null) return delegate
        current = current.parent
    }
    return null
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
                val result = view!!.asHelper().performAccessibilityAction("click")
                ActionDispatchResult(result == null, result)
            }
        }

        is UiAction.LongClick -> {
            val (view, err) = root.resolveOrFail(action.targetId)
            if (err != null) err
            else {
                val result = view!!.asHelper().performAccessibilityAction("longClick")
                ActionDispatchResult(result == null, result)
            }
        }

        is UiAction.SetValue -> {
            val (view, err) = root.resolveOrFail(action.targetId)
            if (err != null) err
            else {
                val result = view!!.asHelper().performAccessibilityAction("setValue", action.value)
                ActionDispatchResult(result == null, result)
            }
        }

        is UiAction.Scroll -> {
            val (view, err) = root.resolveOrFail(action.targetId)
            if (err != null) err
            else {
                val result = view!!.asHelper().performAccessibilityAction("scroll", "${action.dx},${action.dy}")
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

        // by Claude - drag-and-drop: get dragData from source, find drop target, invoke delegate directly
        is UiAction.DragAndDrop -> {
            val (srcView, srcErr) = root.resolveOrFail(action.targetId)
            if (srcErr != null) srcErr
            else {
                val dragData = srcView!!.asHelper().dragData
                    ?: return ActionDispatchResult(false, "Source view '${action.targetId}' has no dragData")

                val (destView, destErr) = root.resolveOrFail(action.toTargetId)
                if (destErr != null) destErr
                else {
                    // Walk up from destView to find the nearest ancestor with a dropTargetDelegate
                    val delegate = findDropDelegate(destView!!)
                        ?: return ActionDispatchResult(false, "Destination view '${action.toTargetId}' (and its ancestors) have no dropTargetDelegate")

                    val destRect = destView.screenRectangle()
                    val xInView = destRect?.let { (it.right - it.left) / 2 } ?: 0.0
                    val yInView = destRect?.let { (it.bottom - it.top) / 2 } ?: 0.0
                    val event = DragEvent(data = dragData, xInView = xInView, yInView = yInView)

                    delegate.enter(event)
                    val accepted = delegate.drop(event)
                    delegate.end(event)

                    if (accepted) ActionDispatchResult(true, "Drop accepted")
                    else ActionDispatchResult(false, "Drop rejected by delegate")
                }
            }
        }
    }
} catch (e: Exception) {
    ActionDispatchResult(false, e.message)
}
