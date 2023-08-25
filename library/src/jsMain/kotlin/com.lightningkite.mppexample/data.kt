package com.lightningkite.mppexample

actual typealias DimensionRaw = String
actual val Int.px: Dimension
    get() = Dimension("${this}px")

actual val Int.rem: Dimension
    get() = Dimension("${this}rem")

actual inline operator fun Dimension.plus(other: Dimension): Dimension = Dimension("calc(${this.value} + ${other.value})")

actual typealias Font = String

actual val systemDefaultFont: Font get() = "Helvetica"

actual sealed class ImageSource actual constructor()
actual class ImageResource(val relativeUrl: String) : ImageSource()
