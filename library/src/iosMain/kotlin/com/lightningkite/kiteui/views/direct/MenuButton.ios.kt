package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.l2.overlayFrame
import com.lightningkite.kiteui.views.themed
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.Reactive
import platform.UIKit.UIAccessibilityTraitButton
import platform.UIKit.UIControl
import platform.UIKit.accessibilityHint
import platform.UIKit.accessibilityTraits
import platform.UIKit.setAccessibilityHint
import platform.UIKit.setAccessibilityTraits

public actual class MenuButton actual constructor(context: ElementContext): NativeInteractiveContainerElement(context) {
    private var _openMenu: (() -> Unit)? = null
    override val driverActions get() = super.driverActions + menuDriverActions()
    override val native = FrameLayoutButton()
    override val control: UIControl get() = native
    init {
        native.accessibilityTraits = native.accessibilityTraits or UIAccessibilityTraitButton
        native.accessibilityHint = "Opens menu"
        setupControl()
    }

    public actual fun opensMenu(createMenu: Frame.() -> Unit) {
        val openFn: () -> Unit = {
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
                    native.anchor = this@MenuButton.preferredDirection to this@MenuButton.native
                    onClick {
                        closePopovers()
                    }
                    themed(PopoverSemantic).frame {
                        createMenu()
                    }
                }
            }
        }
        onRemove(native.setOnClick(openFn))
        _openMenu = openFn
    }

    public actual var requireClick: Boolean = true
    public actual var preferredDirection: PopoverPreferredDirection = PopoverPreferredDirection.belowLeft
}
