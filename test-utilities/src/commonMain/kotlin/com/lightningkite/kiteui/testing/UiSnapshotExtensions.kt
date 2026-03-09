// by Claude - extension functions for UiSnapshot/UiComponent for test assertions
package com.lightningkite.kiteui.testing

import com.lightningkite.kiteui.aidriver.UiComponent
import com.lightningkite.kiteui.aidriver.UiSnapshot

/** Render the snapshot as a human-readable text tree (same format as CLI `./ui snapshot`). */
fun UiSnapshot.renderText(): String = buildString {
    appendLine("Page: $page  URL: $url")
    components.forEach { it.render(this, 0) }
}

/** Find all components matching a predicate. */
fun UiSnapshot.findAll(predicate: (UiComponent) -> Boolean): List<UiComponent> {
    val result = mutableListOf<UiComponent>()
    fun walk(components: List<UiComponent>) {
        for (c in components) {
            if (predicate(c)) result.add(c)
            walk(c.children)
        }
    }
    walk(components)
    return result
}

/** Find components by type. */
fun UiSnapshot.findByType(type: String): List<UiComponent> =
    findAll { it.type == type }

/** Find the first component whose value contains the given text. */
fun UiSnapshot.findByValue(text: String): UiComponent? =
    findAll { it.value?.contains(text) == true }.firstOrNull()
