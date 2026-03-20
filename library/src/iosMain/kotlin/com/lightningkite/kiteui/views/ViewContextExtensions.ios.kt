package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.afterTimeout
import com.lightningkite.kiteui.models.ScreenTransition
import com.lightningkite.kiteui.models.ScreenTransitions
import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.models.ThemeDerivation
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.beforeSetup
import com.lightningkite.kiteui.views.direct.frame
import com.lightningkite.kiteui.views.l2.coordinatorFrame
import com.lightningkite.kiteui.views.l2.overlayFrame
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import platform.UIKit.UIModalPresentationOverFullScreen
import platform.UIKit.UIViewController

actual fun ViewWriter.overlayWriter(
    modal: Boolean,
    transition: ScreenTransitions,
    body: ViewWriter.(remove: () -> Unit) -> Unit
) {
    if (!modal) {
        var willRemove: RView? = null
        with(overlayFrame ?: return) {
            withoutAnimation {
                beforeSetup {
                    animateIn(transition.forward)
                    willRemove = this
                }.body {
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
            beforeSetup { themeChoice = ThemeDerivation { theme.withoutBack } }.frame {
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