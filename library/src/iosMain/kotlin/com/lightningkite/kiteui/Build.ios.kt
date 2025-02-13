package com.lightningkite.kiteui

import platform.Foundation.NSBundle
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.Platform

actual object Build {
    actual val version: String get() = (NSBundle.mainBundle.infoDictionary?.get("CFBundleShortVersionString") as? String) ?: "???"
    @OptIn(ExperimentalNativeApi::class)
    actual val debug: Boolean get() = Platform.isDebugBinary
}