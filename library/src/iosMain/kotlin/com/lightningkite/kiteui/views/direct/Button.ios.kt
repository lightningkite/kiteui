package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.reactive.Action
import com.lightningkite.readable.onRemove
import com.lightningkite.readable.reactiveScope
import com.lightningkite.kiteui.views.*

actual class Button actual constructor(context: RContext) : RViewWithSecondaryAction(context) {
    override val native = FrameLayoutButton()
    override fun childTouches(side: Side, child: RView): Boolean {
        return when(side) {
            Side.Left -> child.native.extensionHorizontalAlign?.touchesStart != false
            Side.Top -> child.native.extensionVerticalAlign?.touchesStart != false
            Side.Right -> child.native.extensionHorizontalAlign?.touchesEnd != false
            Side.Bottom -> child.native.extensionVerticalAlign?.touchesEnd != false
        }
    }

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

    override fun applyState(theme: ThemeAndBack): ThemeAndBack {
        var t = theme[ClickableSemantic]
        if (!enabled) t = t[DisabledSemantic]
        if (native.highlighted) t = t[DownSemantic]
        if (native.focused) t = t[FocusSemantic]
        return super.applyState(t)
    }
}