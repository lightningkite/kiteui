package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.ClickableSemantic
import com.lightningkite.kiteui.models.Dimension
import com.lightningkite.kiteui.models.Icon
import com.lightningkite.kiteui.models.ScreenTransition
import com.lightningkite.kiteui.models.ScreenTransitions
import com.lightningkite.kiteui.models.px
import com.lightningkite.kiteui.views.*
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import kotlinx.coroutines.launch

private var ViewWriter.bottomSheetState: MutableReactive<BottomSheetState>? by rContextAddon<MutableReactive<BottomSheetState>?>(null)

actual class CoordinatorFrame actual constructor(context: ElementContext) : RView(context) {

    init {
        native.tag = "div"
        native.style.lineHeight = "0px !important"
    }

    override fun internalAddChild(index: Int, view: RView) {
        super.internalAddChild(index, view)
        Frame.internalAddChildStack(this, index, view)
    }

    actual fun bottomSheet(
        peekSize: Dimension?,
        partialRatio: Float,
        draggable: Boolean,
        startState: BottomSheetState,
        shouldRemoveExpandedCorners: Boolean,
        blockBehind: Boolean,
        content: ViewWriter.(control: BottomSheetControl) -> Unit
    ) {

        val expanded = Signal(startState)
        var willRemove: RView? = null
        val transition = ScreenTransitions.VerticalSlide
        fun closePanel() {
            willRemove?.let {
            it.animateOut(transition.reverse) {
                this@CoordinatorFrame.removeChild(it)
            }
        }}
        withoutAnimation {
            bottomSheetState = expanded
            willRemove = beforeNextElementSetup {
                animateIn(transition.forward)
            }.col {
                gap = 0.px
                ignoreInteraction = true
                expanding.shownWhen { expanded() == BottomSheetState.PARTIALLY_EXPANDED }.frame {
                    ignoreInteraction = true
                }
                expanding.content(object : BottomSheetControl {
                    override val state: MutableReactive<BottomSheetState> = expanded
                    override fun close() {
                        closePanel()
                    }
                })
            }
        }
    }

    actual fun leftSlidingPanel(
        ratio: Float?,
        blockBehind: Boolean,
        content: ViewWriter.(control: SlidingPanelControl) -> Unit
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
            willRemove = produceOne {
                beforeNextElementSetup {
                    animateIn(transition.forward)
                }
                if(ratio == null) {
                    align(Align.Start, Align.Stretch).content(control)
                } else {
                    row {
                        gap = 0.px
                        ignoreInteraction = true
                        weight(ratio).content(control)
                        weight(1f - ratio).frame { ignoreInteraction = true }
                    }
                }
            }
        }
    }

    actual fun rightSlidingPanel(
        ratio: Float?,
        blockBehind: Boolean,
        content: ViewWriter.(control: SlidingPanelControl) -> Unit
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
            willRemove = produceOne {
                beforeNextElementSetup {
                    animateIn(transition.forward)
                }
                if(ratio == null) {
                    align(Align.End, Align.Stretch).content(control)
                } else {
                    row {
                        gap = 0.px
                        ignoreInteraction = true
                        weight(1f - ratio).frame { ignoreInteraction = true }
                        weight(ratio).content(control)
                    }
                }
            }
        }
    }

    actual fun onLeftSwipe(action: suspend () -> Unit) {

    }

    actual fun onRightSwipe(action: suspend () -> Unit) {

    }
}


actual class CoordinatorDragHandle actual constructor(context: ElementContext) : RView(context) {
    init {
        themeChoice += ClickableSemantic
        native.tag = "button"
        native.classes.add("kiteui-stack")
        native.classes.add("clickable")
    }

    override fun internalAddChild(index: Int, view: RView) {
        super.internalAddChild(index, view)
        Frame.internalAddChildStack(this, index, view)
    }

    val iconView = icon {
        source = Icon.expand
    }

    override fun postSetup() {
        super.postSetup()
        val e = bottomSheetState ?: return
        native.addEventListener("click") {
            launch {
                e set when(e()) {
                    BottomSheetState.EXPANDED -> BottomSheetState.PARTIALLY_EXPANDED
                    BottomSheetState.PARTIALLY_EXPANDED -> BottomSheetState.EXPANDED
                    else -> BottomSheetState.PARTIALLY_EXPANDED
                }
            }
        }
        iconView.reactive {
            iconView.source = if(e() == BottomSheetState.EXPANDED) Icon.collapse else Icon.expand
        }
    }
}

