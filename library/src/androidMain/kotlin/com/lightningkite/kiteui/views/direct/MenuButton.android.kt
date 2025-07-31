package com.lightningkite.kiteui.views.direct

import android.widget.FrameLayout
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.utils.getBoundariesInWindow
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.l2.overlayFrame

public actual class MenuButton public actual constructor(context: RContext): RView(context) {
    override val native = FrameLayout(context.activity).apply {
        isClickable = true
    }

    public actual fun opensMenu(createMenu: Frame.() -> Unit) {
        native.setOnClickListener { view ->
            var willRemove: RView? = null
            this.overlayFrame!!.popoverWriter {
                willRemove?.let { overlayFrame!!.removeChild(it) }
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
                    onClick {
                        closePopovers()
                    }
                    atTopStart - dialog - frame {
                        this@dismissBackground.native.apply {
                            clipChildren = false
                            clipToPadding = false
                        }
                        this@dismissBackground.native.addOnLayoutChangeListener{ dismissBackground, _, _, _, _, _, _, _, _ ->
                            val overlayContainer = this@frame.native
                            val anchor = this@MenuButton.native

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
    }

    public actual var requireClick: Boolean = true
    public actual var preferredDirection: PopoverPreferredDirection = PopoverPreferredDirection.belowLeft

    public actual var enabled: Boolean
        get() = native.isEnabled
        set(value) {
            native.isEnabled = value
            refreshTheming()
        }

    override fun applyState(theme: ThemeAndBack): ThemeAndBack {
        var t = theme[ClickableSemantic]
        if(!enabled) t = t[DisabledSemantic]
        return super.applyState(t)
    }

    override fun applyTheme(theme: ThemeAndBack) = applyThemeWithRipple(theme)
}
