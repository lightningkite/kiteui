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

actual class MenuButton actual constructor(context: RContext): RView(context) {
    override val native = FrameLayout(context.activity).apply {
        isClickable = true
    }

    actual fun opensMenu(createMenu: Stack.() -> Unit) {
        native.setOnClickListener { view ->
            dialogScreenNavigator.navigate(object : Screen {
                override fun ViewWriter.render() {
                    dismissBackground {
                        centered - card - stack {
                           object: ViewWriter() {
                               override val context: RContext = this@stack.context.split().also {
                                   popoverClosers = BasicListenable().also {
                                       it.addListener {
                                           dialogScreenNavigator.dismiss()
                                       }
                                   }
                               }
                               override fun addChild(view: RView) {
                                   this@stack.addChild(view)
                               }
                           }
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

    override fun hasAlternateBackedStates(): Boolean = true
    override fun applyState(theme: ThemeAndBack): ThemeAndBack {
        var t = theme
        if(!enabled) t = t[DisabledSemantic]
        return t
    }

    override fun applyBackground(theme: Theme, fullyApply: Boolean) = applyBackgroundWithRipple(theme, fullyApply)
}
