package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.ExternalServices
import com.lightningkite.kiteui.models.ThemeDerivation
import com.lightningkite.kiteui.views.direct.frame
import com.lightningkite.kiteui.views.l2.overlayFrame
import platform.UIKit.UIModalPresentationOverFullScreen
import platform.UIKit.UIViewController

public actual fun ViewWriter.overlayWriter(body: RView.() -> Unit) {
    val viewController = object : UIViewController(null, null) {
        override fun viewDidDisappear(animated: Boolean) {
            super.viewDidDisappear(animated)
        }
    }
    viewController.modalPresentationStyle = UIModalPresentationOverFullScreen
    viewController.kiteUi(context.split()) {
        beforeNextElementSetup {
            this@overlayWriter.overlayFrame?.theme?.let { overlayTheme -> themeChoice = ThemeDerivation { overlayTheme.withoutBack } }
        }
        frame {
            overlayFrame = this
            body()
        }.also {
            it.children.first().onShutdown {
                viewController.dismissViewControllerAnimated(true) {  }
            }
        }
    }
    ExternalServices.present(viewController)
}