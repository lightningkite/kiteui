package com.lightningkite.rock.models

import android.graphics.Typeface
import android.graphics.drawable.Drawable
import com.lightningkite.rock.views.AndroidAppContext

actual typealias Font = Typeface

actual val systemDefaultFont: Font
    get() = Typeface.DEFAULT

//actual sealed class ImageSource actual constructor()
actual typealias DimensionRaw = Int

actual val Int.px: Dimension
    get() = Dimension(this)
actual val Int.rem: Dimension
    get() = Dimension((this * AndroidAppContext.oneRem).toInt())
actual val Double.rem: Dimension
    get() = this.toInt().rem

actual inline operator fun Dimension.plus(other: Dimension): Dimension = Dimension(this.value + other.value)
actual inline operator fun Dimension.minus(other: Dimension): Dimension = Dimension(this.value - other.value)
actual inline operator fun Dimension.times(other: Float): Dimension = Dimension(this.value * other.toInt())
actual inline operator fun Dimension.div(other: Float): Dimension = Dimension(
    if (other != 0f) {
        val dimenValue = this.value.toFloat() / other
        dimenValue.toInt()
    } else {
        0
    }
)

actual sealed class ImageSource actual constructor()
actual class ImageResource(val drawable: Drawable) : ImageSource()