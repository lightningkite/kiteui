package com.lightningkite.kiteui.views.direct

import androidx.compose.runtime.Composable
import com.lightningkite.kiteui.models.Dimension
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView
import com.lightningkite.kiteui.views.ViewModifiable
import com.lightningkite.kiteui.views.ViewWriter

actual class CoordinatorFrame actual constructor(context: RContext) :
    RView(context) {
    actual fun bottomSheet(
        peekSize: Dimension?,
        partialRatio: Float,
        draggable: Boolean,
        startState: BottomSheetState,
        shouldRemoveExpandedCorners: Boolean,
        blockBehind: Boolean,
        content: ViewWriter.(control: BottomSheetControl) -> ViewModifiable
    ) {
    }

    actual fun leftSlidingPanel(
        ratio: Float?,
        blockBehind: Boolean,
        content: ViewWriter.(control: SlidingPanelControl) -> ViewModifiable
    ) {
    }

    actual fun rightSlidingPanel(
        ratio: Float?,
        blockBehind: Boolean,
        content: ViewWriter.(control: SlidingPanelControl) -> ViewModifiable
    ) {
    }

    actual fun onLeftSwipe(action: suspend () -> Unit) {
    }

    actual fun onRightSwipe(action: suspend () -> Unit) {
    }

    @Composable
    override fun compose() {
        TODO("Not yet implemented")
    }
}

actual class CoordinatorDragHandle actual constructor(context: RContext) :
    RView(context) {
    @Composable
    override fun compose() {
        TODO("Not yet implemented")
    }
}