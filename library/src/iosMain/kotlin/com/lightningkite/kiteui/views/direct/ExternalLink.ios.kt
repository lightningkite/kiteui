package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.reactive.context.*
import kotlinx.coroutines.launch
import platform.Foundation.NSURL
import platform.UIKit.UIApplication

actual class ExternalLink actual constructor(context: ElementContext): RView(context) {
    override val driverActions get() = super.driverActions + externalLinkDriverActions()
    override val native = FrameLayoutButton()
    init {
        onRemove(native.setOnClick {
            to?.let { to ->
                launch {
                    onNavigate()
                    UIApplication.sharedApplication.openURL(NSURL(string = to), mapOf<Any?, Any?>()) {}
                }
            }
        })
    }


    actual var to: String? = null
    actual var newTab: Boolean = false
    private var onNavigate: suspend () -> Unit = {}
    actual fun onNavigate(action: suspend () -> Unit): Unit {
        onNavigate = action
    }

    actual var enabled: Boolean
        get() = native.enabled
        set(value) {
            native.enabled = value
        }

    init {
        onRemove(native.observe("highlighted", { refreshTheming() }))
        onRemove(native.observe("selected", { refreshTheming() }))
        onRemove(native.observe("enabled", { refreshTheming() }))
    }

    override fun applyState(theme: ThemeAndBack): ThemeAndBack {
        var t = theme[ClickableSemantic]
        if(!enabled) t = t[DisabledSemantic]
        if(native.highlighted) t = t[DownSemantic]
        if(native.focused) t = t[FocusSemantic]
        return super.applyState(t)
    }
}
