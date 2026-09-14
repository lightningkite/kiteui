package com.lightningkite.kiteui.views.direct

import android.view.ViewGroup
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.utils.getBoundariesInWindow
import com.lightningkite.kiteui.views.Element
import com.lightningkite.kiteui.views.closePopovers
import com.lightningkite.kiteui.views.l2.overlayFrame
import com.lightningkite.kiteui.views.native
import com.lightningkite.kiteui.views.popoverWriter
import com.lightningkite.kiteui.views.themed

public actual fun Element.openPopover(
    preferredDirection: PopoverPreferredDirection,
    anchor: Element?,
    createMenu: Frame.() -> Unit
) {
    var willRemove: Element? = null
    popoverWriter(context.overlayFrame) {
        willRemove?.let { context.overlayFrame.removeChild(it) }
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
            onClick {
                context.closePopovers()
            }

            themed(PopoverSemantic).frame {
                configurePopoverLayout(
                    dismissBackground = this@dismissBackground,
                    anchorView = (anchor ?: this@openPopover).native,
                    preferredDirection = preferredDirection
                )
                createMenu()
            }
        }
    }
}

public fun Frame.configurePopoverLayout(
    dismissBackground: Element,
    anchorView: android.view.View,
    preferredDirection: PopoverPreferredDirection
) {
    native.layoutParams = android.widget.FrameLayout.LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
    ).apply {
        gravity = android.view.Gravity.TOP or android.view.Gravity.LEFT
    }
    (dismissBackground.native as? ViewGroup)?.apply {
        clipChildren = false
        setClipToPadding(false)
    }
    dismissBackground.native.addOnLayoutChangeListener { dismissBackgroundView, _, _, _, _, _, _, _, _ ->
        val overlayContainer = this.native

        val overlayBoundsInWindow = overlayContainer.getBoundariesInWindow()
        val offset = preferredDirection.calculatePopoverOffset(
            anchorView.getBoundariesInWindow(),
            overlayBoundsInWindow,
            dismissBackgroundView.getBoundariesInWindow()
        )

        overlayContainer.offsetLeftAndRight((offset.first - overlayBoundsInWindow.left).toInt())
        overlayContainer.offsetTopAndBottom((offset.second - overlayBoundsInWindow.top).toInt())
    }
}