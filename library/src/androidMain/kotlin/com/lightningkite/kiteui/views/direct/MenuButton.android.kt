package com.lightningkite.kiteui.views.direct

import android.widget.FrameLayout
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.AiDriver
import com.lightningkite.kiteui.views.l2.overlayFrame

public actual class MenuButton actual constructor(context: ElementContext): NativeInteractiveContainerElement(context) {
    override val driverActions: AiDriver.Actions get() = super.driverActions + menuDriverActions()
    override val native: FrameLayout = FrameLayout(context.activity).apply {
        isClickable = true
    }

    private var _openMenu: (() -> Unit)? = null

    public actual fun opensMenu(createMenu: Frame.() -> Unit) {
        val openFn = {
            var willRemove: Element? = null
            popoverWriter(context.overlayFrame) {
                val r = willRemove
                willRemove = null
                r?.let { context.overlayFrame.removeChild(it) }

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
                            anchorView = this@MenuButton.native,
                            preferredDirection = this@MenuButton.preferredDirection
                        )
                        createMenu()
                    }
                }
            }
        }
        native.setOnClickListener {
            openFn()
            @Suppress("DEPRECATION")
            native.announceForAccessibility("Menu opened")
        }
        _openMenu = openFn
    }

    public actual var requireClick: Boolean = true
    public actual var preferredDirection: PopoverPreferredDirection = PopoverPreferredDirection.belowLeft

    override fun nativeApplyTheme(theme: ThemeAndBack): Unit = applyThemeWithRipple(theme)
}
