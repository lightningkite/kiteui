package com.lightningkite.kiteui

import com.lightningkite.kiteui.navigation.DefaultJson
import kotlinx.browser.document
import kotlinx.serialization.Serializable

@Serializable
data class VersionInfo(
    val version: String = "0.0.0",
    val debug: Boolean = false,
)

actual object Build {
    val versionInfo = document.getElementById("version-info")?.textContent?.let {
        DefaultJson.decodeFromString(VersionInfo.serializer(), it)
    }
    actual val version: String get() = versionInfo?.version ?: ""
    actual val debug: Boolean get() = versionInfo?.debug ?: false
}