package com.lightningkite.kiteui.models

import android.graphics.Typeface
import android.graphics.drawable.Drawable
import com.lightningkite.kiteui.views.AndroidAppContext

actual typealias Font = Typeface

actual val systemDefaultFont: Font  get() = Typeface.DEFAULT
actual val systemDefaultFixedWidthFont: Font  get() = Typeface.MONOSPACE

//actual sealed class ImageSource actual constructor()
actual typealias DimensionRaw = Float

actual val Int.px: Dimension
    get() = Dimension(this.toFloat())
actual val Int.rem: Dimension
    get() = Dimension((this * AndroidAppContext.oneRem))
actual val Double.rem: Dimension
    get() = Dimension((this.toFloat() * AndroidAppContext.oneRem))
actual val Int.dp: Dimension
    get() = Dimension((this * AndroidAppContext.density))
actual val Double.dp: Dimension
    get() = Dimension((this.toFloat() * AndroidAppContext.density))

actual inline operator fun Dimension.plus(other: Dimension): Dimension = Dimension(this.value + other.value)
actual inline operator fun Dimension.minus(other: Dimension): Dimension = Dimension(this.value - other.value)
actual inline operator fun Dimension.times(other: Float): Dimension = Dimension(this.value * other)
actual inline operator fun Dimension.div(other: Float): Dimension = Dimension(
    if (other != 0f) {
        val dimenValue = this.value / other
        dimenValue
    } else {
        0f
    }
)

actual sealed class ImageSource actual constructor()
actual class ImageResource(val resource: Int) : ImageSource()

actual sealed class VideoSource actual constructor()
actual class VideoResource(val resource: Int) : VideoSource()

actual sealed class AudioSource actual constructor()
actual class AudioResource(val resource: Int) : AudioSource()

actual val Dimension.px: Double get() = value.toDouble()
