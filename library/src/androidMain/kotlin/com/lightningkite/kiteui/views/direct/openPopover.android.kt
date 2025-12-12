package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.Color
import com.lightningkite.kiteui.models.CornerRadii
import com.lightningkite.kiteui.models.DismissSemantic
import com.lightningkite.kiteui.models.PopoverPreferredDirection
import com.lightningkite.kiteui.models.PopoverSemantic
import com.lightningkite.kiteui.models.SemanticOverrides
import com.lightningkite.kiteui.models.ThemeDerivation
import com.lightningkite.kiteui.models.dp
import com.lightningkite.kiteui.models.override
import com.lightningkite.kiteui.utils.getBoundariesInWindow
import com.lightningkite.kiteui.views.RView
import com.lightningkite.kiteui.views.atTopStart
import com.lightningkite.kiteui.views.closePopovers
import com.lightningkite.kiteui.views.l2.overlayFrame
import com.lightningkite.kiteui.views.popoverWriter

actual fun RView.openPopover(
    preferredDirection: PopoverPreferredDirection,
    anchor: RView?,
    createMenu: Frame.() -> Unit
) {
    var willRemove: RView? = null
    popoverWriter(this.overlayFrame!!) {
        willRemove?.let { overlayFrame!!.removeChild(it) }
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
                closePopovers()
            }

            atTopStart.onNext(PopoverSemantic).frame {
                this@dismissBackground.native.apply {
                    clipChildren = false
                    clipToPadding = false
                }
                this@dismissBackground.native.addOnLayoutChangeListener{ dismissBackground, _, _, _, _, _, _, _, _ ->
                    val overlayContainer = this@frame.native
                    val anchorView = (anchor ?: this@openPopover).native

                    val overlayBoundsInWindow = overlayContainer.getBoundariesInWindow()
                    val offset = preferredDirection.calculatePopoverOffset(
                        anchorView.getBoundariesInWindow(),
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