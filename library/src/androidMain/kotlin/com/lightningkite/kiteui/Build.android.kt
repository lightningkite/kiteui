package com.lightningkite.kiteui

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import com.lightningkite.kiteui.views.AndroidAppContext


actual object Build {
    actual val version: String get() {
        val pInfo: PackageInfo = AndroidAppContext.applicationCtx.getPackageManager().getPackageInfo(AndroidAppContext.applicationCtx.getPackageName(), 0)
        return pInfo.versionName
    }
    actual val debug: Boolean get() {
        return AndroidAppContext.applicationCtx.applicationInfo.flags.and(ApplicationInfo.FLAG_DEBUGGABLE) != 0
    }
}