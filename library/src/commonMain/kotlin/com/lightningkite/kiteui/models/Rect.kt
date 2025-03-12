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

    val size: Size get() = Size(width, height)
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

    fun offset(x: Double, y: Double) = copy(left + x)
}

data class Edges(
    val left: Dimension,
    val top: Dimension,
    val right: Dimension,
    val bottom: Dimension
) {
    constructor(horizontal: Dimension, vertical: Dimension):this(left = horizontal, right = horizontal, top = vertical, bottom = vertical)
    companion object {
        val ZERO = Edges(0.px)
    }
    val horizontalSum get() = left + right
    val verticalSum get() = top + bottom
    constructor(dimension: Dimension): this(dimension, dimension, dimension, dimension)
    operator fun plus(other: Edges) = Edges(left + other.left, top + other.top, right + other.right, bottom + other.bottom)
    operator fun minus(other: Edges) = Edges(left - other.left, top - other.top, right - other.right, bottom - other.bottom)
    operator fun times(other: Int) = Edges(left * other, top * other, right * other, bottom * other)
    operator fun div(other: Int) = Edges(left / other, top / other, right / other, bottom / other)
    operator fun times(other: Float) = Edges(left * other, top * other, right * other, bottom * other)
    operator fun div(other: Float) = Edges(left / other, top / other, right / other, bottom / other)
    operator fun times(other: Double) = Edges(left * other, top * other, right * other, bottom * other)
    operator fun div(other: Double) = Edges(left / other, top / other, right / other, bottom / other)
}