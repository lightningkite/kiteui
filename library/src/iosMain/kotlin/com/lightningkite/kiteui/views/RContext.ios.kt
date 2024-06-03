package com.lightningkite.kiteui.views

import platform.UIKit.UIUserInterfaceStyle
import platform.UIKit.UIViewController

actual class RContext(val controller: UIViewController) : RContextHelper() {
    actual fun split(): RContext = RContext(controller).apply { addons.putAll(this@RContext.addons) }

    override val darkMode: Boolean?
        get() = when (controller.traitCollection.userInterfaceStyle) {
            UIUserInterfaceStyle.UIUserInterfaceStyleDark -> true
            UIUserInterfaceStyle.UIUserInterfaceStyleLight -> false
            else -> null
        }
}