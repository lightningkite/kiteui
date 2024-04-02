package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.launchManualCancel
import com.lightningkite.kiteui.navigation.KiteUiNavigator
import com.lightningkite.kiteui.views.*
import kotlinx.cinterop.ExperimentalForeignApi
import platform.UIKit.UIControlEventTouchUpInside

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual typealias NDismissBackground = FrameLayoutButton

@ViewDsl
actual inline fun ViewWriter.dismissBackgroundActual(crossinline setup: DismissBackground.() -> Unit): Unit = element(FrameLayoutButton()) {
    handleTheme(
        this,
        foreground = {
            backgroundColor = it.background.closestColor().copy(alpha = 0.5f).toUiColor()
        },
    ) {
        val d = DismissBackground(this)
        __dismissBackgroundOtherSetupX(navigator)
        setup(d)
        __dismissBackgroundOtherSetup()
    }

}

fun FrameLayoutButton.__dismissBackgroundOtherSetupX(navigator: KiteUiNavigator) {
    DismissBackground(this).onClick {
        navigator.dismiss()
    }
}
fun FrameLayoutButton.__dismissBackgroundOtherSetup() {
    listNViews().forEach {
        it.userInteractionEnabled = true
    }
}

@OptIn(ExperimentalForeignApi::class)
actual fun DismissBackground.onClick(action: suspend () -> Unit): Unit {
    var virtualDisable: Boolean = false
    native.onEvent(UIControlEventTouchUpInside) {
        if(!virtualDisable) {
            native.calculationContext.launchManualCancel {
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