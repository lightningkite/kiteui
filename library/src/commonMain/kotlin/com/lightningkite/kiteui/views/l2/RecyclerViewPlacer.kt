package com.lightningkite.kiteui.views.l2

import com.lightningkite.kiteui.models.Rect
import com.lightningkite.kiteui.models.Size
import com.lightningkite.kiteui.views.ViewWriter

public interface RecyclerViewPlacer {
    public fun place(
        dataRange: IntRange,
        anchor: RecyclerViewAnchor?,
        previousViewport: Rect,
        existingCells: List<RecyclerViewPlaceable>,
        getNewCell: (Int, Size) -> RecyclerViewPlaceable,
        viewport: Rect,
        overdraw: Rect,
        paddingTop: Double,
        paddingLeft: Double,
        paddingRight: Double,
        paddingBottom: Double,
        gap: Double,
    )

    public fun prebake(
        prebakeRange: IntRange,
        dataRange: IntRange,
        writer: ViewWriter,
        render: ViewWriter.(Int) -> Unit
    )
}