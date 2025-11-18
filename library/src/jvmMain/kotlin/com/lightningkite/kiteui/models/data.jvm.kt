package com.lightningkite.kiteui.models

import com.lightningkite.kiteui.Blob

actual val Dimension.px: Double get() = value.roughPx
actual val Dimension.canvasUnits: Double get() = value.roughPx
actual val Dimension.viewUnits: Double get() = value.roughPx
