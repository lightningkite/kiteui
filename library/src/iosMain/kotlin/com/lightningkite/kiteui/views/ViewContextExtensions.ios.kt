package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.ExternalServices
import com.lightningkite.kiteui.afterTimeout
import com.lightningkite.kiteui.models.ScreenTransition
import com.lightningkite.kiteui.models.ScreenTransitions
import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.models.ThemeDerivation
import com.lightningkite.kiteui.views.direct.frame
import com.lightningkite.kiteui.views.l2.coordinatorFrame
import com.lightningkite.kiteui.views.l2.overlayFrame
import com.lightningkite.readable.AppScope
import com.lightningkite.readable.onRemove
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import platform.UIKit.UIModalPresentationOverFullScreen
import platform.UIKit.UIViewController

actual fun ViewWriter.overlayWriter(
    modal: Boolean,
    transition: ScreenTransitions,
    body: RView.(remove: () -> Unit) -> Unit
) {
    if (!modal) {
        var willRemove: RView? = null
        with(overlayFrame ?: return) {
            withoutAnimation {
                beforeNextElementSetup {
                    animateIn(transition.forward)
                    willRemove = this
                }
                body {
                    willRemove?.let {
                        it.animateOut(transition.reverse) {
                            this@with.removeChild(it)
                        }
                    }
                }
            }
        }
    } else {
        println("Waiting...")
        val theme = this@overlayWriter.overlayFrame?.theme ?: Theme.placeholder
        println("Let's go!")
        val viewController = object : UIViewController(null, null) {
            override fun viewDidDisappear(animated: Boolean) {
                super.viewDidDisappear(animated)
            }
        }
        viewController.definesPresentationContext = true
        viewController.modalPresentationStyle = UIModalPresentationOverFullScreen
        viewController.kiteUi(context.split(viewController)) {
            beforeNextElementSetup {
                themeChoice = ThemeDerivation { theme.withoutBack }
            }
            frame {
                coordinatorFrame = null
                overlayFrame = this
                body {
                    this@kiteUi.context.dismissSelf()
                }
            }
        }
        AppScope.launch {
            delay(100)
            context.present(viewController)
        }
    }
}