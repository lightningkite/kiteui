package com.lightningkite.kiteui.views.l2

import com.lightningkite.kiteui.models.Size

public interface RecyclerViewPlaceable {
    public val index: Int
    public val item: Any?
    public val size: Size

    public val left: Double
    public val top: Double
    public val right: Double
    public val bottom: Double
    public val centerX: Double get() = (left + right) / 2
    public val centerY: Double get() = (top + bottom) / 2
    public fun place(left: Double, top: Double, right: Double, bottom: Double)

    public val type: RecyclerViewRenderer<*>
}