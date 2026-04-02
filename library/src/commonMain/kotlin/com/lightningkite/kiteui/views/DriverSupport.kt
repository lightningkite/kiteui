package com.lightningkite.kiteui.views

fun Element.driverChildren(): List<Element> =
    (this as? ElementWithChildren)?.children ?: emptyList()

fun Element.driverChildrenOrNull(): List<Element>? =
    (this as? ElementWithChildren)?.children

/**
 * Renders a text snapshot of this view's subtree for AI driver / testing inspection.
 */
fun Element.driverSnapshot(options: Element.DriverSnapshotOptions = Element.DriverSnapshotOptions()): String = buildString {
    val baseActions = setOf("snapshot", "screenshot", "find", "findClickable", "scrollIntoView")

    fun walk(view: Element, depth: Int) {
        if (!options.includeHidden && !(view.shown && view.visible)) return
        val line = view.underlyingNativeElement.driverDisplay(options)

        if (options.interactiveOnly && view.driverActions.keys.all { it in baseActions } && view.driverValue == null && view.debugName == null) {
            // Skip structural-only nodes but still walk children
            for (child in view.driverChildren()) walk(child, depth)
            return
        }

        repeat(depth) { append("  ") }

        appendLine(line)

        for (child in view.driverChildren()) walk(child, depth + 1)
    }

    walk(this@driverSnapshot, 0)
}

/**
 * Searches this view's subtree for views matching [query] by debugName, driverValue,
 * widget type (class name), or action names.
 * Returns a compact listing of matches with their display line.
 */
fun Element.driverFind(query: String, includeHidden: Boolean = false): String = buildString {
    val baseActions = setOf("snapshot", "screenshot", "find", "findClickable", "scrollIntoView")
    val results = mutableListOf<Pair<String, Element>>()
    fun walk(view: Element, pathPrefix: String) {
        if (!includeHidden && !(view.shown && view.visible)) return
        val segment = view.debugName ?: view.parent?.children?.indexOf(view)?.takeIf { it >= 0 }?.toString() ?: "0"
        // Named views reset the path prefix — resolveDriverPath deep-searches for the first
        // named segment, so the full ancestor chain is unnecessary. This keeps paths short
        // (e.g. "email" instead of "0/1/email"). Requires debugNames to be unique within a subtree.
        val path = if (pathPrefix.isEmpty() || segment.toIntOrNull() == null) segment else "$pathPrefix/$segment"
        val nameMatch = view.debugName?.contains(query, ignoreCase = true) == true
        val valueMatch = view.driverValue?.contains(query, ignoreCase = true) == true
        val typeMatch = view::class.simpleName?.contains(query, ignoreCase = true) == true
        val actionMatch = view.driverActions.keys.any { it !in baseActions && it.contains(query, ignoreCase = true) }
        if (nameMatch || valueMatch || typeMatch || actionMatch) {
            results.add(path to view)
        }
        for (child in view.driverChildren()) walk(child, path)
    }
    // Start from root's children so returned paths align with resolveDriverPath's indexing.
    // resolveDriverPath("0") means root.driverChildren[0], so paths must be relative to root's children.
    for (child in this@driverFind.driverChildren()) walk(child, "")
    if (results.isEmpty()) {
        append("No views matching '$query'")
    } else {
        for ((path, view) in results) {
            appendLine("$path: ${view.driverDisplay(Element.DriverSnapshotOptions())}")
        }
    }
}

/**
 * Searches this view's subtree for views matching [query], then walks each match up to
 * the nearest ancestor with a "click" action. Returns deduplicated clickable ancestors
 * in the same format as [driverFind].
 */
