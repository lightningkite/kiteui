package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.Log
import platform.UIKit.UIApplication
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
        Log.info("Dismissing myself $controller through ${parent?.controller}")
        controller.presentingViewController?.dismissViewControllerAnimated(true) {}
    }
    fun present(vc: UIViewController) {
        // 1. Try to find a valid controller in the RContext hierarchy
        val contextToUse = generateSequence(this) { it.parent }.firstOrNull {
            !it.dismissing && it.controller.view.window != null
        }

        val host = contextToUse?.controllerForPresenting ?: run {
            // 2. FALLBACK: Get the actual active root view controller from the window
            Log.info("RContext chain stale, falling back to Window Root")
            UIApplication.sharedApplication.keyWindow?.rootViewController?.let {
                generateSequence(it) { current -> current.presentedViewController }.last()
            }
        }

        if (host == null) {
            Log.error("Dismissing myself $controller through ${parent?.controller}")
            return
        }

        // 3. Perform the presentation on the host
        if (host.presentedViewController != null) {
            host.dismissViewControllerAnimated(true) {
                host.presentViewController(vc, animated = true, completion = null)
            }
        } else {
            host.presentViewController(vc, animated = true, completion = null)
        }
    }

    actual companion object {}
}
