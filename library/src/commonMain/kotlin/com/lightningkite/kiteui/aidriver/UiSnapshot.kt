// by Claude - UI snapshot types for AI driver
package com.lightningkite.kiteui.aidriver

import kotlinx.serialization.Serializable

/**
 * A complete snapshot of the app's current UI state, rooted at the current page.
 */
@Serializable
data class UiSnapshot(
    val page: String,
    val url: String,
    val settings: UiSnapshotSettings,
    val components: List<UiComponent>
) {
    // by Claude - recursive search by full path or last segment
    fun findById(id: String): UiComponent? {
        for (c in components) {
            c.findById(id)?.let { return it }
        }
        return null
    }

    /**
     * Prunes structural-only containers from the snapshot tree.
     * A component is "interesting" if it has a name (non-numeric id), value, or actions.
     * - Leaf with no interesting properties → discarded
     * - Single-child structural container → collapsed, merging its index into the child's path
     * - Multi-child structural container → kept as unnamed grouping node
     */
    // by Claude - post-processing pass for interactiveOnly mode
    fun compact(): UiSnapshot = copy(components = components.mapNotNull { it.compact() })

    /**
     * Search for components matching the given criteria.
     * All filters are optional and combined with AND logic.
     * Returns up to [limit] matching components.
     */
    // by Claude - extracted from CliServer for shared use between server and tests
    fun find(
        value: String? = null,
        type: String? = null,
        action: String? = null,
        id: String? = null,
        limit: Int = 10
    ): List<FindResult> {
        val results = mutableListOf<FindResult>()
        fun search(comp: UiComponent) {
            if (results.size >= limit) return
            val matches =
                (value == null || comp.value?.contains(value, ignoreCase = true) == true) &&
                (type == null || comp.type.equals(type, ignoreCase = true)) &&
                (action == null || comp.actions.any { it.equals(action, ignoreCase = true) }) &&
                (id == null || comp.id.contains(id, ignoreCase = true))
            if (matches) {
                results.add(FindResult(
                    id = comp.id, type = comp.type, value = comp.value,
                    actions = comp.actions, enabled = comp.enabled,
                ))
            }
            for (child in comp.children) search(child)
        }
        components.forEach { search(it) }
        return results
    }
}

@Serializable
data class UiSnapshotSettings(
    val includeTheme: Boolean = false,
    val includeLayoutInfo: Boolean = false,
    val includeLessVisible: Boolean = false,
)

@Serializable
data class ScreenRect(
    val top: Double,
    val left: Double,
    val width: Double,
    val height: Double
) {
    override fun toString(): String = "x: $left-${left+width}, y: ${top}-${top+height}"
}
/**
 * A single UI component in the snapshot tree.
 * [id] is a relative path from the nearest named ancestor (e.g. "submitBtn" or "0/2").
 * Named elements (with debugName or ariaDescription) use just their name as the id.
 * [actions] lists available interaction types (e.g. "click", "setValue").
 */
@Serializable
data class UiComponent(
    val id: String,
    val type: String,
    val theme: String? = null,
    val value: String? = null,
    val enabled: Boolean = true,
    val shown: Boolean = true,
    val visible: Boolean = true,
    val screenCoordinates: ScreenRect? = null,
    val actions: Set<String> = setOf(),
    val children: List<UiComponent> = listOf()
) {
    // by Claude - hierarchical search: supports relative IDs from compact snapshot format.
    // Matches by exact id, suffix match, or anchored prefix (e.g. "navigatorView/0/0/2"
    // finds component "navigatorView" then searches its children for "0/0/2").
    fun findById(id: String): UiComponent? {
        if (this.id == id || this.id.endsWith("/$id")) return this
        // Anchored prefix: if search ID starts with this component's id + "/",
        // strip the prefix and search children for the remainder
        if (id.startsWith("${this.id}/")) {
            val remainder = id.removePrefix("${this.id}/")
            for (c in children) {
                c.findById(remainder)?.let { return it }
            }
        }
        for (c in children) {
            c.findById(id)?.let { return it }
        }
        return null
    }

    /** Whether this component is "interesting" — has a name, value, or actions. */
    // by Claude - check last segment only, since IDs like "0/0" contain "/" which isn't a digit
    private val isInteresting: Boolean
        get() {
            val segment = id.substringAfterLast("/")
            return !segment.all(Char::isDigit) || value != null || actions.isNotEmpty()
        }

    /**
     * Prune structural-only containers:
     * - Leaf with no interesting properties → null (discarded)
     * - Single-child structural container → collapse, prepending this index to child's path
     * - Multi-child structural container → keep as grouping node
     */
    // by Claude - post-processing for interactiveOnly mode
    fun compact(): UiComponent? {
        val compactedChildren = children.mapNotNull { it.compact() }
        if (isInteresting) return copy(children = compactedChildren)
        // Not interesting
        if (compactedChildren.isEmpty()) return null  // discard empty structural leaf
        if (compactedChildren.size == 1) {
            // Collapse: merge this index into the child's path for addressability
            val child = compactedChildren[0]
            val mergedId = "$id/${child.id}"
            return child.copy(id = mergedId, children = child.children)
        }
        // Multi-child: keep as grouping node
        return copy(children = compactedChildren)
    }

    // by Claude - compact text format: "segment: Type = value [actions] (flags)"
    fun render(builder: StringBuilder, tab: Int) {
        builder.append(" ".repeat(tab * 2))
        // Show just the segment (last part of the id), not the full path
        val segment = id.substringAfterLast("/")
        builder.append("$segment: $type")
        if (value != null) builder.append(" = \"${value.replace("\n", "\\n").replace("\"", "\\\"")}\"")
        if (actions.isNotEmpty()) builder.append(" [${actions.joinToString(",")}]")
        val flags = ArrayList<String>()
        if (!enabled) flags.add("disabled")
        if (!shown) flags.add("hidden")
        if (!visible) flags.add("invisible")
        if (flags.isNotEmpty()) builder.append(" (${flags.joinToString(", ")})")
        theme?.let { builder.append(" (theme: $it)") }
        screenCoordinates?.let { builder.append(" ($it)") }
        builder.append("\n")
        children.forEach { it.render(builder, tab + 1) }
    }
}
