package com.lightningkite.kiteui.models

actual val Dimension.px: Double get() = value.filter { it.isDigit() }.toDouble()