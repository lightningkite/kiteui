package com.lightningkite.kiteui.models

import java.awt.Font as AwtFont

// Dimension types
actual typealias DimensionRaw = Double

actual val Dimension.px: Double get() = value
actual val Dimension.canvasUnits: Double get() = value
actual val Dimension.viewUnits: Double get() = value

// Dimension operators
actual operator fun Dimension.plus(other: Dimension): Dimension = Dimension(value + other.value)
actual operator fun Dimension.minus(other: Dimension): Dimension = Dimension(value - other.value)
actual operator fun Dimension.times(other: Float): Dimension = Dimension(value * other)
actual operator fun Dimension.div(other: Float): Dimension = Dimension(value / other)

actual fun Dimension.coerceAtMost(other: Dimension): Dimension =
    Dimension(value.coerceAtMost(other.value))
actual fun Dimension.coerceAtLeast(other: Dimension): Dimension =
    Dimension(value.coerceAtLeast(other.value))

// Dimension constructors
actual val Int.px: Dimension get() = Dimension(this.toDouble())
actual val Int.rem: Dimension get() = Dimension(this.toDouble() * 16)
actual val Int.dp: Dimension get() = Dimension(this.toDouble())
actual val Double.rem: Dimension get() = Dimension(this * 16)
actual val Double.dp: Dimension get() = Dimension(this)

// Font
actual typealias Font = AwtFont
actual val systemDefaultFont: Font get() = AwtFont("SansSerif", AwtFont.PLAIN, 14)
actual val systemDefaultFixedWidthFont: Font get() = AwtFont("Monospaced", AwtFont.PLAIN, 14)

// Media sources
actual sealed class ImageSource actual constructor() : VisualMediaSource
actual class ImageResource(val name: String) : ImageSource()

actual sealed class VideoSource actual constructor() : VisualMediaSource
actual class VideoResource(val name: String) : VideoSource()

actual sealed class AudioSource actual constructor()
actual class AudioResource(val name: String) : AudioSource()