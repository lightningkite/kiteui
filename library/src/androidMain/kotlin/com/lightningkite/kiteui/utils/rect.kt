package com.lightningkite.kiteui.utils

import android.view.View
import com.lightningkite.kiteui.models.Rect

fun View.getBoundariesInWindow(): Rect {
    val posInWindow = IntArray(2)
    getLocationInWindow(posInWindow)
    return Rect(
        posInWindow[0].toDouble(),
        posInWindow[1].toDouble(),
        posInWindow[0].toDouble() + width,
        posInWindow[1].toDouble() + height
    )
}