package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.ScreenTransition
import com.lightningkite.kiteui.views.*
import kotlinx.cinterop.useContents
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import platform.CoreGraphics.*

actual class SwapView actual constructor(context: RContext): RView(context) {
    override val native = FrameLayout()


    actual fun swap(transition: ScreenTransition, createNewView: ViewWriter.() -> Unit): Unit {
//        clearChildren()
//        createNewView()
//        native.informParentOfSizeChange()
        native.hidden = false
        val oldView = children.lastOrNull()
        oldView?.let { oldView ->
            animateIfAllowed {
                println("Animating old view from ${oldView.native.transform.useContents { "$a $b $c $d $tx $ty" }} / ${oldView.native.alpha}")
                transition.exit(oldView.native)
                println("to ${oldView.native.transform.useContents { "$a $b $c $d $tx $ty" }} / ${oldView.native.alpha}")
            }
        }
        launch(Dispatchers.Main) {
            delay(500)
            oldView?.let { removeChild(it) }
            native.hidden = native.subviews.isEmpty()
            native.informParentOfSizeChange()
        }

        val newViewWriter = NewViewWriter(this, context)
        withoutAnimation {
            native.swapViewKeepLast = false
            newViewWriter.createNewView()
            newViewWriter.newView?.let {
                transition.enter(it.native)
                println("Animating new view from ${it.native.transform.useContents { "$a $b $c $d $tx $ty" }} / ${it.native.alpha}")

                addChild(it)
            }
        }
        val created = newViewWriter.newView
        created?.let { it ->
            animateIfAllowed {
                it.native.transform = CGAffineTransformMake(1.0, 0.0, 0.0, 1.0, 0.0, 0.0)
                it.native.alpha = 1.0
                println("to ${it.native.transform.useContents { "$a $b $c $d $tx $ty" }} / ${it.native.alpha}")
            }
        }
    }

}