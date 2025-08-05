package com.lightningkite.kiteui

@InternalKiteUi
public actual fun clockMillis(): Double {
    return System.currentTimeMillis().toDouble()
}