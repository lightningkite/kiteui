package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.Dimension
import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.ElementWriter
import com.lightningkite.kiteui.views.NativeContainerElement
import com.lightningkite.kiteui.views.NativeElement
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.reactive.core.*


public expect class CoordinatorFrame(context: ElementContext) : NativeContainerElement {
    public fun bottomSheet(
        peekSize: Dimension? = null,
        partialRatio: Float = 0.5f,
        draggable: Boolean = true,
        startState: BottomSheetState = BottomSheetState.EXPANDED,
        shouldRemoveExpandedCorners: Boolean = false,
        blockBehind: Boolean = false,
        content: ElementWriter.CanAddShownWhen.(control: BottomSheetControl) -> Unit
    )
    public fun leftSlidingPanel(
        ratio: Float? = null,
        blockBehind: Boolean = false,
        content: ElementWriter.CanAddShownWhen.(control: SlidingPanelControl) -> Unit
    )
    public fun rightSlidingPanel(
        ratio: Float? = null,
        blockBehind: Boolean = false,
        content: ElementWriter.CanAddShownWhen.(control: SlidingPanelControl) -> Unit
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

public expect class CoordinatorDragHandle(context: ElementContext): NativeElement {
    override val underlyingNativeElement: CoordinatorDragHandle     // this is necessary, I promise. You can try to get rid of it if you want, but you won't be able to.
}

