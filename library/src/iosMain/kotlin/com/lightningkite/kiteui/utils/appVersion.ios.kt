package com.lightningkite.kiteui.utils


import platform.Foundation.NSBundle

public actual fun getAppVersion(): String =
    NSBundle.Companion.mainBundle.objectForInfoDictionaryKey("CFBundleShortVersionString") as? String ?: ""