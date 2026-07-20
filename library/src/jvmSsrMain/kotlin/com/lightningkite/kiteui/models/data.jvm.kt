package com.lightningkite.kiteui.models

public actual val Dimension.px: Double get() = value.roughPx
public actual val Dimension.canvasUnits: Double get() = value.roughPx
public actual val Dimension.viewUnits: Double get() = value.roughPx