package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.Dimension
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView
import com.lightningkite.kiteui.views.ViewModifiable
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.signal.Writable


public expect class CoordinatorFrame(context: RContext) : RView {
    fun bottomSheet(
        peekSize: Dimension? = null,
        partialRatio: Float = 0.5f,
        draggable: Boolean = true,
        startState: BottomSheetState = BottomSheetState.EXPANDED,
        shouldRemoveExpandedCorners: Boolean = false,
        blockBehind: Boolean = false,
        content: ViewWriter.(control: BottomSheetControl) -> ViewModifiable
    )
    fun leftSlidingPanel(
        ratio: Float? = null,
        blockBehind: Boolean = false,
        content: ViewWriter.(control: SlidingPanelControl) -> ViewModifiable
    )
    fun rightSlidingPanel(
        ratio: Float? = null,
        blockBehind: Boolean = false,
        content: ViewWriter.(control: SlidingPanelControl) -> ViewModifiable
    )
    fun onLeftSwipe(action: suspend () -> Unit)
    fun onRightSwipe(action: suspend () -> Unit)
}

enum class BottomSheetState {
    EXPANDED,
    PARTIALLY_EXPANDED,
    COLLAPSED
}

interface BottomSheetControl {
    val state: Writable<BottomSheetState>
    fun close()
}

interface SlidingPanelControl {
    fun close()
}

public expect class CoordinatorDragHandle(context: RContext): RView