fun Element.driverFindClickable(query: String, includeHidden: Boolean = false): String = buildString {
    val baseActions = setOf("snapshot", "screenshot", "find", "findClickable", "scrollIntoView")
    val viewPaths = mutableMapOf<Element, String>()
    val matches = mutableListOf<Element>()

    fun walk(view: Element, pathPrefix: String) {
        if (!includeHidden && !(view.shown && view.visible)) return
        val segment = view.debugName ?: view.parent?.children?.indexOf(view)?.takeIf { it >= 0 }?.toString() ?: "0"
        val path = if (pathPrefix.isEmpty() || segment.toIntOrNull() == null) segment else "$pathPrefix/$segment"
        viewPaths[view] = path
        val nameMatch = view.debugName?.contains(query, ignoreCase = true) == true
        val valueMatch = view.driverValue?.contains(query, ignoreCase = true) == true
        val typeMatch = view::class.simpleName?.contains(query, ignoreCase = true) == true
        val actionMatch = view.driverActions.keys.any { it !in baseActions && it.contains(query, ignoreCase = true) }
        if (nameMatch || valueMatch || typeMatch || actionMatch) {
            matches.add(view)
        }
        for (child in view.driverChildren()) walk(child, path)
    }
    // Start from root's children so returned paths align with resolveDriverPath's indexing.
    for (child in this@driverFindClickable.driverChildren()) walk(child, "")

    val seen = mutableSetOf<Element>()
    val results = mutableListOf<Pair<String, Element>>()
    for (match in matches) {
        var current: Element? = match
        while (current != null && current in viewPaths) {
            if (current.driverActions.containsKey("click")) {
                if (seen.add(current)) {
                    val path = viewPaths[current] ?: break
                    results.add(path to current)
                }
                break
            }
            current = current.parent
        }
    }

    if (results.isEmpty()) {
        append("No clickable views matching '$query'")
    } else {
        for ((path, view) in results) {
            appendLine("$path: ${view.driverDisplay(Element.DriverSnapshotOptions())}")
        }
    }
}

/**
 * Returns the full driver path for this element from the root.
 *
 * Builds a path using debugName for named elements and numeric indices for unnamed elements.
 * The path can be used with [resolveDriverPath] to locate this element.
 *
 * Examples:
 * - Named element at root: "email"
 * - Unnamed at index 0 of root: "0"
 * - Named element nested: "form/submitButton"
 * - Unnamed at index 1 under named "form": "form/1"
 * - Root element: "" (empty string)
 */
fun Element.driverPath(options: Element.DriverSnapshotOptions): String =
    generateSequence(this) { it.parent }
        .map { current ->
            current.debugName
                ?: current.parent?.children?.indexOf(current)?.takeIf { it >= 0 }?.toString()
                ?: ""
        }
        .toList()
        .reversed()
        .joinToString("/")

/**
 * Resolves a `/`-separated driver path to a view in this subtree.
 * Each segment matches by debugName first, then by numeric child index.
 * The first segment is deep-searched if no direct child matches.
 * Use `..` to navigate to the parent view.
 */
fun Element.resolveDriverPath(path: String): Element? {
    val segments = path.split("/").filter { it.isNotEmpty() }
    if (segments.isEmpty()) return this

    var current: Element = this

    // First segment: deep-search for named match
    val first = segments[0]
    if (first == "..") {
        current = current.parent ?: return null
    } else {
        val firstTarget = first.toIntOrNull()?.let { idx ->
            current.driverChildrenOrNull()?.getOrNull(idx)
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
            current.driverChildrenOrNull()?.getOrNull(idx)
        } ?: current.driverChildrenOrNull()?.firstOrNull { it.debugName == seg }
        current = next ?: return null
    }
    return current
}

private fun Element.findByName(name: String): Element? {
    if (debugName == name) return this
    for (child in driverChildren()) {
        val found = child.findByName(name)
        if (found != null) return found
    }
    return null
}

/**
 * Parses snapshot option flags from command args.
 */
fun parseSnapshotOptions(args: Array<out String>): Element.DriverSnapshotOptions {
    return Element.DriverSnapshotOptions(
        includeHidden = "--hidden" in args,
        interactiveOnly = "--interactive" in args,
        includeThemes = "--themes" in args,
    )
}
