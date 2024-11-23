package com.lightningkite.kiteui.models

sealed class CornerRadius : CornerRadiusOptions {
    data class Constant(val value: Dimension): CornerRadius()
    data class RatioOfSpacing(val value: Float): CornerRadius()
    data class ForceConstant(val value: Dimension): CornerRadius()
    data class RatioOfSize(val ratio: Float = 0.5f): CornerRadius()
}

sealed interface CornerRadiusOptions

data class CornerRadii(
    val topStart: CornerRadius,
    val topEnd: CornerRadius,
    val bottomStart: CornerRadius,
    val bottomEnd: CornerRadius
) : CornerRadiusOptions {
    constructor(sameForAll: CornerRadius) : this(sameForAll, sameForAll, sameForAll, sameForAll)
}