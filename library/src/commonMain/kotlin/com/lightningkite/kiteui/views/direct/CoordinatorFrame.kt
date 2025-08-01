package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.Dimension
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView
import com.lightningkite.kiteui.views.ViewModifiable
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*


public expect class CoordinatorFrame(context: RContext) : RView {
    public fun bottomSheet(
        peekSize: Dimension? = null,
        partialRatio: Float = 0.5f,
        draggable: Boolean = true,
        startState: BottomSheetState = BottomSheetState.EXPANDED,
        shouldRemoveExpandedCorners: Boolean = false,
        blockBehind: Boolean = false,
        content: ViewWriter.(control: BottomSheetControl) -> ViewModifiable
    )
    public fun leftSlidingPanel(
        ratio: Float? = null,
        blockBehind: Boolean = false,
        content: ViewWriter.(control: SlidingPanelControl) -> ViewModifiable
    )
    public fun rightSlidingPanel(
        ratio: Float? = null,
        blockBehind: Boolean = false,
        content: ViewWriter.(control: SlidingPanelControl) -> ViewModifiable
    )
    public fun onLeftSwipe(action: suspend () -> Unit)
    public fun onRightSwipe(action: suspend () -> Unit)
}

public enum class BottomSheetState {
    EXPANDED,
    PARTIALLY_EXPANDED,
    COLLAPSED
}

public interface BottomSheetControl {
    public val state: MutableReactive<BottomSheetState>
    public fun close()
}

public interface SlidingPanelControl {
    public fun close()
}

public expect class CoordinatorDragHandle(context: RContext): RView

