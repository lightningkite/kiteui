package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.ExternalServices
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
import platform.UIKit.UISheetPresentationControllerDetentIdentifier
import platform.UIKit.UISheetPresentationController
import platform.UIKit.UIView
import platform.UIKit.UIViewController
import com.lightningkite.kiteui.objc.presentationController
import com.lightningkite.kiteui.views.popoverWriter
import com.lightningkite.readable.BasicListenable
import kotlinx.cinterop.useContents
import kotlinx.coroutines.delay
import platform.UIKit.UIModalPresentationPageSheet
import platform.UIKit.UISheetPresentationControllerDetent

private var ViewWriter.bottomSheetState: Writable<BottomSheetState>? by rContextAddon<Writable<BottomSheetState>?>(null)

actual class CoordinatorFrame actual constructor(context: RContext) : RView(context) {
    override val cannotBeCovered: Boolean get() = false
    override val native = FrameLayout()
    override fun childTouches(side: Side, child: RView): Boolean {
        return when(side) {
            Side.Left -> child.native.extensionHorizontalAlign?.touchesStart != false
            Side.Top -> child.native.extensionVerticalAlign?.touchesStart != false
            Side.Right -> child.native.extensionHorizontalAlign?.touchesEnd != false
            Side.Bottom -> child.native.extensionVerticalAlign?.touchesEnd != false
        }
    }

    actual fun bottomSheet(
        peekSize: Dimension?,
        partialRatio: Float,
        draggable: Boolean,
        startState: BottomSheetState,
        shouldRemoveExpandedCorners: Boolean,
        blockBehind: Boolean,
        content: ViewWriter.(control: BottomSheetControl) -> ViewModifiable
    ) {
        val viewController = object : UIViewController(null, null) {
            override fun viewDidDisappear(animated: Boolean) {
                super.viewDidDisappear(animated)
            }
        }
        viewController.modalPresentationStyle = UIModalPresentationPageSheet
        val control = object : BottomSheetControl {
            override val state: Writable<BottomSheetState> = Property(startState)
            override fun close() {
                viewController.dismissViewControllerAnimated(true) {}
            }
        }
        viewController.kiteUi(context.split()) {
            closeSiblingPopovers()
            val childCloser = BasicListenable()
            var closeCurrent = {}
            var stopListeningToCloser = {}
            fun internalClose() {
                stopListeningToCloser()
                closeCurrent()
                control.close()
            }
            stopListeningToCloser = popoverClosers.addListener {
                childCloser.invokeAll()
                internalClose()
            }
            rootPopoverCloser = childCloser
            popoverClosers = childCloser

            beforeNextElementSetup {
                parent = this@CoordinatorFrame
                launch {
                    while(true) {
                        delay(100)
                        refreshTheming()
                    }
                }
            }
            frame {
                overlayFrame = this
                content(control)
            }
        }
        val windowHeight = context.controller.view.frame.useContents { size.height }
        val relativeToWindow = native.convertRect(native.bounds, toView = context.controller.view)
        val bottomInset = context.controller.view.safeAreaInsets.useContents { bottom }
        val fullSize = windowHeight - relativeToWindow.useContents { origin.y } - bottomInset
//        println("windowHeight: $windowHeight")
//        println("relativeToWindow: ${relativeToWindow.useContents { origin.y }}")
//        println("bottomInset: $bottomInset")
//        println("fullSize: $fullSize")
        val wholeDetent = UISheetPresentationControllerDetent.Companion.customDetentWithIdentifier(null) {
            fullSize
        }
        val partialDetent = UISheetPresentationControllerDetent.Companion.customDetentWithIdentifier(null) {
            fullSize * partialRatio
        }
        (viewController.presentationController as? UISheetPresentationController)?.apply {
            if(partialRatio < 0.99) {
                detents = listOf(
                    partialDetent,
                    wholeDetent,
                )

                selectedDetentIdentifier = when (startState) {
                    BottomSheetState.EXPANDED -> wholeDetent.identifier
                    else -> partialDetent.identifier
                }
                largestUndimmedDetentIdentifier = wholeDetent.identifier
//                largestUndimmedDetentIdentifier = partialDetent.identifier
            } else {
                detents = listOf(
                    wholeDetent
                )
                largestUndimmedDetentIdentifier = wholeDetent.identifier
                selectedDetentIdentifier = wholeDetent.identifier
            }
            prefersGrabberVisible = draggable
        }
        ExternalServices.currentPresenter(viewController)
        ExternalServices.currentlyPresented = viewController
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
                willRemove = null
            }
        }
        val popoverWriter = popoverWriter { closePanel() }
        with(popoverWriter) {
            withoutAnimation {
                val control = object : SlidingPanelControl {
                    override fun close() {
                        closePopovers()
                    }
                }
                willRemove = if (ratio == null) {
                    align(Align.Start, Align.Stretch) - content(control)
                } else {
                    row {
                        gap = 0.px
                        ignoreInteraction = true
                        weight(ratio) - content(control)
                        weight(1f - ratio) - frame { ignoreInteraction = true }
                    }
                }.rView
            }
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
                willRemove = null
            }
        }
        val popoverWriter = popoverWriter { closePanel() }
        with(popoverWriter) {
            withoutAnimation {
                val control = object : SlidingPanelControl {
                    override fun close() {
                        closePopovers()
                    }
                }
                willRemove = if (ratio == null) {
                    align(Align.End, Align.Stretch) - content(control)
                } else {
                    row {
                        gap = 0.px
                        ignoreInteraction = true
                        weight(1f - ratio) - frame { ignoreInteraction = true }
                        weight(ratio) - content(control)
                    }
                }.rView
            }
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

