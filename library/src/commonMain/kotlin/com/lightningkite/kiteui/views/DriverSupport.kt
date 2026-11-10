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

    fun walk(view: Element, depth: Int) {
        if (!options.includeHidden && !(view.shown && view.visible)) return
        val line = view.underlyingNativeElement.driverDisplay(options)

        if (options.interactiveOnly && view.driverActions.keys.all { it in baseActions } && view.driverValue == null && view.debugName == null) {
            // Skip structural-only nodes but still walk children
            for (child in view.driverChildren()) walk(child, depth)
            return
        }

        repeat(depth) { append("  ") }
        append(view.driverPath())
        append(": ")
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
public fun Element.driverFind(query: String, includeHidden: Boolean = false): String = buildString {
    val baseActions = setOf("snapshot", "screenshot", "find", "findClickable", "scrollIntoView")
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
    for (child in this@driverFind.driverChildren()) walk(child)
    if (results.isEmpty()) {
        append("No views matching '$query'")
    } else {
        for (view in results) {
            appendLine("${view.driverPath()}: ${view.driverDisplay(Element.DriverSnapshotOptions())}")
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
    for (child in this@driverFindClickable.driverChildren()) walk(child)

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
            appendLine("${view.driverPath()}: ${view.driverDisplay(Element.DriverSnapshotOptions())}")
        }
    }
}

/**
 * The shortest path [resolveDriverPath] finds this element by from the root: its name, if it's the first view with it
 * (names are deep-searched), or else its parent's path and its index.  Children of the root are just their index.
 */
public fun Element.driverPath(): String {
    val parent = parent ?: return debugName ?: "root"
    val root = generateSequence<Element>(this) { it.parent }.last()
    debugName?.let { if (root.findByName(it) === this) return it }
    val index = parent.driverChildren().indexOf(this)
    return if (parent.parent == null) "$index" else "${parent.driverPath()}/$index"
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
