package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.views.ViewWriter
import android.widget.FrameLayout
import com.lightningkite.kiteui.models.PopoverPreferredDirection
import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.navigation.Screen
import com.lightningkite.kiteui.views.*

actual class MenuButton actual constructor(context: RContext): RView(context) {
    override val native = FrameLayout(context.activity).apply {
        isClickable = true
    }

    actual fun opensMenu(action: ViewWriter.() -> Unit) {
        native.setOnClickListener { view ->
            val originalNavigator = navigator
            navigator.dialog.navigate(object : Screen {
                override fun ViewWriter.render() {
                    val dialogNavigator = navigator
                    dismissBackground {
                        centered - card - stack {
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

    actual var requireClick: Boolean = true
    actual var preferredDirection: PopoverPreferredDirection = PopoverPreferredDirection.belowLeft

    actual var enabled: Boolean
        get() = native.isEnabled
        set(value) {
            native.isEnabled = value
            refreshTheming()
        }

    override fun getStateThemeChoice() = when {
        !enabled -> ThemeChoice.Derive { it.disabled() }
        else -> null
    }

    init { if(useBackground == UseBackground.No) useBackground = UseBackground.IfChanged }
    override fun applyBackground(theme: Theme, fullyApply: Boolean) = applyBackgroundWithRipple(theme, fullyApply)
}
