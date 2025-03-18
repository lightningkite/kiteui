package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.ClickableSemantic
import com.lightningkite.kiteui.models.Dimension
import com.lightningkite.kiteui.models.DisabledSemantic
import com.lightningkite.kiteui.models.DownSemantic
import com.lightningkite.kiteui.models.FocusSemantic
import com.lightningkite.kiteui.models.Icon
import com.lightningkite.kiteui.models.ScreenTransition
import com.lightningkite.kiteui.models.ScreenTransitions
import com.lightningkite.kiteui.models.ThemeAndBack
import com.lightningkite.kiteui.models.px
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.l2.icon
import com.lightningkite.kiteui.views.l2.overlayFrame
import com.lightningkite.readable.Property
import com.lightningkite.readable.Writable
import com.lightningkite.readable.invoke
import com.lightningkite.readable.onRemove
import com.lightningkite.readable.reactive
import kotlinx.coroutines.launch
import platform.UIKit.UIView

private var ViewWriter.bottomSheetState: Writable<BottomSheetState>? by rContextAddon<Writable<BottomSheetState>?>(null)

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
//        val transition = ScreenTransitions.VerticalSlide
        val control = object : BottomSheetControl {
            override val state: Writable<BottomSheetState> = Property(startState)
            override fun close() {
                willRemove?.let {
//                    it.animateOut(transition.reverse) {
                        this@CoordinatorFrame.removeChild(it)
//                    }
                }
                willRemove = null
            }
        }
        withoutAnimation {
            bottomSheetState = control.state
            willRemove = col {
                spacing = 0.px
                ignoreInteraction = true
                expanding - onlyWhen { control.state() == BottomSheetState.PARTIALLY_EXPANDED } - frame {
                    ignoreInteraction = true
                }
                expanding - content(control)
            }
        }
//        willRemove?.animateIn(transition.forward)
    }

    actual fun leftSlidingPanel(
        ratio: Float?,
        blockBehind: Boolean,
        content: ViewWriter.(control: SlidingPanelControl) -> ViewModifiable
    ) {
        var willRemove: RView? = null
        val transition = ScreenTransitions(ScreenTransition.Pop, ScreenTransition.Push, ScreenTransition.Fade)
        fun closePanel() {
            willRemove?.let {
                it.animateOut(transition.reverse) {
                    this@CoordinatorFrame.removeChild(it)
                }
            }
        }
        withoutAnimation {
            val control = object : SlidingPanelControl {
                override fun close() {
                    closePanel()
                }
            }
            willRemove = if (ratio == null) {
                align(Align.Start, Align.Stretch) - content(control)
            } else {
                row {
                    spacing = 0.px
                    ignoreInteraction = true
                    weight(ratio) - content(control)
                    weight(1f - ratio) - frame { ignoreInteraction = true }
                }
            }.rView
        }
        willRemove?.animateIn(transition.forward)
    }

    actual fun rightSlidingPanel(
        ratio: Float?,
        blockBehind: Boolean,
        content: ViewWriter.(control: SlidingPanelControl) -> ViewModifiable
    ) {
        var willRemove: RView? = null
        val transition = ScreenTransitions.HorizontalSlide
        fun closePanel() {
            willRemove?.let {
                it.animateOut(transition.reverse) {
                    this@CoordinatorFrame.removeChild(it)
                }
            }
        }
        withoutAnimation {
            val control = object : SlidingPanelControl {
                override fun close() {
                    closePanel()
                }
            }
            willRemove = if (ratio == null) {
                align(Align.End, Align.Stretch) - content(control)
            } else {
                row {
                    spacing = 0.px
                    ignoreInteraction = true
                    weight(1f - ratio) - frame { ignoreInteraction = true }
                    weight(ratio) - content(control)
                }
            }.rView

        }
        willRemove?.animateIn(transition.forward)
    }
}


actual class CoordinatorDragHandle actual constructor(context: RContext) : RView(context) {
    override val native = FrameLayoutButton()

    val iconView = icon {
        source = Icon.expand
    }

    override fun postSetup() {
        super.postSetup()
        val e = bottomSheetState ?: return
        onRemove(native.setOnClick {
            launch {
                e set when (e()) {
                    BottomSheetState.EXPANDED -> BottomSheetState.PARTIALLY_EXPANDED
                    BottomSheetState.PARTIALLY_EXPANDED -> BottomSheetState.EXPANDED
                    else -> BottomSheetState.PARTIALLY_EXPANDED
                }
            }
        })
        iconView.reactive {
            iconView.source = if (e() == BottomSheetState.EXPANDED) Icon.collapse else Icon.expand
        }
    }

    override fun applyState(theme: ThemeAndBack): ThemeAndBack {
        var t = theme[ClickableSemantic]
        if (native.highlighted) t = t[DownSemantic]
        if (native.focused) t = t[FocusSemantic]
        return super.applyState(t)
    }
}

