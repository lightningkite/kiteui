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
 * For single named segments, falls back to a deep tree search if no direct child matches.
 * This allows `resolveAiPath("loginButton")` to find a deeply nested view by its debugName.
 */
// by Claude - added deep search fallback for single named segments
fun RView.resolveAiPath(path: String): RView? {
    val segments = path.split("/").filter { it.isNotEmpty() }

    // Try direct path resolution
    var current: RView = this
    var resolved = true
    for (segment in segments) {
        val next = when {
            segment == ".." -> current.parent
            segment.all(Char::isDigit) -> current.children.getOrNull(segment.toInt())
            else -> current.children.find {
                it.debugName == segment || (it as? RViewHelper)?.ariaDescription?.toAiId() == segment
            }
        }
        if (next == null) { resolved = false; break }
        current = next
    }
    if (resolved) return current

    // Fallback: deep search for a single named segment
    if (segments.size == 1 && !segments[0].all(Char::isDigit) && segments[0] != "..") {
        return deepSearchByName(segments[0])
    }

    return null
}

// by Claude - depth-first search for a view by debugName or ariaDescription
private fun RView.deepSearchByName(name: String): RView? {
    for (child in children) {
        if (child.debugName == name || (child as? RViewHelper)?.ariaDescription?.toAiId() == name) return child
        child.deepSearchByName(name)?.let { return it }
    }
    return null
}
