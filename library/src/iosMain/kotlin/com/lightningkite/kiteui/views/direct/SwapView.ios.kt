package com.lightningkite.kiteui.views.direct


import com.lightningkite.kiteui.afterTimeout
import com.lightningkite.kiteui.models.ScreenTransition
import com.lightningkite.kiteui.views.NewViewWriter
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.animateIfAllowed
import com.lightningkite.kiteui.views.extensionIgnoreInteraction
import com.lightningkite.kiteui.views.informParentOfSizeChange
import com.lightningkite.kiteui.views.withoutAnimation
import kotlinx.cinterop.useContents
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import platform.CoreGraphics.CGAffineTransformMake


actual class SwapView actual constructor(context: RContext): RView(context) {
    override val native = FrameLayout()
    private var currentView: RView? = null

    actual fun swap(transition: ScreenTransition, createNewView: ViewWriter.() -> Unit): Unit {
//        clearChildren()
//        createNewView()
//        native.informParentOfSizeChange()
        native.hidden = false
        currentView?.let { oldView ->
            animateIfAllowed {
                transition.exit(oldView.native)
            }
            launch {
                delay(theme.transitionDuration)
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
                transition.enter(it.native)
                addChild(it)
                currentView = it
            }
        }
        val created = newViewWriter.newView
        created?.let { it ->
            native.extensionIgnoreInteraction = !true
            animateIfAllowed {
                it.native.transform = CGAffineTransformMake(1.0, 0.0, 0.0, 1.0, 0.0, 0.0)
                it.native.alpha = 1.0
                println("to ${it.native.transform.useContents { "$a $b $c $d $tx $ty" }} / ${it.native.alpha}")
            }
        } ?: run {
            native.extensionIgnoreInteraction = !false
        }
    }

}