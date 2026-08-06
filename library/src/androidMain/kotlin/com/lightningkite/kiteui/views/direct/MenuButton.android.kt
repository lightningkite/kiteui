package com.lightningkite.kiteui.views.direct

import android.widget.FrameLayout
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.utils.getBoundariesInWindow
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.closePopovers
import com.lightningkite.kiteui.views.l2.overlayFrame
import com.lightningkite.kiteui.views.themed

public actual class MenuButton actual constructor(context: ElementContext): NativeInteractiveContainerElement(context) {
    override val driverActions: Map<String, suspend (List<String>) -> String> get() = super.driverActions + menuDriverActions()
    override val native: FrameLayout = FrameLayout(context.activity).apply {
        isClickable = true
    }

    private var _openMenu: (() -> Unit)? = null

    public actual fun opensMenu(createMenu: Frame.() -> Unit) {
        val openFn = {
            var willRemove: Element? = null
            popoverWriter(context.overlayFrame!!) {
                val r = willRemove
                willRemove = null
                r?.let { context.overlayFrame!!.removeChild(it) }

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
                    atTopStart.themed(PopoverSemantic).frame {
                        this@dismissBackground.native.apply {
                            clipChildren = false
                            clipToPadding = false
                        }
                        this@dismissBackground.native.addOnLayoutChangeListener { dismissBackground, _, _, _, _, _, _, _, _ ->
                            val overlayContainer = this@frame.native
                            val anchor = this@MenuButton.native

                            val overlayBoundsInWindow = overlayContainer.getBoundariesInWindow()
                            val offset = this@MenuButton.preferredDirection.calculatePopoverOffset(
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
