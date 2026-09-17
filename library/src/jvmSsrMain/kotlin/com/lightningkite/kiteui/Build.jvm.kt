package com.lightningkite.kiteui

public actual object Build {
    private var _version: String = ""
    public actual val version: String get() = _version
    private var _debug: Boolean = false
    public actual val debug: Boolean get() = _debug
}