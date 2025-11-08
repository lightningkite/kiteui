package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.ExternalServices
import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.ClickableSemantic
import com.lightningkite.kiteui.models.Dimension
import com.lightningkite.kiteui.models.DownSemantic
import com.lightningkite.kiteui.models.FocusSemantic
import com.lightningkite.kiteui.models.ScreenTransition
import com.lightningkite.kiteui.models.ScreenTransitions
import com.lightningkite.kiteui.models.ThemeAndBack
import com.lightningkite.kiteui.models.px
import com.lightningkite.kiteui.objc.presentationController
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.l2.overlayFrame
import com.lightningkite.kiteui.views.popoverWriter
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*
import kotlinx.cinterop.ObjCAction
import kotlinx.cinterop.useContents
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import platform.UIKit.*
import platform.UIKit.UISheetPresentationController
import platform.UIKit.UIViewController
import platform.darwin.NSObject
import platform.objc.sel_registerName

private var ViewWriter.bottomSheetState: MutableReactive<BottomSheetState>? by rContextAddon<MutableReactive<BottomSheetState>?>(null)

actual class CoordinatorFrame actual constructor(context: RContext) : RView(context) {
    
    override val native = FrameLayout()

    // The system only keeps weak references to the following objects, so we must keep our own references for the
    // lifetime of the view
    private var leftSwipeTarget: NSObject? = null
    private var leftSwipeRecognizer: UISwipeGestureRecognizer? = null
    private var rightSwipeTarget: NSObject? = null
    private var rightSwipeRecognizer: UISwipeGestureRecognizer? = null

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
            override val state: MutableReactive<BottomSheetState> = Signal(startState)
            override fun close() {
                viewController.dismissViewControllerAnimated(true) {}
            }
        }
        popoverCloser?.invoke()
        popoverCloser = { control.close() }
        viewController.kiteUi(context.split(viewController)) {
            popoverCloser = null

            beforeNextElementSetup {
                parent = this@CoordinatorFrame
                launch {
                    while(true) {
                        delay(100)
                        refreshTheming()
                    }
                }
            }.frame {
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
        viewController.definesPresentationContext = true
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
        context.present(viewController)
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
        val popoverWriter = popoverWriter(popoverRoot = true) { closePanel() }
        with(popoverWriter) {
            withoutAnimation {
                val control = object : SlidingPanelControl {
                    override fun close() {
                        this@CoordinatorFrame.closePopovers()
                    }
                }
                willRemove = produceOne {
                    frame {
                        overlayFrame = this
                        if (ratio == null) {
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
        val popoverWriter = popoverWriter(popoverRoot = true) { closePanel() }
        with(popoverWriter) {
            withoutAnimation {
                val control = object : SlidingPanelControl {
                    override fun close() {
                        this@CoordinatorFrame.closePopovers()
                    }
                }
                willRemove = produceOne {
                    frame {
                        overlayFrame = this
                        if (ratio == null) {
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
        }
        willRemove?.animateIn(transition.forward)
    }

    actual fun onLeftSwipe(action: suspend () -> Unit) {
        leftSwipeTarget = object : NSObject() {
            @ObjCAction
            fun handleLeftSwipe(sender: UISwipeGestureRecognizer) {
                launch { action() }
            }
        }
        leftSwipeRecognizer?.let(native::removeGestureRecognizer)
        leftSwipeRecognizer = UISwipeGestureRecognizer(leftSwipeTarget, sel_registerName("handleLeftSwipe:")).apply {
            direction = UISwipeGestureRecognizerDirectionLeft
        }.also(native::addGestureRecognizer)
        native.userInteractionEnabled = true
    }

    actual fun onRightSwipe(action: suspend () -> Unit) {
        rightSwipeTarget = object : NSObject() {
            @ObjCAction
            fun handleRightSwipe(sender: UISwipeGestureRecognizer) {
                launch { action() }
            }
        }
        rightSwipeRecognizer?.let(native::removeGestureRecognizer)
        rightSwipeRecognizer = UISwipeGestureRecognizer(rightSwipeTarget, sel_registerName("handleRightSwipe:")).apply {
            direction = UISwipeGestureRecognizerDirectionRight
        }.also(native::addGestureRecognizer)
        native.userInteractionEnabled = true
    }
}


actual class CoordinatorDragHandle actual constructor(context: RContext) : RView(context) {
    override val native = FrameLayoutButton()

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
    }

    override fun applyState(theme: ThemeAndBack): ThemeAndBack {
        var t = theme[ClickableSemantic]
        if (native.highlighted) t = t[DownSemantic]
        if (native.focused) t = t[FocusSemantic]
        return super.applyState(t)
    }
}

