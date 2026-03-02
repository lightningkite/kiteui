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
)

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
 * [id] is the full absolute path from page root (slash-separated, e.g. "loginForm/emailInput").
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
    fun render(builder: StringBuilder, tab: Int) {
        builder.append(" ".repeat(tab * 2))
        builder.append("$id: $type")
        if(value != null) builder.append(" = \"${value.replace("\n", "\\n").replace("\"", "\\\"")}\"")
        val list = ArrayList<String>()
        if(!enabled) list.add("disabled")
        if(!shown) list.add("hidden")
        if(!visible) list.add("invisible")
        if(list.isNotEmpty()) builder.append(" (${list.joinToString(", ")})")
        theme?.let { builder.append(" (theme: $it)") }
        screenCoordinates?.let { builder.append(" ($it)") }
        builder.append("\n")
        children.forEach { it.render(builder, tab + 1) }
    }
}
