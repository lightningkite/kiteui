package com.lightningkite.kiteui.views.l2

import com.lightningkite.kiteui.models.Align

sealed class RecyclerViewAnchor {
    data class SpecificElement(
        val index: Int,
        val align: Align
    ): RecyclerViewAnchor()
    data class FuzzyIndex(
        val index: Double,
        val ratioOfFocus: Double
    ): RecyclerViewAnchor()
}