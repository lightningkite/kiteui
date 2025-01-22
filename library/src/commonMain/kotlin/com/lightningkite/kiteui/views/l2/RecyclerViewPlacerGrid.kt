package com.lightningkite.kiteui.views.l2

interface RecyclerViewPlacerGrid: RecyclerViewPlacer {
    fun withOrthogonalCount(count: Int): RecyclerViewPlacerGrid
}