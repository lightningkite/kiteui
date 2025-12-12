package com.lightningkite.kiteui.views.direct

import android.widget.FrameLayout
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.utils.getBoundariesInWindow
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.l2.overlayFrame

actual class MenuButton actual constructor(context: RContext): RView(context) {
    override val native = FrameLayout(context.activity).apply {
        isClickable = true
    }

    actual fun opensMenu(createMenu: Frame.() -> Unit) {
        native.setOnClickListener { view ->
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
                        configurePopoverLayout(
                            dismissBackground = this@dismissBackground,
                            anchorView = this@MenuButton.native,
                            preferredDirection = preferredDirection
                        )
                        createMenu()
                    }
                }
            }
        }
    }

    actual var requireClick: Boolean = true
    actual var preferredDirection: PopoverPreferredDirection = PopoverPreferredDirection.belowLeft

    actual var enabled: Boolean
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
