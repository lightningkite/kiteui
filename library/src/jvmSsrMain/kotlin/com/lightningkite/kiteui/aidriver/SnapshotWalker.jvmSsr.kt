// by Claude - JVM/SSR snapshot walker: walks the view tree for server-side rendering
package com.lightningkite.kiteui.aidriver

import com.lightningkite.kiteui.navigation.PageNavigator
import com.lightningkite.kiteui.views.RView
import com.lightningkite.kiteui.views.RViewHelper
import com.lightningkite.kiteui.views.ariaDescription

// by Claude - relative IDs: named elements reset the path prefix for shorter, more readable IDs
// by Claude - hidden element filtering: skips invisible elements when includeLessVisible is false
actual suspend fun buildSnapshot(root: RView, navigator: PageNavigator?, settings: UiSnapshotSettings): UiSnapshot {
    val currentPage = navigator?.stack?.value?.lastOrNull()
    val pageName = currentPage?.let { it::class.simpleName } ?: "Unknown"
    val url = currentPage?.let {
        try {
            navigator.routes.render(it)?.urlLikePath?.render()
        } catch (_: Exception) {
            null
        }
    } ?: ""

    fun RView.toComponent(pathPrefix: String, indexAmongSiblings: Int): UiComponent? {
        val isVisible = shown && visible
        if (!isVisible && !settings.includeLessVisible) return null

        val segment = toPathSegment(indexAmongSiblings)
        val hasName = debugName != null || (this as? RViewHelper)?.ariaDescription != null
        val id = if (hasName) segment else if (pathPrefix.isEmpty()) segment else "$pathPrefix/$segment"
        val childPrefix = if (hasName) segment else id
        val helper = this as RViewHelper

        val value: String? = helper.accessibilityValue

        return UiComponent(
            id = id,
            type = helper.accessibilityType,
            value = value,
            enabled = helper.accessibilityEnabled,
            shown = shown, // by Claude - accurately reflect view's shown property
            visible = isVisible,
            actions = helper.accessibilityActions,
            children = activeChildren.mapIndexedNotNull { i, child -> child.toComponent(childPrefix, i) }
        )
    }

    return UiSnapshot(
        page = pageName,
        url = url,
        settings = settings,
        components = root.activeChildren.mapIndexedNotNull { i, child -> child.toComponent("", i) }
    )
}
