// by Claude - aria description modifier and AI path utilities
package com.lightningkite.kiteui.views

/**
 * Accessible description for this view.
 * On Android, sets contentDescription; on iOS, sets accessibilityLabel;
 * on Web, sets the aria-label attribute.
 * Also used as a fallback ID by the AI driver when no explicit testId is set.
 */
var RView.ariaDescription: String?
    get() = (this as RViewHelper).ariaDescription
    set(value) {
        (this as RViewHelper).ariaDescription = value
    }

/**
 * Modifier to set an aria description on the next view element.
 *
 * Example:
 * ```kotlin
 * ariaDescription("Sign in button") - button {
 *     text("Sign In")
 * }
 * ```
 */
@ViewModifierDsl3
fun ViewWriter.ariaDescription(value: String): ViewWriter = also {
    it.beforeNextElementSetup {
        ariaDescription = value
    }
}

/**
 * Converts a human-readable string to a camelCase AI path segment.
 * Splits on whitespace, hyphens, and underscores.
 * E.g. "Sign In Button" → "signInButton"
 */
fun String.toAiId(): String =
    trim().split(Regex("[\\s\\-_]+"))
        .filter { it.isNotEmpty() }
        .mapIndexed { i, word ->
            if (i == 0) word.lowercase()
            else word.lowercase().replaceFirstChar { it.uppercaseChar() }
        }
        .joinToString("")

/**
 * Resolves a slash-separated path from this view's subtree.
 * - ".." moves to parent
 * - Numeric segment selects child by index
 * - Named segment matches debugName or ariaDescription.toAiId()
 *
 * If the first segment is a name and doesn't match a direct child, deep-searches for it
 * as an anchor, then follows remaining segments from there. This supports the compact
 * snapshot format where named elements reset their path prefix.
 */
// by Claude - anchored path resolution: deep-search for named first segment, then follow remaining path
fun RView.resolveAiPath(path: String): RView? {
    val segments = path.split("/").filter { it.isNotEmpty() }

    // Try direct path resolution using activeChildren to skip stale SwapView children
    var current: RView = this
    var resolved = true
    for (segment in segments) {
        val next = when {
            segment == ".." -> current.parent
            segment.all(Char::isDigit) -> current.activeChildren.getOrNull(segment.toInt())
            else -> current.activeChildren.find {
                it.debugName == segment || (it as? RViewHelper)?.ariaDescription?.toAiId() == segment
            }
        }
        if (next == null) { resolved = false; break }
        current = next
    }
    if (resolved) return current

    // Fallback: if the first segment is a name, deep-search for it as an anchor,
    // then follow remaining segments from there
    if (segments.isNotEmpty() && !segments[0].all(Char::isDigit) && segments[0] != "..") {
        val anchor = deepSearchByName(segments[0]) ?: return null
        if (segments.size == 1) return anchor
        // Follow remaining segments from the anchor
        return anchor.resolveAiPath(segments.drop(1).joinToString("/"))
    }

    return null
}

// by Claude - depth-first search for a view by debugName or ariaDescription
private fun RView.deepSearchByName(name: String): RView? {
    // by Claude - use activeChildren to skip stale SwapView children
    for (child in activeChildren) {
        if (child.debugName == name || (child as? RViewHelper)?.ariaDescription?.toAiId() == name) return child
        child.deepSearchByName(name)?.let { return it }
    }
    return null
}
