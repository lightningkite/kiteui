package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.views.ViewWriter
import android.widget.FrameLayout
import com.lightningkite.kiteui.models.DisabledSemantic
import com.lightningkite.kiteui.models.PopoverPreferredDirection
import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.models.ThemeAndBack
import com.lightningkite.kiteui.navigation.Screen
import com.lightningkite.kiteui.navigation.dialogScreenNavigator
import com.lightningkite.kiteui.navigation.screenNavigator
import com.lightningkite.kiteui.reactive.BasicListenable
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.l2.overlayStack
import kotlin.random.Random

actual class MenuButton actual constructor(context: RContext): RView(context) {
    override val native = FrameLayout(context.activity).apply {
        isClickable = true
    }

    actual fun opensMenu(createMenu: Stack.() -> Unit) {
        native.setOnClickListener { view ->
            var willRemove: RView? = null
            this.overlayStack!!.popoverWriter {
                willRemove?.let { overlayStack!!.removeChild(it) }
            }.run {
                willRemove = dismissBackground {
                    onClick {
                        closePopovers()
                    }
                    centered - card - stack {
                        createMenu()
                    }
                }
            }
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

    override fun hasAlternateBackedStates(): Boolean = true
    override fun applyState(theme: ThemeAndBack): ThemeAndBack {
        var t = theme
        if(!enabled) t = t[DisabledSemantic]
        return t
    }

    override fun applyBackground(theme: Theme, fullyApply: Boolean) = applyBackgroundWithRipple(theme, fullyApply)
}
