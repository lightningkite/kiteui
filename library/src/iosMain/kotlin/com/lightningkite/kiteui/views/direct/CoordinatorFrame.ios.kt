package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.OverrideOnly
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.beforeSetup
import platform.UIKit.UIControl
import com.lightningkite.kiteui.views.l2.overlayFrame
import com.lightningkite.reactive.context.invoke
import com.lightningkite.reactive.context.onRemove
import com.lightningkite.reactive.core.MutableReactive
import com.lightningkite.reactive.core.Signal
import kotlinx.cinterop.ObjCAction
import kotlinx.cinterop.useContents
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import platform.UIKit.*
import platform.darwin.NSObject
import platform.objc.sel_registerName

private var ElementContext.bottomSheetState: MutableReactive<BottomSheetState>? by contextAddon(null)

public actual class CoordinatorFrame actual constructor(context: ElementContext) : NativeContainerElement(context) {
    override val native = FrameLayout()

    // The system only keeps weak references to the following objects, so we must keep our own references for the
    // lifetime of the view
    private var leftSwipeTarget: NSObject? = null
    private var leftSwipeRecognizer: UISwipeGestureRecognizer? = null
    private var rightSwipeTarget: NSObject? = null
    private var rightSwipeRecognizer: UISwipeGestureRecognizer? = null

    public actual fun bottomSheet(
        peekSize: Dimension?,
        partialRatio: Float,
        draggable: Boolean,
        startState: BottomSheetState,
        shouldRemoveExpandedCorners: Boolean,
        blockBehind: Boolean,
        content: ElementWriter.CanAddShownWhen.(control: BottomSheetControl) -> Unit,
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
        viewController.kiteUi(context.split(viewController)) {
            beforeSetup {
                underlyingNativeElement.parent = this@CoordinatorFrame
                launch {    // TODO: What the fuck
                    while(true) {
                        delay(100)
                        underlyingNativeElement.refreshTheming()
                    }
                }
            }.split().frame {
                context.overlayFrame = this
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
            if (partialRatio < 0.99) {
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

    public actual fun leftSlidingPanel(
        ratio: Float?,
        blockBehind: Boolean,
        content: ElementWriter.CanAddShownWhen.(control: SlidingPanelControl) -> Unit,
    ) {
        var willRemove: Element? = null
        val transition = ScreenTransitions(ScreenTransition.Pop, ScreenTransition.Push, ScreenTransition.Fade)
        fun closePanel() {
            willRemove?.let {
                it.animateOut(transition.reverse) {
                    this@CoordinatorFrame.removeChild(it)
                }
                willRemove = null
            }
        }
        withoutAnimation {
            val control = object : SlidingPanelControl {
                override fun close() {
                    closePanel()
                }
            }
            willRemove = split().produceExactlyOneElement {
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
        willRemove?.animateIn(transition.forward)
    }

    public actual fun rightSlidingPanel(
        ratio: Float?,
        blockBehind: Boolean,
        content: ElementWriter.CanAddShownWhen.(control: SlidingPanelControl) -> Unit,
    ) {
        var willRemove: Element? = null
        val transition = ScreenTransitions.HorizontalSlide
        fun closePanel() {
            willRemove?.let {
                it.animateOut(transition.reverse) {
                    this@CoordinatorFrame.removeChild(it)
                }
                willRemove = null
            }
        }

        withoutAnimation {
            val control = object : SlidingPanelControl {
                override fun close() {
                    closePanel()
                }
            }
            willRemove = split().produceExactlyOneElement {
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
        willRemove?.animateIn(transition.forward)
    }

    public actual fun onLeftSwipe(action: suspend () -> Unit) {
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

    public actual fun onRightSwipe(action: suspend () -> Unit) {
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


public actual class CoordinatorDragHandle actual constructor(context: ElementContext) : NativeInteractiveContainerElement(context) {
    actual override val underlyingNativeElement: CoordinatorDragHandle get() = this
    override val native = FrameLayoutButton()
    override val control: UIControl get() = native
    init { setupControl() }

    @OverrideOnly
    override fun onStartup() {
        super.onStartup()
        val e = context.bottomSheetState ?: return
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
}

