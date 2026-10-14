package com.lightningkite.kiteui.views.direct

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.views.*

public actual class CoordinatorFrame actual constructor(context: ElementContext) : NativeContainerElement(context) {
    public actual fun bottomSheet(
        peekSize: Dimension?,
        partialRatio: Float,
        draggable: Boolean,
        startState: BottomSheetState,
        shouldRemoveExpandedCorners: Boolean,
        blockBehind: Boolean,
        content: ElementWriter.CanAddShownWhen.(control: BottomSheetControl) -> Unit
    ): Unit = TODO("Compose backend: CoordinatorFrame.bottomSheet")

    public actual fun leftSlidingPanel(ratio: Float?, blockBehind: Boolean, content: ElementWriter.CanAddShownWhen.(control: SlidingPanelControl) -> Unit): Unit =
        TODO("Compose backend: CoordinatorFrame.leftSlidingPanel")

    public actual fun rightSlidingPanel(ratio: Float?, blockBehind: Boolean, content: ElementWriter.CanAddShownWhen.(control: SlidingPanelControl) -> Unit): Unit =
        TODO("Compose backend: CoordinatorFrame.rightSlidingPanel")

    // Swipes are touch gestures; desktop has none.
    public actual fun onLeftSwipe(action: suspend () -> Unit) {}
    public actual fun onRightSwipe(action: suspend () -> Unit) {}

    @Composable
    override fun Content(modifier: Modifier) {
        Layout(content = { ShownChildren() }, modifier = modifier, measurePolicy = FrameMeasurePolicy)
    }
}

public actual class CoordinatorDragHandle actual constructor(context: ElementContext) : NativeElement(context) {
    init { throw NotImplementedError("Compose backend: CoordinatorDragHandle is not implemented yet") }

    actual override val underlyingNativeElement: CoordinatorDragHandle get() = this

    @Composable
    override fun Content(modifier: Modifier) {}
}
