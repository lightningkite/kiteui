package com.lightningkite.kiteui.views.direct

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.DraggableState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import com.lightningkite.kiteui.OverrideOnly
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.reactive.context.invoke
import com.lightningkite.reactive.context.reactive
import com.lightningkite.reactive.core.Signal

public actual class CoordinatorFrame actual constructor(context: ElementContext) : NativeContainerElement(context) {
    // Laid out like the web: a full-size column whose top part is an empty spacer, so the content takes the rest.
    public actual fun bottomSheet(
        peekSize: Dimension?,
        partialRatio: Float,
        draggable: Boolean,
        startState: BottomSheetState,
        shouldRemoveExpandedCorners: Boolean,
        blockBehind: Boolean,
        content: ElementWriter.CanAddShownWhen.(control: BottomSheetControl) -> Unit
    ) {
        val sheet = BottomSheet(this, Signal(startState), partialRatio, draggable)
        val control = object : BottomSheetControl {
            override val state = sheet.state
            override fun close() = sheet.close()
        }
        sheet.column = col {
            gap = 0.px
            ignoreInteraction = true
            context.bottomSheet = sheet
            sheet.above = frame { ignoreInteraction = true }
            beforeSetup { sheet.content = underlyingNativeElement }.content(control)
            reactive { sheet.fraction = sheet.fractionOf(sheet.state()) }
        }
    }

    public actual fun leftSlidingPanel(ratio: Float?, blockBehind: Boolean, content: ElementWriter.CanAddShownWhen.(control: SlidingPanelControl) -> Unit) {
        lateinit var panel: Element
        val control = object : SlidingPanelControl {
            override fun close() = removeChild(panel)
        }
        panel = if (ratio == null) {
            checkNotNull(produceAtMostOneView { align(Align.Start, Align.Stretch).content(control) })
        } else row {
            gap = 0.px
            ignoreInteraction = true
            weight(ratio).content(control)
            weight(1f - ratio).frame { ignoreInteraction = true }
        }
    }

    public actual fun rightSlidingPanel(ratio: Float?, blockBehind: Boolean, content: ElementWriter.CanAddShownWhen.(control: SlidingPanelControl) -> Unit) {
        lateinit var panel: Element
        val control = object : SlidingPanelControl {
            override fun close() = removeChild(panel)
        }
        panel = if (ratio == null) {
            checkNotNull(produceAtMostOneView { align(Align.End, Align.Stretch).content(control) })
        } else row {
            gap = 0.px
            ignoreInteraction = true
            weight(1f - ratio).frame { ignoreInteraction = true }
            weight(ratio).content(control)
        }
    }

    // Swipes are touch gestures; desktop has none.
    public actual fun onLeftSwipe(action: suspend () -> Unit) {}
    public actual fun onRightSwipe(action: suspend () -> Unit) {}

    @Composable
    override fun Content(modifier: Modifier) {
        Layout(content = { ShownChildren() }, modifier = modifier, measurePolicy = FrameMeasurePolicy)
    }
}

private var ElementContext.bottomSheet: BottomSheet? by contextAddon(null)

private class BottomSheet(
    val frame: CoordinatorFrame,
    val state: Signal<BottomSheetState>,
    val partialRatio: Float,
    val draggable: Boolean,
) {
    lateinit var column: Element
    lateinit var above: NativeElement
    lateinit var content: NativeElement
    private var closed = false

    // How much of the frame's height the content covers, from 0 to 1.
    var fraction: Float = 1f
        set(value) {
            field = value
            above.shown = value < 1f
            above.weight = 1f - value
            content.weight = value
        }

    fun fractionOf(state: BottomSheetState): Float = when (state) {
        BottomSheetState.EXPANDED -> 1f
        BottomSheetState.PARTIALLY_EXPANDED -> partialRatio
        BottomSheetState.COLLAPSED -> TODO("Compose backend: BottomSheetState.COLLAPSED (peekSize)")
    }

    fun close() {
        if (closed) return
        closed = true
        frame.removeChild(column)
    }

    fun dragBy(dp: Float) {
        val height = checkNotNull(frame.screenRectangle()) { "Dragging a bottom sheet that isn't laid out" }.height
        val next = (fraction - dp / height.toFloat()).coerceAtMost(1f)
        // Dragged off the bottom of the frame.
        if (next <= 0f) close() else fraction = next
    }

    fun settle() {
        if (closed) return
        val target = when {
            fraction < partialRatio / 2 -> return close()
            fraction < (1f + partialRatio) / 2 -> BottomSheetState.PARTIALLY_EXPANDED
            else -> BottomSheetState.EXPANDED
        }
        state.value = target
        // Setting the state again doesn't notify, so put the sheet back explicitly.
        fraction = fractionOf(target)
    }
}

public actual class CoordinatorDragHandle actual constructor(context: ElementContext) : NativeInteractiveContainerElement(context) {
    actual override val underlyingNativeElement: CoordinatorDragHandle get() = this

    private val iconView: IconView = icon { source = Icon.expand }
    private var sheet: BottomSheet? = null
    private val dragging = DraggableState { sheet!!.dragBy(it / density) }

    @OverrideOnly
    override fun onStartup() {
        super.onStartup()
        val sheet = context.bottomSheet ?: return
        this.sheet = sheet
        iconView.reactive {
            iconView.source = if (sheet.state() == BottomSheetState.EXPANDED) Icon.collapse else Icon.expand
        }
    }

    override fun inputModifier(): Modifier {
        val sheet = sheet ?: return Modifier
        var m = Modifier.clickable(interactions.source, indication = null, enabled = enabled) {
            sheet.state.value = when (sheet.state.value) {
                BottomSheetState.EXPANDED -> BottomSheetState.PARTIALLY_EXPANDED
                BottomSheetState.PARTIALLY_EXPANDED -> BottomSheetState.EXPANDED
                BottomSheetState.COLLAPSED -> BottomSheetState.PARTIALLY_EXPANDED
            }
        }
        if (sheet.draggable) m = m.draggable(dragging, Orientation.Vertical, enabled, interactions.source, onDragStopped = { sheet.settle() })
        return m
    }

    @Composable
    override fun Content(modifier: Modifier) {
        Layout(content = { ShownChildren() }, modifier = modifier, measurePolicy = FrameMeasurePolicy)
    }
}
