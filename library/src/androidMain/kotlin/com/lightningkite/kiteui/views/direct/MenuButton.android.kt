package com.lightningkite.kiteui.views.direct

import android.widget.FrameLayout
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.utils.getBoundariesInWindow
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.l2.overlayFrame

actual class MenuButton actual constructor(context: RContext): RView(context) {
    private var _openMenu: (() -> Unit)? = null
    override val driverActions get() = super.driverActions + menuDriverActions(_openMenu)
    override val native = FrameLayout(context.activity).apply {
        isClickable = true
    }

    actual fun opensMenu(createMenu: Frame.() -> Unit) {
        val openFn = {
            var willRemove: RView? = null
            popoverWriter(this.overlayFrame!!) {
                val r = willRemove
                willRemove = null
                r?.let { overlayFrame!!.removeChild(it) }

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
        native.setOnClickListener { openFn() }
        _openMenu = openFn
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
