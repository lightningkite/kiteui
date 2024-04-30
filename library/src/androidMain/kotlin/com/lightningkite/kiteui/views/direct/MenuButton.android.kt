package com.lightningkite.kiteui.views.direct

import android.content.Context
import android.widget.FrameLayout
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.lightningkite.kiteui.launchManualCancel
import com.lightningkite.kiteui.models.PopoverPreferredDirection
import com.lightningkite.kiteui.models.rem
import com.lightningkite.kiteui.models.times
import com.lightningkite.kiteui.navigation.KiteUiNavigator
import com.lightningkite.kiteui.navigation.Screen
import com.lightningkite.kiteui.navigation.ScreenStack
import com.lightningkite.kiteui.reactive.CalculationContext
import com.lightningkite.kiteui.reactive.await
import com.lightningkite.kiteui.reactive.invoke
import com.lightningkite.kiteui.views.*

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual class NMenuButton(context: Context): SlightlyModifiedFrameLayout(context) {
    lateinit var navigator: ScreenStack
}

@ViewDsl
actual inline fun ViewWriter.menuButtonActual(crossinline setup: MenuButton.() -> Unit) {
    viewElement(factory = ::NMenuButton, wrapper = ::MenuButton) {
        val frame = native as NMenuButton
        native.isClickable = true
        native.navigator = navigator
        handleThemeControl(frame) {
            setup(this)
        }
    }
}

actual fun MenuButton.opensMenu(action: ViewWriter.() -> Unit) {
    native.setOnClickListener { view ->
        val originalNavigator = native.navigator
        native.navigator.dialog.navigate(object : Screen {
            override fun ViewWriter.render() {
                val dialogNavigator = navigator
                dismissBackground {
                    centered - card - stack {
                        val theme = currentTheme
                        ::spacing { theme().spacing * 2 }
                        popoverClosers.add {
                            dialogNavigator.dismiss()
                        }
                        navigator = originalNavigator
                        action()
                        navigator = dialogNavigator
                    }
                }
            }
        })
    }
}

actual var MenuButton.enabled: Boolean
    get() {
        return native.isEnabled
    }
    set(value) {
        native.isEnabled = value
    }
actual var MenuButton.requireClick: Boolean
    get() = true
    set(value) {}
actual var MenuButton.preferredDirection: PopoverPreferredDirection
    get() = PopoverPreferredDirection.belowLeft
    set(value) {}