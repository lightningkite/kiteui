package com.lightningkite.kiteui

public actual object Build {
    public actual val version: String get() = com.lightningkite.kiteui.utils.getAppVersion()
    public actual val debug: Boolean get() = Platform.isDevelopment
}
