package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.utils.getBoundariesInWindow
import com.lightningkite.kiteui.views.Element
import com.lightningkite.kiteui.views.atTopStart
import com.lightningkite.kiteui.views.closePopovers
import com.lightningkite.kiteui.views.l2.overlayFrame
import com.lightningkite.kiteui.views.native
import com.lightningkite.kiteui.views.popoverWriter
import com.lightningkite.kiteui.views.themed

actual fun Element.openPopover(
    preferredDirection: PopoverPreferredDirection,
    createMenu: Frame.() -> Unit
) {
    var willRemove: Element? = null
    popoverWriter(context.overlayFrame!!) {
        willRemove?.let { context.overlayFrame!!.removeChild(it) }
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
            onClick {
                context.closePopovers()
            }

            atTopStart.themed(PopoverSemantic).frame {
                this@dismissBackground.native.apply {
                    clipChildren = false
                    clipToPadding = false
                }
                this@dismissBackground.native.addOnLayoutChangeListener { dismissBackground, _, _, _, _, _, _, _, _ ->
                    val overlayContainer = this@frame.native
                    val anchor = this@openPopover.native

                    val overlayBoundsInWindow = overlayContainer.getBoundariesInWindow()
                    val offset = preferredDirection.calculatePopoverOffset(
                        anchor.getBoundariesInWindow(),
                        overlayBoundsInWindow,
                        dismissBackground.getBoundariesInWindow()
                    )

                    overlayContainer.offsetLeftAndRight((offset.first - overlayBoundsInWindow.left).toInt())
                    overlayContainer.offsetTopAndBottom((offset.second - overlayBoundsInWindow.top).toInt())
                }
                createMenu()
            }
        }
    }
}