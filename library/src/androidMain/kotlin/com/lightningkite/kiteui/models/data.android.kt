@file:Suppress("NOTHING_TO_INLINE")
package com.lightningkite.kiteui.models

import android.graphics.Typeface
import com.lightningkite.kiteui.views.AndroidAppContext
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlin.math.min

public actual typealias Font = Typeface

public actual val systemDefaultFont: Font  get() = Typeface.DEFAULT
public actual val systemDefaultFixedWidthFont: Font  get() = Typeface.MONOSPACE

//public actual sealed class ImageSource public actual constructor()
public actual typealias DimensionRaw = Float

public actual val Int.px: Dimension
    get() = Dimension(this.toFloat())
public actual val Int.rem: Dimension
    get() = Dimension((this * AndroidAppContext.oneRem))
public actual val Double.rem: Dimension
    get() = Dimension((this.toFloat() * AndroidAppContext.oneRem))
public actual val Int.dp: Dimension
    get() = Dimension((this * AndroidAppContext.density))
public actual val Double.dp: Dimension
    get() = Dimension((this.toFloat() * AndroidAppContext.density))

public actual inline operator fun Dimension.plus(other: Dimension): Dimension = Dimension(this.value + other.value)
public actual inline operator fun Dimension.minus(other: Dimension): Dimension = Dimension(this.value - other.value)
public actual inline operator fun Dimension.times(other: Float): Dimension = Dimension(this.value * other)
public actual inline operator fun Dimension.div(other: Float): Dimension = Dimension(
    if (other != 0f) {
        val dimenValue = this.value / other
        dimenValue
    } else {
        0f
    }
)
public actual inline fun Dimension.coerceAtMost(other: Dimension): Dimension = Dimension(this.value.coerceAtMost(other.value))
public actual inline fun Dimension.coerceAtLeast(other: Dimension): Dimension = Dimension(this.value.coerceAtLeast(other.value))

public actual sealed class ImageSource actual constructor()
public actual class ImageResource(public val resource: Int) : ImageSource()

public actual sealed class VideoSource actual constructor()
public actual class VideoResource(public val resource: Int) : VideoSource()

public actual sealed class AudioSource actual constructor()
public actual class AudioResource(public val resource: Int) : AudioSource()

public actual val Dimension.px: Double get() = value.toDouble()
public actual val Dimension.canvasUnits: Double get() = value.toDouble()