package com.lightningkite.kiteui.views.direct


import com.lightningkite.kiteui.ExperimentalKiteUi
import com.lightningkite.kiteui.models.ScreenTransition
import com.lightningkite.kiteui.views.ContainerElement
import com.lightningkite.kiteui.views.Element
import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.ElementWithChildren
import com.lightningkite.kiteui.views.NativeContainerElement
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.animateIn
import com.lightningkite.kiteui.views.animateOut
import com.lightningkite.kiteui.views.extensionIgnoreInteraction
import com.lightningkite.kiteui.views.informParentOfSizeChange
import com.lightningkite.kiteui.views.produceExactlyOneElement
import com.lightningkite.kiteui.views.produceAtMostOneView
import com.lightningkite.kiteui.views.withoutAnimation


@OptIn(ExperimentalKiteUi::class)
public actual class SwapView actual constructor(context: ElementContext): NativeContainerElement(context) {
    actual override val underlyingNativeElement: SwapView get() = this
    override val native: FrameLayout = FrameLayout()
    private var currentView: Element? = null

    init {
        native.clipsToBounds = true
    }

    public actual fun swap(transition: ScreenTransition, createNewView: ViewWriter.() -> Unit): Unit {
        native.hidden = false
        currentView?.let { oldView ->
            oldView.animateOut(transition) {
                removeChild(oldView)
                native.hidden = native.subviews.isEmpty()
                native.informParentOfSizeChange()
            }
        }
        currentView = null

        var newView: Element? = null
        withoutAnimation {
            newView = produceAtMostOneView { createNewView() }
            println("Swapping to $newView")
            currentView = newView
        }
        newView?.let {
            it.animateIn(transition) {}
        }
        native.extensionIgnoreInteraction = newView == null
    }
}