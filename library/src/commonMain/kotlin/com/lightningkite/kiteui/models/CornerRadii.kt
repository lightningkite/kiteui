package com.lightningkite.kiteui.models

sealed class CornerRadii {
    data class Constant(val value: Dimension): CornerRadii()
    data class RatioOfSize(val ratio: Float = 0.5f): CornerRadii()
}