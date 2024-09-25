package com.lightningkite.kiteui.views.direct


import com.lightningkite.kiteui.afterTimeout
import com.lightningkite.kiteui.models.Dimension
import com.lightningkite.kiteui.models.ScreenTransition
import com.lightningkite.kiteui.objc.UIViewWithSizeOverridesProtocol
import com.lightningkite.kiteui.objc.UIViewWithSpacingRulesProtocol
import com.lightningkite.kiteui.reactive.Property
import com.lightningkite.kiteui.views.*
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readValue
import platform.CoreGraphics.*
import platform.UIKit.UIEvent
import platform.UIKit.UISwitch
import platform.UIKit.UIView


@OptIn(ExperimentalForeignApi::class)
actual class SwapView actual constructor(context: RContext): RView(context) {
    override val native = FrameLayout()

    @OptIn(ExperimentalForeignApi::class)
    actual fun swap(transition: ScreenTransition, createNewView: ViewWriter.() -> Unit): Unit {
        clearChildren()
        createNewView()
        native.informParentOfSizeChange()
//        native.hidden = false
//        val oldView = children.lastOrNull()
//        oldView?.let { oldView ->
//            native.animateIfAllowed {
//                transition.exit(oldView.native)
//            }
//        }
//        afterTimeout((native.extensionAnimationDuration ?: 0.15).times(1000).toLong()) {
//            oldView?.let { removeChild(it) }
//            native.hidden = native.subviews.isEmpty()
//            native.informParentOfSizeChange()
//        }
//
//        val newViewWriter = NewViewWriter(context)
//        withoutAnimation {
//            native.swapViewKeepLast = false
//            newViewWriter.createNewView()
//            newViewWriter.newView?.let {
//                transition.enter(it.native)
//                addChild(it)
//            }
//        }
//        val created = newViewWriter.newView
//        created?.let { created ->
//            native.animateIfAllowed {
//                created.native.transform = CGAffineTransformMake(1.0, 0.0, 0.0, 1.0, 0.0, 0.0)
//                created.native.alpha = 1.0
//            }
//        }
    }

}