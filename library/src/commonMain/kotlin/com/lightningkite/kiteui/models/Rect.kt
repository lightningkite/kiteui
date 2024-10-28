package com.lightningkite.kiteui.models

data class Rect(
    val left: Double,
    val top: Double,
    val right: Double,
    val bottom: Double
) {
    val width: Double get() = right - left
    val height: Double get() = bottom - top

    fun shift(dx: Double, dy: Double) = copy(
        left = left + dx,
        top = top + dy,
        right = right + dx,
        bottom = bottom + dy
    )
}
