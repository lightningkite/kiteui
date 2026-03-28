package com.lightningkite.kiteui

import com.lightningkite.kiteui.navigation.DefaultJson
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.serialization.Serializable


actual object Build {
    actual val version: String get() = window.asDynamic()?.version?.toString() ?: "Unknown"
    actual val debug: Boolean get() = (window.asDynamic()?.debug as? Boolean) == true
}