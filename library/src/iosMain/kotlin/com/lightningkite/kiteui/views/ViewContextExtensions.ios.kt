package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.models.ScreenTransitions
import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.models.ThemeDerivation
import com.lightningkite.kiteui.reactive.*
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

public actual fun ElementContext.overlay(
    modal: Boolean,
    navClosable: Boolean,
    transition: ScreenTransitions,
    body: ContainerElement.(remove: () -> Unit) -> Unit
) {
    if (!modal) {
        var willRemove: Element? = null
        with(overlayFrame ?: return) {
            withoutAnimation {
                beforeSetupContainer {
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
        val theme = this@overlay.overlayFrame?.theme ?: Theme.placeholder
        println("Let's go!")
        val viewController = object : UIViewController(null, null) {
            override fun viewDidDisappear(animated: Boolean) {
                super.viewDidDisappear(animated)
            }
        }
        viewController.definesPresentationContext = true
        viewController.modalPresentationStyle = UIModalPresentationOverFullScreen
        viewController.kiteUi(split(viewController)) {
            beforeSetup { themeChoice = ThemeDerivation { theme.withoutBack } }.frame {
                context.coordinatorFrame = null
                context.overlayFrame = this
                body {
                    this@kiteUi.context.dismissSelf()
                }
            }
        }
        AppScope.launch {
            delay(100)
            present(viewController)
        }
    }
}