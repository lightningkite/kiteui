@file:Suppress("NOTHING_TO_INLINE")
package com.lightningkite.kiteui.models

import android.graphics.Typeface
import com.lightningkite.kiteui.InternalKiteUi
import com.lightningkite.kiteui.views.AndroidAppContext
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlin.math.min

@InternalKiteUi
public actual typealias Font = Typeface

@InternalKiteUi
public actual val systemDefaultFont: Font  get() = Typeface.DEFAULT
@InternalKiteUi
public actual val systemDefaultFixedWidthFont: Font  get() = Typeface.MONOSPACE

//public actual sealed class ImageSource public actual constructor()
@InternalKiteUi
public actual typealias DimensionRaw = Float

@InternalKiteUi
public actual val Int.px: Dimension
    get() = Dimension(this.toFloat())
@InternalKiteUi
public actual val Int.rem: Dimension
    get() = Dimension((this * AndroidAppContext.oneRem))
@InternalKiteUi
public actual val Double.rem: Dimension
    get() = Dimension((this.toFloat() * AndroidAppContext.oneRem))
@InternalKiteUi
public actual val Int.dp: Dimension
    get() = Dimension((this * AndroidAppContext.density))
@InternalKiteUi
public actual val Double.dp: Dimension
    get() = Dimension((this.toFloat() * AndroidAppContext.density))

@InternalKiteUi
public actual inline operator fun Dimension.plus(other: Dimension): Dimension = Dimension(this.value + other.value)
@InternalKiteUi
public actual inline operator fun Dimension.minus(other: Dimension): Dimension = Dimension(this.value - other.value)
@InternalKiteUi
public actual inline operator fun Dimension.times(other: Float): Dimension = Dimension(this.value * other)
@InternalKiteUi
public actual inline operator fun Dimension.div(other: Float): Dimension = Dimension(
    if (other != 0f) {
        val dimenValue = this.value / other
        dimenValue
    } else {
        0f
    }
)
@InternalKiteUi
public actual inline fun Dimension.coerceAtMost(other: Dimension): Dimension = Dimension(this.value.coerceAtMost(other.value))
@InternalKiteUi
public actual inline fun Dimension.coerceAtLeast(other: Dimension): Dimension = Dimension(this.value.coerceAtLeast(other.value))

@InternalKiteUi
public actual sealed class ImageSource actual constructor()
@InternalKiteUi
public actual class ImageResource(public val resource: Int) : ImageSource()

@InternalKiteUi
public actual sealed class VideoSource actual constructor()
@InternalKiteUi
public actual class VideoResource(public val resource: Int) : VideoSource()

@InternalKiteUi
public actual sealed class AudioSource actual constructor()
@InternalKiteUi
public actual class AudioResource(public val resource: Int) : AudioSource()

@InternalKiteUi
public actual val Dimension.px: Double get() = value.toDouble()
@InternalKiteUi
public actual val Dimension.canvasUnits: Double get() = value.toDouble()
@InternalKiteUi
public actual val Dimension.viewUnits: Double get() = value.toDouble()