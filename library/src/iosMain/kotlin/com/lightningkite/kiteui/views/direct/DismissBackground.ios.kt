package com.lightningkite.kiteui.views.direct


import com.lightningkite.kiteui.launchManualCancel
import com.lightningkite.kiteui.models.SizeConstraints
import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.navigation.ScreenStack
import com.lightningkite.kiteui.reactive.await
import com.lightningkite.kiteui.reactive.invoke
import com.lightningkite.kiteui.views.*
import kotlinx.cinterop.ExperimentalForeignApi
import platform.UIKit.UIControlEventTouchUpInside


actual class DismissBackground actual constructor(context: RContext): RView(context) {
    override val native = FrameLayoutButton(this)

    actual fun onClick(action: suspend () -> Unit): Unit {
        var virtualDisable: Boolean = false
        native.onEvent(this, UIControlEventTouchUpInside) {
            if(!virtualDisable) {
                launchManualCancel {
                    try {
                        virtualDisable = true
                        action()
                    } finally {
                        virtualDisable = false
                    }
                }
            }
        }
    }

    override fun postSetup() {
        super.postSetup()
        children.forEach { it.native.userInteractionEnabled = true }
    }

    init {
        onClick { navigator.clear() }
    }

    override fun applyBackground(theme: Theme, fullyApply: Boolean) {
        native.backgroundColor = theme.background.closestColor().copy(alpha = 0.5f).toUiColor()
    }
}
