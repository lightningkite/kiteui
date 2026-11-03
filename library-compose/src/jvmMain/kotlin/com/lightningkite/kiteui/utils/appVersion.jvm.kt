package com.lightningkite.kiteui.utils

// jpackage (which Compose Desktop packaging uses) sets this; unpackaged runs have no version.
public actual fun getAppVersion(): String = System.getProperty("jpackage.app-version") ?: ""
