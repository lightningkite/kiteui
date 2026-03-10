package com.lightningkite.kiteui.views

/**
 * Renders a text snapshot of this view's subtree for AI driver / testing inspection.
 */
fun RView.driverSnapshot(options: RViewHelper.DriverSnapshotOptions = RViewHelper.DriverSnapshotOptions()): String = buildString {
    fun walk(view: RView, depth: Int) {
        if (!options.includeHidden && !(view.shown && view.visible)) return
        val line = view.driverDisplay(options)
        val baseActions = setOf("snapshot", "screenshot", "find", "scroll", "scrollIntoView")
        if (options.interactiveOnly && view.driverActions.keys.all { it in baseActions } && view.driverValue == null && view.debugName == null) {
            // Skip structural-only nodes but still walk children
            for (child in view.driverChildren) walk(child, depth)
            return
        }
        repeat(depth) { append("  ") }
        appendLine(line)
        for (child in view.driverChildren) walk(child, depth + 1)
    }
    walk(this@driverSnapshot, 0)
}

/**
 * Searches this view's subtree for views matching [query] by debugName, driverValue,
 * widget type (class name), or action names.
 * Returns a compact listing of matches with their display line.
 */
fun RView.driverFind(query: String): String = buildString {
    val baseActions = setOf("snapshot", "screenshot", "find", "scroll", "scrollIntoView")
    val results = mutableListOf<Pair<String, RView>>()
    fun walk(view: RView, pathPrefix: String) {
        val segment = view.debugName ?: view.parent?.driverChildren?.indexOf(view)?.toString() ?: "0"
        val path = if (pathPrefix.isEmpty() || segment.toIntOrNull() == null) segment else "$pathPrefix/$segment"
        val nameMatch = view.debugName?.contains(query, ignoreCase = true) == true
        val valueMatch = view.driverValue?.contains(query, ignoreCase = true) == true
        val typeMatch = view::class.simpleName?.contains(query, ignoreCase = true) == true
        val actionMatch = view.driverActions.keys.any { it !in baseActions && it.contains(query, ignoreCase = true) }
        if (nameMatch || valueMatch || typeMatch || actionMatch) {
            results.add(path to view)
        }
        for (child in view.driverChildren) walk(child, path)
    }
    walk(this@driverFind, "")
    if (results.isEmpty()) {
        append("No views matching '$query'")
    } else {
        for ((path, view) in results) {
            appendLine("$path: ${view.driverDisplay(RViewHelper.DriverSnapshotOptions())}")
        }
    }
}

/**
 * Resolves a `/`-separated driver path to a view in this subtree.
 * Each segment matches by debugName first, then by numeric child index.
 * The first segment is deep-searched if no direct child matches.
 * Use `..` to navigate to the parent view.
 */
fun RView.resolveDriverPath(path: String): RView? {
    val segments = path.split("/").filter { it.isNotEmpty() }
    if (segments.isEmpty()) return this

    var current: RView = this

    // First segment: deep-search for named match
    val first = segments[0]
    if (first == "..") {
        current = current.parent ?: return null
    } else {
        val firstTarget = first.toIntOrNull()?.let { idx ->
            current.driverChildren.getOrNull(idx)
        } ?: current.findByName(first)
        current = firstTarget ?: return null
    }

    // Remaining segments: direct child lookup or parent navigation
    for (i in 1 until segments.size) {
        val seg = segments[i]
        if (seg == "..") {
            current = current.parent ?: return null
            continue
        }
        val next = seg.toIntOrNull()?.let { idx ->
            current.driverChildren.getOrNull(idx)
        } ?: current.driverChildren.firstOrNull { it.debugName == seg }
        current = next ?: return null
    }
    return current
}

private fun RView.findByName(name: String): RView? {
    if (debugName == name) return this
    for (child in driverChildren) {
        val found = child.findByName(name)
        if (found != null) return found
    }
    return null
}

/**
 * Parses snapshot option flags from command args.
 */
fun parseSnapshotOptions(args: Array<out String>): RViewHelper.DriverSnapshotOptions {
    return RViewHelper.DriverSnapshotOptions(
        includeHidden = "--hidden" in args,
        interactiveOnly = "--interactive" in args,
        includeThemes = "--themes" in args,
    )
}
