package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.InternalKiteUi
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.reactive.Action
import com.lightningkite.kiteui.views.*
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*

@InternalKiteUi
public actual class Button actual constructor(context: RContext) : RViewWithSecondaryAction(context) {
    override val native: FrameLayoutButton = FrameLayoutButton()

    init {
        activityIndicator {
            opacity = 0.0
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

    override fun secondaryActionSet(value: Action?) {
        super.secondaryActionSet(value)
        onRemove(native.setOnLongPress {
            value?.startAction(this)
        })
    }

    public actual var enabled: Boolean
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

    override fun applyState(theme: ThemeAndBack): ThemeAndBack {
        var t = theme[ClickableSemantic]
        if (!enabled) t = t[DisabledSemantic]
        if (native.highlighted) t = t[DownSemantic]
        if (native.focused) t = t[FocusSemantic]
        return super.applyState(t)
    }
}