// by Claude - WebSocket message types between app and daemon
package com.lightningkite.kiteui.aidriver

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Messages sent from the KiteUI app → daemon.
 */
@Serializable
sealed class AppMessage {
    /** Sent immediately after connecting to identify this app instance. */
    @Serializable @SerialName("register")
    data class Register(val appName: String, val platform: String) : AppMessage()

    /** Response to a [DaemonMessage.RequestSnapshot]. */
    @Serializable @SerialName("snapshotResponse")
    data class SnapshotResponse(val requestId: String, val snapshot: UiSnapshot) : AppMessage()

    /** Response to a [DaemonMessage.RequestScreenshot]. */
    @Serializable @SerialName("screenshotResponse")
    data class ScreenshotResponse(
        val requestId: String,
        val error: String? = null,
        val base64: String? = null
    ) : AppMessage()

    /** Response to a [DaemonMessage.PerformAction]. */
    @Serializable @SerialName("actionResult")
    data class ActionResult(
        val requestId: String,
        val error: String? = null,
        val result: String? = null
    ) : AppMessage()

    /** Lightweight push notification when navigation or state changes. */
    @Serializable @SerialName("changed")
    data object Changed : AppMessage()
}
