package com.lightningkite.kiteui.views

public fun Element.driverChildren(): List<Element> =
    (this as? ElementWithChildren)?.children ?: emptyList()

public fun Element.driverChildrenOrNull(): List<Element>? =
    (this as? ElementWithChildren)?.children

/**
 * Renders a text snapshot of this view's subtree for AI driver / testing inspection.
 */
public fun Element.driverSnapshot(options: Element.DriverSnapshotOptions = Element.DriverSnapshotOptions()): String = buildString {
    val baseActions = setOf("snapshot", "screenshot", "find", "findClickable", "scrollIntoView")
    val (start, paths) = driverTree()

    fun walk(view: Element, depth: Int) {
        if (!options.includeHidden && !(view.shown && view.visible)) return
        val line = view.underlyingNativeElement.driverDisplay(options)

        if (options.interactiveOnly && view.driverActions.keys.all { it in baseActions } && view.driverValue == null && view.debugName == null) {
            // Skip structural-only nodes but still walk children
            for (child in view.driverChildren()) walk(child, depth)
            return
        }

        repeat(depth) { append("  ") }
        append(paths.getValue(view))
        append(": ")
        appendLine(line)

        for (child in view.driverChildren()) walk(child, depth + 1)
    }

    walk(start, 0)
}

/**
 * Searches this view's subtree for views matching [query] by debugName, driverValue,
 * widget type (class name), or action names.
 * Returns a compact listing of matches with their display line.
 */
public fun Element.driverFind(query: String, includeHidden: Boolean = false): String = buildString {
    val baseActions = setOf("snapshot", "screenshot", "find", "findClickable", "scrollIntoView")
    val (start, paths) = driverTree()
    val results = mutableListOf<Element>()
    fun walk(view: Element) {
        if (!includeHidden && !(view.shown && view.visible)) return
        val nameMatch = view.debugName?.contains(query, ignoreCase = true) == true
        val valueMatch = view.driverValue?.contains(query, ignoreCase = true) == true
        val typeMatch = view::class.simpleName?.contains(query, ignoreCase = true) == true
        val actionMatch = view.driverActions.keys.any { it !in baseActions && it.contains(query, ignoreCase = true) }
        if (nameMatch || valueMatch || typeMatch || actionMatch) {
            results.add(view)
        }
        for (child in view.driverChildren()) walk(child)
    }
    for (child in start.driverChildren()) walk(child)
    if (results.isEmpty()) {
        append("No views matching '$query'")
    } else {
        for (view in results) {
            appendLine("${paths.getValue(view)}: ${view.driverDisplay(Element.DriverSnapshotOptions())}")
        }
    }
}

/**
 * Searches this view's subtree for views matching [query], then walks each match up to
 * the nearest ancestor with a "click" action. Returns deduplicated clickable ancestors
 * in the same format as [driverFind].
 */
public fun Element.driverFindClickable(query: String, includeHidden: Boolean = false): String = buildString {
    val baseActions = setOf("snapshot", "screenshot", "find", "findClickable", "scrollIntoView")
    val (start, paths) = driverTree()
    val searched = mutableSetOf<Element>()
    val matches = mutableListOf<Element>()

    fun walk(view: Element) {
        if (!includeHidden && !(view.shown && view.visible)) return
        searched.add(view)
        val nameMatch = view.debugName?.contains(query, ignoreCase = true) == true
        val valueMatch = view.driverValue?.contains(query, ignoreCase = true) == true
        val typeMatch = view::class.simpleName?.contains(query, ignoreCase = true) == true
        val actionMatch = view.driverActions.keys.any { it !in baseActions && it.contains(query, ignoreCase = true) }
        if (nameMatch || valueMatch || typeMatch || actionMatch) {
            matches.add(view)
        }
        for (child in view.driverChildren()) walk(child)
    }
    for (child in start.driverChildren()) walk(child)

    val results = mutableSetOf<Element>()
    for (match in matches) {
        var current: Element? = match
        while (current != null && current in searched) {
            if (current.driverActions.containsKey("click")) {
                results.add(current)
                break
            }
            current = current.parent
        }
    }

    if (results.isEmpty()) {
        append("No clickable views matching '$query'")
    } else {
        for (view in results) {
            appendLine("${paths.getValue(view)}: ${view.driverDisplay(Element.DriverSnapshotOptions())}")
        }
    }
}

/**
 * This view as it is in the tree, with the paths [resolveDriverPath] resolves from the root to each view in the tree.
 *
 * A path is the view's name, if it's the first view with that name (names are deep-searched), or else its parent's path
 * and its index.  They're built down from the root, because a view that another wraps (a recycler's frame, say) has a
 * [Element.parent] that doesn't list it.  A view that delegates to another (a recycler to its frame) runs that one's
 * actions, so this may be the delegate, when the view in the tree is the one delegating to it.
 */
private fun Element.driverTree(): Pair<Element, Map<Element, String>> {
    val root = generateSequence(this) { it.parent }.last()
    val paths = HashMap<Element, String>()
    val names = HashSet<String>()
    fun walk(view: Element, path: String) {
        paths[view] = path
        for ((index, child) in view.driverChildren().withIndex()) {
            val name = child.debugName
            walk(child, if (name != null && names.add(name)) name else if (view === root) "$index" else "$path/$index")
        }
    }
    root.debugName?.let { names.add(it) }
    walk(root, root.debugName ?: "root")
    val inTree = if (this in paths) this else paths.keys.single { it.underlyingNativeElement === underlyingNativeElement }
    return inTree to paths
}

/**
 * Resolves a `/`-separated driver path to a view in this subtree.
 * Each segment matches by debugName first, then by numeric child index.
 * The first segment is deep-searched if no direct child matches.
 * Use `..` to navigate to the parent view.
 */
public fun Element.resolveDriverPath(path: String): Element? {
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
public fun parseSnapshotOptions(args: Array<out String>): Element.DriverSnapshotOptions {
    return Element.DriverSnapshotOptions(
        includeHidden = "--hidden" in args,
        interactiveOnly = "--interactive" in args,
        includeThemes = "--themes" in args,
    )
}
