package com.lightningkite.kiteui

public actual fun clockMillis(): Double {
    return System.currentTimeMillis().toDouble()
}