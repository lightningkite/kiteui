package com.lightningkite.kiteui.utils

import com.lightningkite.kiteui.InternalKiteUi
import com.lightningkite.kiteui.views.AndroidAppContext

@InternalKiteUi
public actual fun getAppVersion(): String {
    try {
        val packageInfo = AndroidAppContext.applicationCtx.packageManager.getPackageInfo(AndroidAppContext.applicationCtx.packageName, 0)
        return packageInfo.versionName ?:""
    } catch (e: Exception) {
        ""
    }
    return ""
}