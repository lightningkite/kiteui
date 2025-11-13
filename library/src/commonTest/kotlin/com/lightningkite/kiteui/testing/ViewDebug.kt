package com.lightningkite.kiteui.testing

import com.lightningkite.kiteui.views.RView

/**
 * Debugging utilities for inspecting view hierarchies in tests.
 *
 * These helpers make it easier to understand the structure of your UI
 * when tests fail or when developing new tests.
 */
object ViewDebug {
    /**
     * Prints the view hierarchy starting from this view.
     *
     * Outputs a tree structure showing:
     * - View type (class name)
     * - debugName if set
     * - Key properties (text, visibility, enabled)
     * - Children recursively
     *
     * Example output:
     * ```
     * StackView
     *   ├─ TextView [debugName="title"] text="Hello" visible=true
     *   ├─ RowView
     *   │  ├─ Button [debugName="ok"] text="OK" enabled=true
     *   │  └─ Button [debugName="cancel"] text="Cancel" enabled=true
     *   └─ TextView text="Footer"
     * ```
     *
     * @param view The root view to print
     * @param maxDepth Maximum depth to traverse (default: 10)
     */
    fun printHierarchy(view: RView, maxDepth: Int = 10) {
        println(buildHierarchyString(view, maxDepth))
    }

    /**
     * Builds a string representation of the view hierarchy.
     *
     * @param view The root view
     * @param maxDepth Maximum depth to traverse
     * @return String representation of the hierarchy
     */
    fun buildHierarchyString(view: RView, maxDepth: Int = 10): String {
        val sb = StringBuilder()
        buildHierarchyRecursive(view, "", true, 0, maxDepth, sb)
        return sb.toString()
    }

    private fun buildHierarchyRecursive(
        view: RView,
        prefix: String,
        isLast: Boolean,
        depth: Int,
        maxDepth: Int,
        sb: StringBuilder
    ) {
        if (depth > maxDepth) {
            sb.append(prefix)
            sb.append(if (isLast) "└─ " else "├─ ")
            sb.append("... (max depth reached)\n")
            return
        }

        // Print current view
        sb.append(prefix)
        sb.append(if (depth == 0) "" else if (isLast) "└─ " else "├─ ")

        // View class name
        val className = view::class.simpleName ?: "Unknown"
        sb.append(className)

        // Debug name
        if (!view.debugName.isNullOrEmpty()) {
            sb.append(" [debugName=\"${view.debugName}\"]")
        }

        // Key properties
        val properties = mutableListOf<String>()

        try {
            val text = ViewProperties.getText(view)
            if (!text.isNullOrEmpty()) {
                val truncated = if (text.length > 30) text.take(27) + "..." else text
                properties.add("text=\"$truncated\"")
            }
        } catch (e: Exception) {
            // Ignore - property not available
        }

        try {
            if (!ViewProperties.isVisible(view)) {
                properties.add("visible=false")
            }
        } catch (e: Exception) {
            // Ignore
        }

        try {
            if (!ViewProperties.isEnabled(view)) {
                properties.add("enabled=false")
            }
        } catch (e: Exception) {
            // Ignore
        }

        if (properties.isNotEmpty()) {
            sb.append(" ")
            sb.append(properties.joinToString(" "))
        }

        sb.append("\n")

        // Print children
        val children = view.children
        if (children.isNotEmpty()) {
            val newPrefix = prefix + if (depth == 0) "" else if (isLast) "   " else "│  "
            children.forEachIndexed { index, child ->
                buildHierarchyRecursive(
                    child,
                    newPrefix,
                    index == children.size - 1,
                    depth + 1,
                    maxDepth,
                    sb
                )
            }
        }
    }

    /**
     * Counts the total number of views in the hierarchy.
     *
     * @param view The root view
     * @return Total number of views including the root
     */
    fun countViews(view: RView): Int {
        return 1 + view.children.sumOf { countViews(it) }
    }

    /**
     * Finds all views of a specific type in the hierarchy.
     *
     * @param view The root view to search
     * @param type The class to search for
     * @return List of all views matching the type
     */
    inline fun <reified T : RView> findByType(view: RView): List<T> {
        val results = mutableListOf<T>()
        findByTypeRecursive(view, results)
        return results
    }

    inline fun <reified T : RView> findByTypeRecursive(view: RView, results: MutableList<T>) {
        if (view is T) {
            results.add(view)
        }
        view.children.forEach { child ->
            findByTypeRecursive(child, results)
        }
    }

    /**
     * Prints statistics about the view hierarchy.
     *
     * Shows:
     * - Total view count
     * - Max depth
     * - View type distribution
     *
     * @param view The root view
     */
    fun printStats(view: RView) {
        val stats = collectStats(view)
        println("View Hierarchy Statistics:")
        println("  Total views: ${stats.totalCount}")
        println("  Max depth: ${stats.maxDepth}")
        println("  View types:")
        stats.typeCounts.entries.sortedByDescending { it.value }.forEach { (type, count) ->
            println("    $type: $count")
        }
    }

    private data class ViewStats(
        val totalCount: Int,
        val maxDepth: Int,
        val typeCounts: Map<String, Int>
    )

    private fun collectStats(view: RView): ViewStats {
        val typeCounts = mutableMapOf<String, Int>()
        var totalCount = 0
        var maxDepth = 0

        fun traverse(v: RView, depth: Int) {
            totalCount++
            maxDepth = maxOf(maxDepth, depth)

            val typeName = v::class.simpleName ?: "Unknown"
            typeCounts[typeName] = (typeCounts[typeName] ?: 0) + 1

            v.children.forEach { child ->
                traverse(child, depth + 1)
            }
        }

        traverse(view, 0)
        return ViewStats(totalCount, maxDepth, typeCounts)
    }
}

/**
 * Extension function to print the hierarchy of this view.
 *
 * Example:
 * ```
 * val root = harness.render { /* UI */ }
 * root.printHierarchy()  // Prints to console
 * ```
 */
fun RView.printHierarchy(maxDepth: Int = 10) {
    ViewDebug.printHierarchy(this, maxDepth)
}

/**
 * Extension function to get hierarchy as a string.
 *
 * Example:
 * ```
 * val hierarchyString = root.hierarchyString()
 * println(hierarchyString)
 * ```
 */
fun RView.hierarchyString(maxDepth: Int = 10): String {
    return ViewDebug.buildHierarchyString(this, maxDepth)
}

/**
 * Extension function to count views in this hierarchy.
 *
 * Example:
 * ```
 * val count = root.viewCount()
 * assertEquals(15, count)
 * ```
 */
fun RView.viewCount(): Int {
    return ViewDebug.countViews(this)
}

/**
 * Extension function to print statistics about this hierarchy.
 *
 * Example:
 * ```
 * root.printStats()
 * ```
 */
fun RView.printStats() {
    ViewDebug.printStats(this)
}
