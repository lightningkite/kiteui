package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.l2.overlayFrame

public actual fun Element.openPopover(
    preferredDirection: PopoverPreferredDirection,
    anchor: Element?,
    createMenu: Frame.() -> Unit
) {
    var willRemove: Element? = null
    val f = overlayFrame!!
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
                                cornerRadii = CornerRadii.AdaptiveToSpacing(0.dp),
                                cascading = false,
                            )
                        }
                    )
                ).withBack
            }
            native.anchor = preferredDirection to (anchor ?: this@openPopover).native
            onClick {
                context.closePopovers()
            }
            themed(PopoverSemantic).frame {
                createMenu()
            }
        }
    }
}