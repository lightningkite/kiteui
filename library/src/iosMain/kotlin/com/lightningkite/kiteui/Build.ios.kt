package com.lightningkite.kiteui

import platform.Foundation.NSBundle
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.Platform

public actual object Build {
    public actual val version: String get() = (NSBundle.mainBundle.infoDictionary?.get("CFBundleShortVersionString") as? String) ?: "???"
    @OptIn(ExperimentalNativeApi::class)
    public actual val debug: Boolean get() = Platform.isDebugBinary
}