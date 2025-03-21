package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.*
import com.lightningkite.readable.onRemove
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.l2.overlayFrame

actual class MenuButton actual constructor(context: RContext): RView(context) {
    override val native = FrameLayoutButton()
    override fun childTouches(side: Side, child: RView): Boolean {
        return when(side) {
            Side.Left -> child.native.extensionHorizontalAlign?.touchesStart != false
            Side.Top -> child.native.extensionVerticalAlign?.touchesStart != false
            Side.Right -> child.native.extensionHorizontalAlign?.touchesEnd != false
            Side.Bottom -> child.native.extensionVerticalAlign?.touchesEnd != false
        }
    }

    actual fun opensMenu(createMenu: Frame.() -> Unit) {
        onRemove(native.setOnClick {
            var willRemove: RView? = null
            val f= overlayFrame!!
            f.popoverWriter {
                willRemove?.let { f.removeChild(it) }
                willRemove = null
            }.run {
                willRemove = dismissBackground {
                    themeChoice += ThemeDerivation {
                        it.copy(
                            id = "mnubtndsm",
                            revert = true,
                            derivations = mapOf(
                                DismissSemantic to {
                                    it.copy(
                                        background = Color.transparent,
                                        outlineWidth = 0.dp,
                                        cornerRadii = CornerRadii.Constant(0.dp),
                                        revert = true,
                                    ).withBack
                                }
                            )
                        ).withBack
                    }
                    native.anchor = preferredDirection to this@MenuButton.native
                    onClick {
                        closePopovers()
                    }
                    dialog - frame {
                        createMenu()
                    }
                }
            }
        })
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
    override fun applyState(theme: ThemeAndBack): ThemeAndBack {
        var t = theme[ClickableSemantic]
        if(!enabled) t = t[DisabledSemantic]
        if(native.highlighted) t = t[DownSemantic]
        if(native.focused) t = t[FocusSemantic]
        return super.applyState(t)
    }
}
