package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.reactive.PlatformEventSharedReadable
import com.lightningkite.kiteui.reactive.Readable
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.UIKit.UIKeyboardDidHideNotification
import platform.UIKit.UIKeyboardDidShowNotification
import platform.UIKit.UIUserInterfaceStyle
import platform.UIKit.UIViewController
import platform.darwin.NSObjectProtocol

actual class RContext(val controller: UIViewController) : RContextHelper() {
    actual fun split(): RContext = RContext(controller).apply { addons.putAll(this@RContext.addons) }

    actual override val darkMode: Boolean?
        get() = when (controller.traitCollection.userInterfaceStyle) {
            UIUserInterfaceStyle.UIUserInterfaceStyleDark -> true
            UIUserInterfaceStyle.UIUserInterfaceStyleLight -> false
            else -> null
        }

    private var keyboardDidShowObserver: NSObjectProtocol? = null
    private var keyboardDidHideObserver: NSObjectProtocol? = null
    override val keyboardVisible: Readable<Boolean> = PlatformEventSharedReadable(startup = { updateReadable ->
        println("Starting keyboard visible readable, registering with default notification center")
        keyboardDidShowObserver = NSNotificationCenter.defaultCenter.addObserverForName(UIKeyboardDidShowNotification, null, NSOperationQueue.mainQueue) {
            updateReadable(true)
        }
        keyboardDidHideObserver = NSNotificationCenter.defaultCenter.addObserverForName(UIKeyboardDidHideNotification, null, NSOperationQueue.mainQueue) {
            updateReadable(false)
        }
        false
    }, shutdown = {
        println("Shutting down keyboard visible readable")
        keyboardDidShowObserver?.let(NSNotificationCenter.defaultCenter::removeObserver)
        keyboardDidHideObserver?.let(NSNotificationCenter.defaultCenter::removeObserver)
    })
}