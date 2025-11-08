package com.lightningkite.kiteui.views.direct


import com.lightningkite.kiteui.models.ScreenTransition
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView
import com.lightningkite.kiteui.views.ViewModifiable
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.animateIn
import com.lightningkite.kiteui.views.animateOut
import com.lightningkite.kiteui.views.extensionIgnoreInteraction
import com.lightningkite.kiteui.views.informParentOfSizeChange
import com.lightningkite.kiteui.views.withoutAnimation


actual class SwapView actual constructor(context: RContext): RView(context) {
    
    override val native = FrameLayout()
    private var currentView: RView? = null

    init {
        native.clipsToBounds = true
    }

    actual fun swap(transition: ScreenTransition, createNewView: ViewWriter.() -> ViewModifiable?): Unit {
        native.hidden = false
        currentView?.let { oldView ->
            oldView.animateOut(transition) {
                removeChild(oldView)
                native.hidden = native.subviews.isEmpty()
                native.informParentOfSizeChange()
            }
        }
        currentView = null

        var newView: RView? = null
        withoutAnimation {
            newView = createNewView()?.rView
            println("Swapping to $newView")
            currentView = newView
        }
        newView?.let {
            it.animateIn(transition) {}
        }
        native.extensionIgnoreInteraction = newView == null
    }

}