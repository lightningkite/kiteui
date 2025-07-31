package com.lightningkite.kiteui.views

import platform.UIKit.UIUserInterfaceStyle
import platform.UIKit.UIViewController

public actual class RContext(val controller: UIViewController) : RContextHelper() {
    public actual fun split(): RContext = RContext(controller).apply { addons.putAll(this@RContext.addons) }

    public actual override val darkMode: Boolean?
        get() = when (controller.traitCollection.userInterfaceStyle) {
            UIUserInterfaceStyle.UIUserInterfaceStyleDark -> true
            UIUserInterfaceStyle.UIUserInterfaceStyleLight -> false
            else -> null
        }
}