package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.Color
import com.lightningkite.kiteui.models.CornerRadii
import com.lightningkite.kiteui.models.DismissSemantic
import com.lightningkite.kiteui.models.PopoverPreferredDirection
import com.lightningkite.kiteui.models.PopoverSemantic
import com.lightningkite.kiteui.models.ThemeDerivation
import com.lightningkite.kiteui.models.ThemeDerivation.Companion.invoke
import com.lightningkite.kiteui.models.dp
import com.lightningkite.kiteui.views.RView
import com.lightningkite.kiteui.views.closePopovers
import com.lightningkite.kiteui.views.l2.overlayFrame
import com.lightningkite.kiteui.views.popoverWriter

actual fun RView.openPopover(
    preferredDirection: PopoverPreferredDirection,
    createMenu: Frame.() -> Unit
) {
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
            native.anchor = preferredDirection to this@openPopover.native
            onClick {
                closePopovers()
            }
            PopoverSemantic.onNext.frame {
                createMenu()
            }
        }
    }
}