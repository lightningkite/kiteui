package com.lightningkite.kiteui.models

sealed class CornerRadii {
    data class AdaptiveToSpacing(val value: Dimension) : CornerRadii()
    data class RatioOfSpacing(val value: Float) : CornerRadii()
    data class Fixed(val value: Dimension) : CornerRadii()
    data class RatioOfSize(val ratio: Float = 0.5f) : CornerRadii()
    data class PerCorner(
        val value: Dimension,
        val topLeft: Boolean = false,
        val topRight: Boolean = false,
        val bottomLeft: Boolean = false,
        val bottomRight: Boolean = false,
    ) : CornerRadii()
    companion object {
        @Deprecated("Use Fixed instead", ReplaceWith("Fixed(value)"))
        fun ForceConstant(value: Dimension) = Fixed(value)
        @Deprecated("Use AdaptiveToSpacing instead", ReplaceWith("AdaptiveToSpacing(value)"))
        fun Constant(value: Dimension) = AdaptiveToSpacing(value)
    }
}

public fun CornerRadii(value: Dimension): CornerRadii.Fixed = CornerRadii.Fixed(value)
