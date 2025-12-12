package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.l2.overlayFrame
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*

actual class MenuButton actual constructor(context: RContext): RView(context) {
    override val native = FrameLayoutButton()

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
                            cascading = false,
                            semanticOverrides = SemanticOverrides(
                                DismissSemantic.override {
                                    it.withBack(
                                        background = Color.transparent,
                                        outlineWidth = 0.dp,
                                        cornerRadii = CornerRadii.Constant(0.dp),
                                        cascading = false,
                                    )
                                }
                            )
                        ).withBack
                    }
                    native.anchor = preferredDirection to this@MenuButton.native
                    onClick {
                        closePopovers()
                    }
                    PopoverSemantic.onNext.frame {
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
