package com.lightningkite.kiteui.views.direct


import com.lightningkite.kiteui.afterTimeout
import com.lightningkite.kiteui.models.ScreenTransition
import com.lightningkite.kiteui.views.NewViewWriter
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView
import com.lightningkite.kiteui.views.ViewModifiable
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.animateIfAllowed
import com.lightningkite.kiteui.views.animateIn
import com.lightningkite.kiteui.views.animateOut
import com.lightningkite.kiteui.views.extensionHorizontalAlign
import com.lightningkite.kiteui.views.extensionIgnoreInteraction
import com.lightningkite.kiteui.views.extensionVerticalAlign
import com.lightningkite.kiteui.views.informParentOfSizeChange
import com.lightningkite.kiteui.views.withoutAnimation
import kotlinx.cinterop.useContents
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import platform.CoreGraphics.CGAffineTransformMake


actual class SwapView actual constructor(context: RContext): RView(context) {
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

        val newViewWriter = NewViewWriter(this, context)
        withoutAnimation {
            newViewWriter.createNewView()
            newViewWriter.newView?.let {
                addChild(it)
                currentView = it
            }
        }
        newViewWriter.newView?.let {
            it.animateIn(transition) {}
        }
        native.extensionIgnoreInteraction = newViewWriter.newView == null
    }

}