package com.lightningkite.kiteui.testing

import com.lightningkite.kiteui.views.RView

/**
 * Finds views in the view hierarchy.
 *
 * This is a cross-platform way to locate views for testing purposes.
 */
object ViewFinder {
    /**
     * Finds a view by its debugName property.
     *
     * @param root The root view to search from
     * @param debugName The debugName to search for
     * @return The first view with matching debugName, or null if not found
     */
    fun findByDebugName(root: RView, debugName: String): RView? {
        return findByDebugNameRecursive(root, debugName)
    }

    private fun findByDebugNameRecursive(view: RView, debugName: String): RView? {
        // Check if this view matches
        if (view.debugName == debugName) {
            return view
        }

        // Search children
        for (child in view.children) {
            val found = findByDebugNameRecursive(child, debugName)
            if (found != null) {
                return found
            }
        }

        return null
    }

    /**
     * Finds all views matching a predicate.
     *
     * @param root The root view to search from
     * @param predicate The condition to match
     * @return List of all matching views
     */
    fun findAll(root: RView, predicate: (RView) -> Boolean): List<RView> {
        val results = mutableListOf<RView>()
        findAllRecursive(root, predicate, results)
        return results
    }

    private fun findAllRecursive(view: RView, predicate: (RView) -> Boolean, results: MutableList<RView>) {
        if (predicate(view)) {
            results.add(view)
        }

        for (child in view.children) {
            findAllRecursive(child, predicate, results)
        }
    }
}

/**
 * Extension function to find a view by debugName from this root.
 */
fun RView.findByDebugName(debugName: String): RView? {
    return ViewFinder.findByDebugName(this, debugName)
}

/**
 * Extension function to find all views matching a predicate from this root.
 */
fun RView.findAll(predicate: (RView) -> Boolean): List<RView> {
    return ViewFinder.findAll(this, predicate)
}
