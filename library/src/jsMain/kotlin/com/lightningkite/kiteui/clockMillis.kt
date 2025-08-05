package com.lightningkite.kiteui

import kotlin.js.Date

@InternalKiteUi
public actual fun clockMillis(): Double = Date.now()