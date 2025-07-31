package com.lightningkite.kiteui

import com.lightningkite.kiteui.navigation.DefaultJson
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.serialization.Serializable


public actual object Build {
    public actual val version: String get() = window.asDynamic().version.toString()
    public actual val debug: Boolean get() = (window.asDynamic().debug as? Boolean) == true
}