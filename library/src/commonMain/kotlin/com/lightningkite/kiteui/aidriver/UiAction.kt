// by Claude - UI action types for AI driver
package com.lightningkite.kiteui.aidriver

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Actions the daemon can instruct a connected app to perform.
 * [targetId] is the absolute component path from [UiSnapshot.components].
 */
@Serializable
sealed class UiAction {
    @Serializable @SerialName("click")
    data class Click(val targetId: String) : UiAction()

    @Serializable @SerialName("longClick")
    data class LongClick(val targetId: String) : UiAction()

    @Serializable @SerialName("setValue")
    data class SetValue(val targetId: String, val value: String) : UiAction()

    @Serializable @SerialName("scroll")
    data class Scroll(val targetId: String, val dx: Float = 0f, val dy: Float = 0f) : UiAction()

    @Serializable @SerialName("navigate")
    data class Navigate(val route: String) : UiAction()

    @Serializable @SerialName("back")
    data object Back : UiAction()

    @Serializable @SerialName("forward")
    data object Forward : UiAction()

    @Serializable @SerialName("screenshot")
    data object Screenshot : UiAction()
}
