package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.launchManualCancel
import com.lightningkite.kiteui.navigation.ScreenStack
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
            backgroundColor = it.background.closestColor().darken(0.5f).applyAlpha(alpha = 0.5f).toUiColor()
        },
    ) {
        val d = DismissBackground(this)
        __dismissBackgroundOtherSetupX(navigator)
        setup(d)
        __dismissBackgroundOtherSetup()
    }

}

fun FrameLayoutButton.__dismissBackgroundOtherSetupX(navigator: ScreenStack) {
    DismissBackground(this).onClick {
        navigator.clear()
    }
}
fun FrameLayoutButton.__dismissBackgroundOtherSetup() {
    listNViews().forEach {
        it.userInteractionEnabled = true
    }
}

@OptIn(ExperimentalForeignApi::class)
actual fun DismissBackground.onClick(action: suspend () -> Unit): Unit {
    native.onClick = action
}