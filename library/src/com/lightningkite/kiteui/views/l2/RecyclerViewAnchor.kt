package com.lightningkite.kiteui.views.l2

import com.lightningkite.kiteui.models.Align

public sealed class RecyclerViewAnchor {
    public data class SpecificElement(
        val index: Int,
        val align: Align
    ): RecyclerViewAnchor()
    public data class FuzzyIndex(
        val index: Double,
        val ratioOfFocus: Double
    ): RecyclerViewAnchor()
}