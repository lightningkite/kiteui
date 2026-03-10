package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.models.Edges
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*
import platform.UIKit.UIUserInterfaceStyle
import platform.UIKit.UIViewController

actual class RContext(val controller: UIViewController, val parent: RContext? = null) : RContextHelper() {
    // by Claude - use addons.child() for lazy parent lookup instead of copying
    actual fun split(): RContext = RContext(controller).apply { addons = this@RContext.addons.child() }
    fun split(controller: UIViewController): RContext = RContext(controller, this@RContext).apply { addons = this@RContext.addons.child() }

    actual override val darkMode: Boolean?
        get() = when (controller.traitCollection.userInterfaceStyle) {
            UIUserInterfaceStyle.UIUserInterfaceStyleDark -> true
            UIUserInterfaceStyle.UIUserInterfaceStyleLight -> false
            else -> null
        }

    // !!! SETUP REQUIRED !!!
    // To enable immersive mode for iOS, you must:
    //    1) set "View controller-based status bar appearance" to YES in your Info.plist
    //    2) override prefersStatusBarHidden in your view controller and point it to this variable
    actual var immersiveMode: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                controller.setNeedsStatusBarAppearanceUpdate()
            }
        }

    val controllerForPresenting get() = generateSequence(controller) { it.parentViewController }.firstOrNull { it.definesPresentationContext } as? UIViewController?
    private var dismissing: Boolean = false
    fun dismissSelf() {
        dismissing = true
        println("Dismissing myself $controller through ${parent?.controller}")
        controller.presentingViewController?.dismissViewControllerAnimated(true) {}
    }
    fun present(vc: UIViewController) {
        println("$controller present $vc")
        val contextToUse = generateSequence(this) { it.parent }.first {
            println("Can I present from ${it.controller}?  Dismissing is ${it.dismissing}")
            !it.dismissing && it.controller.view.window != null
        }
        val controller = contextToUse.controllerForPresenting
        if(controller == null) return
        if(controller.presentedViewController != null) {
            println("Dismissing old on $controller")
            controller.dismissViewControllerAnimated(true) {
                println("Ready to present next")
                controller.presentViewController(vc, animated = true, completion = null)
            }
        } else {
            println("About to present")
            controller.presentViewController(vc, animated = true, completion = null)
        }
    }

    actual companion object {}
}