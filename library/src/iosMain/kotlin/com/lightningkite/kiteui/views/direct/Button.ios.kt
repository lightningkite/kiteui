package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.reactive.Action
import com.lightningkite.readable.invoke
import com.lightningkite.readable.onRemove
import com.lightningkite.readable.reactiveScope
import com.lightningkite.kiteui.views.*

actual class Button actual constructor(context: RContext) : RViewWithAction(context) {
    override val native = FrameLayoutButton()

    init {
        activityIndicator {
            ::opacity.invoke { if (this@Button.working()) 1.0 else 0.0 }
            native.extensionSizeConstraints = SizeConstraints(minWidth = null, minHeight = null)
        }
    }

    override fun actionSet(value: Action?) {
        super.actionSet(value)
        onRemove(native.setOnClick {
            value?.startAction(this)
        })
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
            opacity = if (loading()) 0.7 else 1.0
        }
    }

    override fun hasAlternateBackedStates(): Boolean = true
    override fun applyState(theme: ThemeAndBack): ThemeAndBack {
        var t = theme
        if (!enabled) t = t[DisabledSemantic]
        if (native.highlighted) t = t[DownSemantic]
        if (native.focused) t = t[FocusSemantic]
        return super.applyState(t)
    }
}