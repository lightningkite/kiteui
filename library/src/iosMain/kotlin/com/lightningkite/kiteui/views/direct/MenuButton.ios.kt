package com.lightningkite.kiteui.views.direct


import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.navigation.Screen
import com.lightningkite.kiteui.navigation.dialogScreenNavigator
import com.lightningkite.kiteui.reactive.BasicListenable
import com.lightningkite.kiteui.views.*
import kotlinx.cinterop.ExperimentalForeignApi
import kotlin.experimental.ExperimentalNativeApi

@OptIn(ExperimentalForeignApi::class, ExperimentalNativeApi::class)
actual class MenuButton actual constructor(context: RContext): RView(context) {
    override val native = FrameLayoutButton(this)

    actual fun opensMenu(createMenu: Stack.() -> Unit) {
        native.onClick = {
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
        return t
    }
}
