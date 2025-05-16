package com.lightningkite.kiteui.views

import platform.UIKit.UIUserInterfaceStyle
import platform.UIKit.UIViewController

actual class RContext(val controller: UIViewController) : RContextHelper() {
    actual fun split(): RContext = RContext(controller).apply { addons.putAll(this@RContext.addons) }

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
}