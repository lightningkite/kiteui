package com.lightningkite.kiteui.utils


import com.lightningkite.kiteui.InternalKiteUi
import platform.Foundation.NSBundle

@InternalKiteUi
public actual fun getAppVersion(): String =
    NSBundle.Companion.mainBundle.objectForInfoDictionaryKey("CFBundleShortVersionString") as? String ?: ""