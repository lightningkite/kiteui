package com.lightningkite.kiteui.utils

import com.lightningkite.kiteui.views.AndroidAppContext

public actual fun getAppVersion(): String {
    return try {
        val packageInfo = AndroidAppContext.applicationCtx.packageManager.getPackageInfo(AndroidAppContext.applicationCtx.packageName, 0)
        packageInfo.versionName ?: ""
    } catch (e: Exception) {
        ""
    }
}