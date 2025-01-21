package com.lightningkite.kiteui.utils


import platform.Foundation.NSBundle

actual fun getAppVersion(): String =
    NSBundle.Companion.mainBundle.objectForInfoDictionaryKey("CFBundleShortVersionString") as? String ?: ""