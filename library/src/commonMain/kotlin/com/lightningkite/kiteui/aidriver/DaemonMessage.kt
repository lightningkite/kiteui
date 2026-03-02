// by Claude - messages sent from the AI driver daemon → app
package com.lightningkite.kiteui.aidriver

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Messages sent from the AI driver daemon → connected KiteUI app.
 */
@Serializable
sealed class DaemonMessage {
    /** Request the app to capture and return a UI snapshot. */
    @Serializable @SerialName("requestSnapshot")
    data class RequestSnapshot(val requestId: String, val component: String? = null) : DaemonMessage()

    /** Request the app to capture and return a screenshot. */
    @Serializable @SerialName("requestScreenshot")
    data class RequestScreenshot(val requestId: String) : DaemonMessage()

    /** Request the app to execute a UI action and return the result. */
    @Serializable @SerialName("performAction")
    data class PerformAction(val requestId: String, val action: UiAction) : DaemonMessage()

    // by Claude - queue a mock file response on the app's MockExternalServices
    @Serializable @SerialName("queueMockFile")
    data class QueueMockFile(
        val requestId: String,
        val base64: String,
        val mimeType: String,
        val fileName: String
    ) : DaemonMessage()

    // by Claude - queue a mock geolocation response on the app's MockExternalServices
    @Serializable @SerialName("queueMockGeolocation")
    data class QueueMockGeolocation(
        val requestId: String,
        val latitude: Double,
        val longitude: Double,
        val accuracyInMeters: Double = 10.0
    ) : DaemonMessage()
}
