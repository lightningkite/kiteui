package com.lightningkite.kiteui.views.l2

import com.lightningkite.kiteui.models.Align

public sealed class RecyclerViewAnchor {
    public data class SpecificElement(
        public val index: Int,
        public val align: Align
    ): RecyclerViewAnchor()
    public data class FuzzyIndex(
        public val index: Double,
        public val ratioOfFocus: Double
    ): RecyclerViewAnchor()
}