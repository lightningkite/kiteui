package com.lightningkite.kiteui.models

actual val Dimension.px: Double get() = value.roughPx
actual val Dimension.canvasUnits: Double get() = value.roughPx