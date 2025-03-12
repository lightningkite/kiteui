package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.Dimension
import com.lightningkite.kiteui.models.ScreenTransition
import com.lightningkite.kiteui.models.ScreenTransitions
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.animateIn
import com.lightningkite.kiteui.views.animateOut
import com.lightningkite.kiteui.views.l2.overlayFrame
import com.lightningkite.kiteui.views.withoutAnimation
import com.lightningkite.readable.Property
import com.lightningkite.readable.Writable
import kotlin.let
import kotlin.run

actual class CoordinatorFrame actual constructor(context: RContext) : RView(context) {
    override val cannotBeCovered: Boolean get() = false
    override val native = FrameLayout()

    actual fun bottomSheet(
        peekSize: Dimension?,
        partialRatio: Float,
        draggable: Boolean,
        startState: BottomSheetState,
        shouldRemoveExpandedCorners: Boolean,
        blockBehind: Boolean,
        content: ViewWriter.(control: BottomSheetControl) -> ViewModifiable
    ) {
        var willRemove: RView? = null
        val transition = ScreenTransitions.VerticalSlide
        withoutAnimation {
            popoverWriter {
                willRemove?.let {
                    it.animateOut(transition.reverse) {
                        overlayFrame!!.removeChild(it)
                    }
                }
            }.run {
                beforeNextElementSetup {
                    animateIn(transition.forward)
                }
                content(object: BottomSheetControl {
                    override val state: Writable<BottomSheetState> = Property(BottomSheetState.EXPANDED)
                    override fun close() { closePopovers() }
                })
                willRemove = lastWrittenView
            }
        }
    }

    actual fun leftSlidingPanel(
        ratio: Float?,
        blockBehind: Boolean,
        content: ViewWriter.(control: SlidingPanelControl) -> ViewModifiable
    ) {
        var willRemove: RView? = null
        val transition = ScreenTransitions(ScreenTransition.Pop, ScreenTransition.Push, ScreenTransition.Fade)
        withoutAnimation {
            popoverWriter {
                willRemove?.let {
                    it.animateOut(transition.reverse) {
                        overlayFrame!!.removeChild(it)
                    }
                }
            }.run {
                beforeNextElementSetup {
                    animateIn(transition.forward)
                }
                align(Align.Start, Align.Stretch) - content(object: SlidingPanelControl {
                    override fun close() { closePopovers() }
                })
                willRemove = lastWrittenView
            }
        }
    }

    actual fun rightSlidingPanel(
        ratio: Float?,
        blockBehind: Boolean,
        content: ViewWriter.(control: SlidingPanelControl) -> ViewModifiable
    ) {
        var willRemove: RView? = null
        val transition = ScreenTransitions.HorizontalSlide
        withoutAnimation {
            popoverWriter {
                willRemove?.let {
                    it.animateOut(transition.reverse) {
                        overlayFrame!!.removeChild(it)
                    }
                }
            }.run {
                beforeNextElementSetup {
                    animateIn(transition.forward)
                }
                align(Align.End, Align.Stretch) - content(object: SlidingPanelControl {
                    override fun close() { closePopovers() }
                })
                willRemove = lastWrittenView
            }
        }
    }
}


actual class CoordinatorDragHandle actual constructor(context: RContext) : RView(context) {
    override val native = FrameLayout()
}

