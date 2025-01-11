package com.lightningkite.kiteui.models

data class Size(
    val width: Double,
    val height: Double
) {
    companion object {
        val Zero = Size(0.0, 0.0)
    }
}

data class Rect(
    val left: Double,
    val top: Double,
    val right: Double,
    val bottom: Double
) {
    companion object {
        val Zero = Rect(0.0, 0.0, 0.0, 0.0)
        fun fromSize(size: Size, x: Double = 0.0, y: Double = 0.0) = Rect(x, y, x + size.width, y + size.height)
        fun fromSize(left: Double = 0.0, top: Double = 0.0, width: Double, height: Double) = Rect(left, top, left + width, top + height)
    }
    val centerX: Double get() = (left + right) / 2
    val centerY: Double get() = (top + bottom) / 2
    val width: Double get() = right - left
    val height: Double get() = bottom - top

    fun shift(dx: Double, dy: Double) = copy(
        left = left + dx,
        top = top + dy,
        right = right + dx,
        bottom = bottom + dy
    )
}
