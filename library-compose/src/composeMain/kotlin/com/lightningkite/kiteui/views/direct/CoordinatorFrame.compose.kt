package com.lightningkite.kiteui.views.direct

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.DraggableState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import com.lightningkite.kiteui.OverrideOnly
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.reactive.context.invoke
import com.lightningkite.reactive.context.reactive
import com.lightningkite.reactive.core.Signal
import kotlin.math.abs

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
        val sheet = BottomSheet(this, Signal(startState), peekSize, partialRatio, draggable)
        val control = object : BottomSheetControl {
            override val state = sheet.state
            override fun close() = sheet.close()
        }
        sheet.column = beforeSetup { animateIn(ScreenTransitions.VerticalSlide.forward) }.col {
            gap = 0.px
            ignoreInteraction = true
            context.bottomSheet = sheet
            sheet.above = frame { ignoreInteraction = true }
            beforeSetup { sheet.content = underlyingNativeElement }.content(control)
            var lastState = startState
            reactive {
                val state = sheet.state()
                val fraction = sheet.fractionOf(state, this@CoordinatorFrame.height())
                // Only state changes animate, not the frame's size changing.
                if (state == lastState) withoutAnimation { sheet.fraction = fraction } else sheet.fraction = fraction
                lastState = state
            }
        }
    }

    // In dp; 0 until laid out.
    internal val height = Signal(0f)

    public actual fun leftSlidingPanel(ratio: Float?, blockBehind: Boolean, content: ElementWriter.CanAddShownWhen.(control: SlidingPanelControl) -> Unit) {
        lateinit var panel: Element
        val transition = ScreenTransitions(ScreenTransition.Pop, ScreenTransition.Push, ScreenTransition.Fade)
        val control = object : SlidingPanelControl {
            override fun close() { if (panel in children) panel.animateOut(transition.reverse) { removeChild(panel) } }
        }
        val writer = beforeSetup { animateIn(transition.forward) }
        panel = if (ratio == null) {
            checkNotNull(writer.produceAtMostOneView { align(Align.Start, Align.Stretch).content(control) })
        } else writer.row {
            gap = 0.px
            ignoreInteraction = true
            weight(ratio).content(control)
            weight(1f - ratio).frame { ignoreInteraction = true }
        }
    }

    public actual fun rightSlidingPanel(ratio: Float?, blockBehind: Boolean, content: ElementWriter.CanAddShownWhen.(control: SlidingPanelControl) -> Unit) {
        lateinit var panel: Element
        val transition = ScreenTransitions.HorizontalSlide
        val control = object : SlidingPanelControl {
            override fun close() { if (panel in children) panel.animateOut(transition.reverse) { removeChild(panel) } }
        }
        val writer = beforeSetup { animateIn(transition.forward) }
        panel = if (ratio == null) {
            checkNotNull(writer.produceAtMostOneView { align(Align.End, Align.Stretch).content(control) })
        } else writer.row {
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
        Layout(
            content = { ShownChildren() },
            modifier = modifier.onSizeChanged { height.value = it.height / density },
            measurePolicy = FrameMeasurePolicy,
        )
    }
}

private var ElementContext.bottomSheet: BottomSheet? by contextAddon(null)

private class BottomSheet(
    val frame: CoordinatorFrame,
    val state: Signal<BottomSheetState>,
    val peekSize: Dimension?,
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
            above.changeWeight(1f - value)
            content.changeWeight(value)
        }

    // Collapsed shows peekSize of the content, as on Android.  (The web shows a collapsed sheet expanded.)
    fun fractionOf(state: BottomSheetState, frameHeight: Float): Float = when (state) {
        BottomSheetState.EXPANDED -> 1f
        BottomSheetState.PARTIALLY_EXPANDED -> partialRatio
        BottomSheetState.COLLAPSED -> {
            val peek = checkNotNull(peekSize) { "A bottom sheet needs a peekSize to be COLLAPSED" }
            // Before the first layout the height is 0, so the sheet starts full until it's known.
            minOf(1f, peek.value / frameHeight)
        }
    }

    fun close() {
        if (closed) return
        closed = true
        column.animateOut(ScreenTransitions.VerticalSlide.reverse) { frame.removeChild(column) }
    }

    fun dragBy(dp: Float) {
        val height = checkNotNull(frame.screenRectangle()) { "Dragging a bottom sheet that isn't laid out" }.height
        val next = (fraction - dp / height.toFloat()).coerceAtMost(1f)
        // Dragged off the bottom of the frame.
        if (next <= 0f) close() else column.withoutAnimation { fraction = next }
    }

    // Settles at the nearest state, or closes if the bottom of the frame is nearer still.
    fun settle() {
        if (closed) return
        val frameHeight = frame.height.value
        val states = if (peekSize == null) BottomSheetState.entries - BottomSheetState.COLLAPSED else BottomSheetState.entries
        val target = states.minBy { abs(fractionOf(it, frameHeight) - fraction) }
        if (fraction < abs(fractionOf(target, frameHeight) - fraction)) return close()
        state.value = target
        // Setting the state again doesn't notify, so put the sheet back explicitly.
        fraction = fractionOf(target, frameHeight)
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

    override fun inputModifier(takesInput: Boolean): Modifier {
        val sheet = sheet ?: return Modifier
        var m = Modifier.clickable(interactions.source, indication = null, enabled = enabled && takesInput) {
            sheet.state.value = when (sheet.state.value) {
                BottomSheetState.EXPANDED -> BottomSheetState.PARTIALLY_EXPANDED
                BottomSheetState.PARTIALLY_EXPANDED -> BottomSheetState.EXPANDED
                BottomSheetState.COLLAPSED -> BottomSheetState.PARTIALLY_EXPANDED
            }
        }
        if (sheet.draggable) m = m.draggable(dragging, Orientation.Vertical, enabled && takesInput, interactions.source, onDragStopped = { sheet.settle() })
        return m
    }

    @Composable
    override fun Content(modifier: Modifier) {
        Layout(content = { ShownChildren() }, modifier = modifier, measurePolicy = FrameMeasurePolicy)
    }
}
