package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.ClickableSemantic
import com.lightningkite.kiteui.models.Dimension
import com.lightningkite.kiteui.models.Icon
import com.lightningkite.kiteui.models.ScreenTransition
import com.lightningkite.kiteui.models.ScreenTransitions
import com.lightningkite.kiteui.models.px
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.l2.icon
import com.lightningkite.kiteui.views.l2.overlayFrame
import com.lightningkite.readable.Property
import com.lightningkite.readable.Writable
import com.lightningkite.readable.reactive

actual class CoordinatorFrame actual constructor(context: RContext) : RView(context) {
    override val cannotBeCovered: Boolean get() = false

    init {
        native.tag = "div"
        native.style.lineHeight = "0px !important"
    }

    override fun internalAddChild(index: Int, view: RView) {
        super.internalAddChild(index, view)
        Frame.internalAddChildStack(this, index, view)
    }

    val expanded = Property(false)

    actual fun bottomSheet(
        peekSize: Dimension?,
        partialRatio: Float,
        draggable: Boolean,
        startState: BottomSheetState,
        shouldRemoveExpandedCorners: Boolean,
        blockBehind: Boolean,
        content: ViewWriter.(control: BottomSheetControl) -> ViewModifiable
    ) {
        expanded.value = startState == BottomSheetState.EXPANDED
        var willRemove: RView? = null
        val transition = ScreenTransitions.VerticalSlide
        withoutAnimation {
            popoverWriter {
                willRemove?.let {
                    it.animateOut(transition.reverse) {
                        this@CoordinatorFrame.removeChild(it)
                    }
                }
            }.run {
                beforeNextElementSetup {
                    animateIn(transition.forward)
                }
                willRemove = col {
                    spacing = 0.px
                    ignoreInteraction = true
                    expanding - onlyWhen { !expanded() } - frame {
                        ignoreInteraction = true
                    }
                    expanding - content(object : BottomSheetControl {
                        override val state: Writable<BottomSheetState> = Property(BottomSheetState.EXPANDED)
                        override fun close() {
                            closePopovers()
                        }
                    })
                }
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
                        this@CoordinatorFrame.removeChild(it)
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
                        this@CoordinatorFrame.removeChild(it)
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

    val expanded get() = generateSequence(this as RView) { it.parent }
        .filterIsInstance<CoordinatorFrame>()
        .firstOrNull()
        ?.expanded ?: Property(false)

    val iconView = icon {
        source = Icon.expand
    }

    override fun postSetup() {
        super.postSetup()
        val e = expanded
        native.addEventListener("click") {
            e.value = !e.value
        }
        iconView.reactive {
            iconView.source = if(e()) Icon.collapse else Icon.expand
        }
    }
}

