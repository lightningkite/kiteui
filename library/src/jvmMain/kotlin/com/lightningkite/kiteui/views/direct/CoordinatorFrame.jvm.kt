package com.lightningkite.kiteui.views.direct

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
        // TODO: Implement bottomSheet
    }

    actual fun leftSlidingPanel(
        ratio: Float?,
        blockBehind: Boolean,
        content: ViewWriter.(control: SlidingPanelControl) -> ViewModifiable
    ) {
        // TODO: Implement leftSlidingPanel
    }

    actual fun rightSlidingPanel(
        ratio: Float?,
        blockBehind: Boolean,
        content: ViewWriter.(control: SlidingPanelControl) -> ViewModifiable
    ) {
        // TODO: Implement rightSlidingPanel
    }

    actual fun onLeftSwipe(action: suspend () -> Unit) {
        // TODO: Implement onLeftSwipe
    }

    actual fun onRightSwipe(action: suspend () -> Unit) {
        // TODO: Implement onRightSwipe
    }

    @Composable
    override fun compose() {
        Box(modifier = Modifier) {
            children.forEach { 
                it.compose()
            }
        }
    }
}

actual class CoordinatorDragHandle actual constructor(context: RContext) :
    RView(context) {
    @Composable
    override fun compose() {
        Box(modifier = Modifier) {
            children.forEach { 
                it.compose()
            }
        }
    }
}