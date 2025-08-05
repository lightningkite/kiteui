package com.lightningkite.kiteui.models

import com.lightningkite.kiteui.InternalKiteUi

@InternalKiteUi
public actual val Dimension.px: Double get() = value.roughPx
@InternalKiteUi
public actual val Dimension.canvasUnits: Double get() = value.roughPx
@InternalKiteUi
public actual val Dimension.viewUnits: Double get() = value.roughPx