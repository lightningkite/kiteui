@file:Suppress("NOTHING_TO_INLINE")

package com.lightningkite.kiteui.models

import androidx.compose.ui.unit.Dp
import com.lightningkite.kiteui.reactive.AppState


// Dimensions are Compose dp. CSS px are also density-independent, so px == dp here, as on the web.
public actual typealias DimensionRaw = Float

private const val DP_PER_REM = 16f

public actual val Int.px: Dimension get() = Dimension(toFloat())
public actual val Int.rem: Dimension get() = Dimension(this * DP_PER_REM)
public actual val Double.rem: Dimension get() = Dimension(toFloat() * DP_PER_REM)
public actual val Int.dp: Dimension get() = Dimension(toFloat())
public actual val Double.dp: Dimension get() = Dimension(toFloat())

public actual val Dimension.px: Double get() = value.toDouble()
public actual val Dimension.viewUnits: Double get() = value.toDouble()
// A canvas draws in the window's pixels, as on every other platform.
public actual val Dimension.canvasUnits: Double get() = value.toDouble() * AppState.windowInfo.value.density

public actual inline operator fun Dimension.plus(other: Dimension): Dimension = Dimension(value + other.value)
public actual inline operator fun Dimension.minus(other: Dimension): Dimension = Dimension(value - other.value)
public actual inline operator fun Dimension.times(other: Float): Dimension = Dimension(value * other)
public actual inline operator fun Dimension.div(other: Float): Dimension = Dimension(value / other)
public actual inline fun Dimension.coerceAtMost(other: Dimension): Dimension = Dimension(value.coerceAtMost(other.value))
public actual inline fun Dimension.coerceAtLeast(other: Dimension): Dimension = Dimension(value.coerceAtLeast(other.value))

public fun Dimension.toDp(): Dp = Dp(value)
