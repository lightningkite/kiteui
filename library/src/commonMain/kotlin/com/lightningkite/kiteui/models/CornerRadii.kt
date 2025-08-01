package com.lightningkite.kiteui.models

public sealed class CornerRadii {
    public data class Constant(val value: Dimension) : CornerRadii()
    public data class RatioOfSpacing(val value: Float) : CornerRadii()
    public data class ForceConstant(val value: Dimension) : CornerRadii()
    public data class RatioOfSize(val ratio: Float = 0.5f) : CornerRadii()
    public data class PerCorner(
        public val value: Dimension,
        public val topLeft: Boolean = false,
        public val topRight: Boolean = false,
        public val bottomLeft: Boolean = false,
        public val bottomRight: Boolean = false,
    ) : CornerRadii()
}