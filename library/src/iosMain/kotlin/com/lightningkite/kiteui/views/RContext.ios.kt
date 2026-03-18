package com.lightningkite.kiteui.views

import platform.UIKit.UIUserInterfaceStyle
import platform.UIKit.UIViewController

actual class ElementContext(val controller: UIViewController, val parent: ElementContext? = null) : ElementContextCommonCode(parent) {
    // by Claude - use addons.child() for lazy parent lookup instead of copying
    actual fun split(): ElementContext = ElementContext(controller, this)

    fun split(controller: UIViewController): ElementContext = ElementContext(controller, this@ElementContext)

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