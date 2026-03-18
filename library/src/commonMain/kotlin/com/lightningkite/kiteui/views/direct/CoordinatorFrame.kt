package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.Dimension
import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.RView
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.reactive.core.*


expect class CoordinatorFrame(context: ElementContext) : RView {
    fun bottomSheet(
        peekSize: Dimension? = null,
        partialRatio: Float = 0.5f,
        draggable: Boolean = true,
        startState: BottomSheetState = BottomSheetState.EXPANDED,
        shouldRemoveExpandedCorners: Boolean = false,
        blockBehind: Boolean = false,
        content: ViewWriter.(control: BottomSheetControl) -> Unit
    )
    fun leftSlidingPanel(
        ratio: Float? = null,
        blockBehind: Boolean = false,
        content: ViewWriter.(control: SlidingPanelControl) -> Unit
    )
    fun rightSlidingPanel(
        ratio: Float? = null,
        blockBehind: Boolean = false,
        content: ViewWriter.(control: SlidingPanelControl) -> Unit
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
    val state: MutableReactive<BottomSheetState>
    fun close()
}

interface SlidingPanelControl {
    fun close()
}

expect class CoordinatorDragHandle(context: ElementContext): RView

