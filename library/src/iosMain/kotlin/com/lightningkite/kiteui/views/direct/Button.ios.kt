package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.reactive.await
import com.lightningkite.kiteui.reactive.invoke
import com.lightningkite.kiteui.reactive.reactiveScope
import com.lightningkite.kiteui.views.*

actual class Button actual constructor(context: RContext): RView(context) {
    override val native = FrameLayoutButton(this)
    init {
        activityIndicator {
            ::opacity.invoke { if (this@Button.working()) 1.0 else 0.0 }
            native.extensionSizeConstraints = SizeConstraints(minWidth = null, minHeight = null)
        }
    }

    actual fun onClick(action: suspend () -> Unit): Unit {
        native.onClick = action
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
        reactiveScope {
            opacity = if (working()) 0.7 else 1.0
        }
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