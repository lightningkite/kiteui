// by Claude - shared buildSnapshot implementation for interactive platforms (android, ios, js)
package com.lightningkite.kiteui.aidriver

import com.lightningkite.kiteui.navigation.PageNavigator
import com.lightningkite.kiteui.views.RView
import com.lightningkite.kiteui.views.RViewHelper
import com.lightningkite.kiteui.views.ariaDescription

/**
 * Shared [buildSnapshot] implementation for Android, iOS, and JS.
 * by Claude - uses [RViewHelper.accessibilityType] and [RViewHelper.accessibilityActions]
 */
actual suspend fun buildSnapshot(root: RView, navigator: PageNavigator?): UiSnapshot {
    val currentPage = navigator?.stack?.value?.lastOrNull()
    val pageName = currentPage?.let { it::class.simpleName } ?: "Unknown"
    val url = currentPage?.let {
        try {
            navigator.routes.render(it)?.urlLikePath?.render()
        } catch (_: Exception) {
            null
        }
    } ?: ""

    fun RView.toComponent(pathPrefix: String, indexAmongSiblings: Int): UiComponent {
        val segment = toPathSegment(indexAmongSiblings)
        val id = if (pathPrefix.isEmpty()) segment else "$pathPrefix/$segment"
        val helper = this as RViewHelper

        val value = helper.accessibilityValue
            ?: helper.ariaDescription
            ?: debugName

        return UiComponent(
            id = id,
            type = helper.accessibilityType,
            value = value,
            // by Claude - uses accessibilityEnabled which checks both ignoreInteraction and native disabled
            enabled = helper.accessibilityEnabled,
            visible = shown && visible,
            actions = helper.accessibilityActions,
            // by Claude - use activeChildren to skip stale SwapView children
            children = activeChildren.mapIndexed { i, child -> child.toComponent(id, i) }
        )
    }

    return UiSnapshot(
        page = pageName,
        url = url,
        settings = UiSnapshotSettings(),
        components = root.activeChildren.mapIndexed { i, child -> child.toComponent("", i) }
    )
}
