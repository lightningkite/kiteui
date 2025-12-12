package com.lightningkite.kiteui.views.direct


import android.view.ViewGroup
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

            PopoverSemantic.onNext.frame {
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


fun Frame.configurePopoverLayout(
    dismissBackground: RView,
    anchorView: android.view.View,
    preferredDirection: PopoverPreferredDirection
) {
    native.layoutParams = android.widget.FrameLayout.LayoutParams(
        android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
        android.view.ViewGroup.LayoutParams.WRAP_CONTENT
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