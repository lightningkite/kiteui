package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.navigation.Screen
import com.lightningkite.kiteui.navigation.dialogScreenNavigator
import com.lightningkite.kiteui.reactive.BasicListenable
import com.lightningkite.kiteui.reactive.onRemove
import com.lightningkite.kiteui.viewDebugTarget
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.l2.overlayStack

actual class MenuButton actual constructor(context: RContext): RView(context) {
    override val native = FrameLayoutButton(this)

    actual fun opensMenu(createMenu: Stack.() -> Unit) {
        native.onClick = {
            var willRemove: RView? = null
            this.overlayStack!!.popoverWriter {
                willRemove?.let { it.parent!!.removeChild(it) }
                willRemove = null
            }.run {
                willRemove = dismissBackground {
                    viewDebugTarget = this
                    themeChoice += ThemeDerivation {
                        it.copy(background = Color.transparent).withBack
                    }
                    native.anchor = preferredDirection to this@MenuButton.native
                    onClick {
                        closePopovers()
                    }
                    card - stack {
                        themeChoice += ThemeDerivation {
                            it.copy(elevation = 5.dp, revert = true).withBack
                        }
                        createMenu()
                    }
                }
            }
        }
    }
    actual var enabled: Boolean
        get() = native.enabled
        set(value) {
            native.enabled = value
        }
    actual var requireClick: Boolean = true
    actual var preferredDirection: PopoverPreferredDirection = PopoverPreferredDirection.belowLeft

    init {
        onRemove(native.observe("highlighted", { refreshTheming() }))
        onRemove(native.observe("selected", { refreshTheming() }))
        onRemove(native.observe("enabled", { refreshTheming() }))
    }
    override fun hasAlternateBackedStates(): Boolean = true
    override fun applyState(theme: ThemeAndBack): ThemeAndBack {
        var t = theme
        if(!enabled) t = t[DisabledSemantic]
        if(native.highlighted) t = t[DownSemantic]
        if(native.focused) t = t[FocusSemantic]
        return super.applyState(t)
    }
}
