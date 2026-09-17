package com.lightningkite.kiteui

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import com.lightningkite.kiteui.views.AndroidAppContext


public actual object Build {
    public actual val version: String
        get() {
            try {
                val pInfo: PackageInfo = AndroidAppContext.applicationCtx.packageManager.getPackageInfo(
                    AndroidAppContext.applicationCtx.packageName,
                    0
                )
                return pInfo.versionName ?: "?"
            } catch (e: Exception) {
                return "?"
            }
        }
    public actual val debug: Boolean
        get() {
            try {
                return AndroidAppContext.applicationCtx.applicationInfo.flags.and(ApplicationInfo.FLAG_DEBUGGABLE) != 0
            } catch (e: Exception) {
                return false
            }
        }
}