// by Claude - local backend for UiTestScope, wraps in-process view tree primitives
package com.lightningkite.kiteui.testing

import com.lightningkite.kiteui.GeolocationResult
import com.lightningkite.kiteui.MockExternalServices
import com.lightningkite.kiteui.aidriver.*
import com.lightningkite.kiteui.createFileReferenceFromBytes
import com.lightningkite.kiteui.navigation.PageNavigator
import com.lightningkite.kiteui.views.RView

/**
 * [UiTestBackend] that operates on an in-process view tree.
 * Used by [uiTest] for local testing on all platforms.
 */
class LocalUiTestBackend(
    val root: RView,
    val navigator: PageNavigator?,
    private val idle: suspend () -> Unit = {},
) : UiTestBackend {
    override suspend fun snapshot(): UiSnapshot {
        idle()
        return buildSnapshot(root, navigator)
    }

    override suspend fun perform(action: UiAction): ActionDispatchResult {
        val result = dispatchAction(action, root, navigator)
        idle()
        return result
    }

    override suspend fun logs(lines: Int): List<LogEntry> =
        AiDriverLogBuffer.entries(lines)

    // by Claude - mock support for local tests
    private fun ensureMock(): MockExternalServices {
        val existing = root.context.addons["externalServices"]
        if (existing is MockExternalServices) return existing
        val mock = MockExternalServices(delegate = existing as? com.lightningkite.kiteui.ExternalServicesAccess)
        root.context.addons["externalServices"] = mock
        return mock
    }

    override suspend fun mockFile(bytes: ByteArray, mimeType: String, fileName: String) {
        ensureMock().pendingFileResponses.add(createFileReferenceFromBytes(bytes, mimeType, fileName))
    }

    override suspend fun mockGeolocation(latitude: Double, longitude: Double, accuracyInMeters: Double) {
        ensureMock().pendingGeolocation.add(GeolocationResult(latitude, longitude, accuracyInMeters))
    }
}
