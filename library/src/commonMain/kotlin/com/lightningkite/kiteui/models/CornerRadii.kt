package com.lightningkite.kiteui.models

public sealed class CornerRadii {
    public data class AdaptiveToSpacing(val value: Dimension) : CornerRadii()
    public data class RatioOfSpacing(val value: Float) : CornerRadii()
    public data class Fixed(val value: Dimension) : CornerRadii()
    public data class RatioOfSize(val ratio: Float = 0.5f) : CornerRadii()
    public data class PerCorner(
        val value: Dimension,
        val topLeft: Boolean = false,
        val topRight: Boolean = false,
        val bottomLeft: Boolean = false,
        val bottomRight: Boolean = false,
    ) : CornerRadii()
    public companion object {
        @Deprecated("Use Fixed instead", ReplaceWith("Fixed(value)"))
        public fun ForceConstant(value: Dimension) = Fixed(value)
        @Deprecated("Use AdaptiveToSpacing instead", ReplaceWith("AdaptiveToSpacing(value)"))
        public fun Constant(value: Dimension) = AdaptiveToSpacing(value)
    }
}

public fun CornerRadii(value: Dimension): CornerRadii.Fixed = CornerRadii.Fixed(value)
