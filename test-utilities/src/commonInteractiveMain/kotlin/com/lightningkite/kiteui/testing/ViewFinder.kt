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
     * Finds a view by its text content.
     *
     * @param root The root view to search from
     * @param text The text to search for (exact match)
     * @param ignoreCase Whether to ignore case when matching
     * @return The first view with matching text, or null if not found
     */
    fun findByText(root: RView, text: String, ignoreCase: Boolean = false): RView? {
        return findAll(root) { view ->
            val viewText = ViewProperties.getText(view)
            if (ignoreCase) {
                viewText?.equals(text, ignoreCase = true) == true
            } else {
                viewText == text
            }
        }.firstOrNull()
    }

    /**
     * Finds a view by partial text content (contains).
     *
     * @param root The root view to search from
     * @param text The text to search for (substring match)
     * @param ignoreCase Whether to ignore case when matching
     * @return The first view containing the text, or null if not found
     */
    fun findByTextContaining(root: RView, text: String, ignoreCase: Boolean = false): RView? {
        return findAll(root) { view ->
            val viewText = ViewProperties.getText(view)
            viewText?.contains(text, ignoreCase = ignoreCase) == true
        }.firstOrNull()
    }

    /**
     * Finds a view by content description (accessibility label).
     *
     * @param root The root view to search from
     * @param description The content description to search for
     * @return The first view with matching content description, or null if not found
     */
    fun findByContentDescription(root: RView, description: String): RView? {
        return findAll(root) { view ->
            ViewProperties.getContentDescription(view) == description
        }.firstOrNull()
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

    /**
     * Finds all views with the given debugName.
     *
     * @param root The root view to search from
     * @param debugName The debugName to search for
     * @return List of all views with matching debugName
     */
    fun findAllByDebugName(root: RView, debugName: String): List<RView> {
        return findAll(root) { it.debugName == debugName }
    }

    /**
     * Finds all views containing the given text.
     *
     * @param root The root view to search from
     * @param text The text to search for
     * @param ignoreCase Whether to ignore case when matching
     * @return List of all views containing the text
     */
    fun findAllByText(root: RView, text: String, ignoreCase: Boolean = false): List<RView> {
        return findAll(root) { view ->
            val viewText = ViewProperties.getText(view)
            if (ignoreCase) {
                viewText?.equals(text, ignoreCase = true) == true
            } else {
                viewText == text
            }
        }
    }
}

// Extension functions for easier usage

/**
 * Extension function to find a view by debugName from this root.
 *
 * Example:
 * ```
 * val submitButton = root.findByDebugName("submit")
 * ```
 */
fun RView.findByDebugName(debugName: String): RView? {
    return ViewFinder.findByDebugName(this, debugName)
}

/**
 * Extension function to find a view by text content from this root.
 *
 * Example:
 * ```
 * val loginButton = root.findByText("Log In")
 * ```
 */
fun RView.findByText(text: String, ignoreCase: Boolean = false): RView? {
    return ViewFinder.findByText(this, text, ignoreCase)
}

/**
 * Extension function to find a view by partial text content from this root.
 *
 * Example:
 * ```
 * val errorMessage = root.findByTextContaining("error")
 * ```
 */
fun RView.findByTextContaining(text: String, ignoreCase: Boolean = false): RView? {
    return ViewFinder.findByTextContaining(this, text, ignoreCase)
}

/**
 * Extension function to find a view by content description from this root.
 *
 * Example:
 * ```
 * val searchIcon = root.findByContentDescription("Search")
 * ```
 */
fun RView.findByContentDescription(description: String): RView? {
    return ViewFinder.findByContentDescription(this, description)
}

/**
 * Extension function to find all views matching a predicate from this root.
 *
 * Example:
 * ```
 * val allButtons = root.findAll { it.isClickable }
 * ```
 */
fun RView.findAll(predicate: (RView) -> Boolean): List<RView> {
    return ViewFinder.findAll(this, predicate)
}

/**
 * Extension function to find all views with the given debugName from this root.
 *
 * Example:
 * ```
 * val allListItems = root.findAllByDebugName("list-item")
 * ```
 */
fun RView.findAllByDebugName(debugName: String): List<RView> {
    return ViewFinder.findAllByDebugName(this, debugName)
}

/**
 * Extension function to find all views with the given text from this root.
 *
 * Example:
 * ```
 * val allDeleteButtons = root.findAllByText("Delete")
 * ```
 */
fun RView.findAllByText(text: String, ignoreCase: Boolean = false): List<RView> {
    return ViewFinder.findAllByText(this, text, ignoreCase)
}
