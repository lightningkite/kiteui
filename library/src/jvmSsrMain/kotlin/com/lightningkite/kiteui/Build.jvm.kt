package com.lightningkite.kiteui

actual object Build {
    private var _version: String = ""
    actual val version: String get() = _version
    private var _debug: Boolean = false
    actual val debug: Boolean get() = _debug
}