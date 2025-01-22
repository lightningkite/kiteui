package com.lightningkite.kiteui.views.l2

import com.lightningkite.kiteui.models.Size

interface RecyclerViewPlaceable {
    val index: Int
    val item: Any?
    val size: Size

    val left: Double
    val top: Double
    val right: Double
    val bottom: Double
    val centerX: Double get() = (left + right) / 2
    val centerY: Double get() = (top + bottom) / 2
    fun place(left: Double, top: Double, right: Double, bottom: Double)

    val type: RecyclerViewRenderer<*>
}