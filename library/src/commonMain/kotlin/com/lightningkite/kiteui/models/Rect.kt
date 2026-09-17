package com.lightningkite.kiteui.models

import kotlin.js.JsName
import kotlin.jvm.JvmName

public data class Size(
    val width: Double,
    val height: Double
) {
    public companion object {
        public val Zero: Size = Size(0.0, 0.0)
    }
}

public data class Rect(
    val left: Double,
    val top: Double,
    val right: Double,
    val bottom: Double
) {
    public companion object {
        public val Zero: Rect = Rect(0.0, 0.0, 0.0, 0.0)
        public fun fromSize(size: Size, x: Double = 0.0, y: Double = 0.0): Rect = Rect(x, y, x + size.width, y + size.height)
        public fun fromSize(left: Double = 0.0, top: Double = 0.0, width: Double, height: Double): Rect = Rect(left, top, left + width, top + height)
    }

    val size: Size get() = Size(width, height)
    val centerX: Double get() = (left + right) / 2
    val centerY: Double get() = (top + bottom) / 2
    val width: Double get() = right - left
    val height: Double get() = bottom - top

    public fun shift(dx: Double, dy: Double): Rect = copy(
        left = left + dx,
        top = top + dy,
        right = right + dx,
        bottom = bottom + dy
    )

    public fun offset(x: Double, y: Double): Rect = copy(
        left = left + x,
        top = top + y,
        right = right + x,
        bottom = bottom + y
    )
}

public data class Edges(
    val left: Dimension,
    val top: Dimension,
    val right: Dimension,
    val bottom: Dimension
) {
    public constructor(horizontal: Dimension, vertical: Dimension):this(left = horizontal, right = horizontal, top = vertical, bottom = vertical)
    public companion object {
        public val ZERO: Edges = Edges(0.px)
    }
    val horizontalSum: Dimension get() = left + right
    val verticalSum: Dimension get() = top + bottom
    public constructor(dimension: Dimension): this(dimension, dimension, dimension, dimension)
    public operator fun plus(other: Edges): Edges = Edges(left + other.left, top + other.top, right + other.right, bottom + other.bottom)
    @JvmName("plusEdgesNullable")
    @JsName("plusEdgesNullable")
    public operator fun plus(other: Edges?): Edges = if(other == null) this else this + other
    public operator fun minus(other: Edges): Edges = Edges(left - other.left, top - other.top, right - other.right, bottom - other.bottom)
    public operator fun times(other: Int): Edges = Edges(left * other, top * other, right * other, bottom * other)
    public operator fun div(other: Int): Edges = Edges(left / other, top / other, right / other, bottom / other)
    public operator fun times(other: Float): Edges = Edges(left * other, top * other, right * other, bottom * other)
    public operator fun div(other: Float): Edges = Edges(left / other, top / other, right / other, bottom / other)
    public operator fun times(other: Double): Edges = Edges(left * other, top * other, right * other, bottom * other)
    public operator fun div(other: Double): Edges = Edges(left / other, top / other, right / other, bottom / other)
}