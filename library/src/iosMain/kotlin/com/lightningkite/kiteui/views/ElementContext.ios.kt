package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.Log
import platform.UIKit.UIApplication
import platform.UIKit.UIUserInterfaceStyle
import platform.UIKit.UIViewController

public actual class ElementContext(public val controller: UIViewController, parent: ElementContext? = null) : ElementContextCommonCode(parent) {
    public actual fun child(): ElementContext = ElementContext(controller, parent = this)
    /**
     * Returns a child context for a new view controller, such as a presented modal.
     */
    public fun child(controller: UIViewController): ElementContext = ElementContext(controller, parent = this)
    @Deprecated("Renamed", ReplaceWith("child(controller)"))
    public fun split(controller: UIViewController): ElementContext = child(controller)

    public actual val root: ElementContext = parent?.root ?: this

    public actual val darkMode: Boolean?
        get() = when (controller.traitCollection.userInterfaceStyle) {
            UIUserInterfaceStyle.UIUserInterfaceStyleDark -> true
            UIUserInterfaceStyle.UIUserInterfaceStyleLight -> false
            else -> null
        }

    /**
     * Set to `true` to hide the system status bar.
     *
     * Set "View controller-based status bar appearance" to YES in Info.plist. Then make your view controller's
     * `prefersStatusBarHidden` return `context.immersiveMode`.
     */
    public actual var immersiveMode: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                controller.setNeedsStatusBarAppearanceUpdate()
            }
        }

    public val controllerForPresenting: UIViewController? get() = generateSequence(controller) { it.parentViewController }.firstOrNull { it.definesPresentationContext } as? UIViewController?
    private var dismissing: Boolean = false
    public fun dismissSelf() {
        dismissing = true
        Log.info("Dismissing myself $controller through ${parent?.controller}")
        controller.presentingViewController?.dismissViewControllerAnimated(true) {}
    }
    public fun present(vc: UIViewController) {
        // 1. Try to find a valid controller in the ElementContext hierarchy
        val contextToUse = generateSequence(this) { it.parent }.firstOrNull {
            !it.dismissing && it.controller.view.window != null
        }

        val host = contextToUse?.controllerForPresenting ?: run {
            // 2. FALLBACK: Get the actual active root view controller from the window
            Log.info("ElementContext chain stale, falling back to Window Root")
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

    public actual companion object : kotlin.coroutines.CoroutineContext.Key<ElementContext>
}
