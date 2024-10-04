package com.lightningkite.kiteui.views.direct

import android.widget.FrameLayout
import androidx.core.view.doOnLayout
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.utils.getBoundariesInWindow
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.l2.overlayStack

actual class MenuButton actual constructor(context: RContext): RView(context) {
    override val native = FrameLayout(context.activity).apply {
        isClickable = true
    }

    actual fun opensMenu(createMenu: Stack.() -> Unit) {
        native.setOnClickListener { view ->
            var willRemove: RView? = null
            this.overlayStack!!.popoverWriter {
                willRemove?.let { overlayStack!!.removeChild(it) }
            }.run {
                willRemove = dismissBackground {
                    themeChoice += ThemeDerivation {
                        it.copy(background = Color.transparent).withBack
                    }
                    onClick {
                        closePopovers()
                    }
                    atTopStart - card - stack {
                        themeChoice += ThemeDerivation {
                            it.copy(elevation = 5.dp, revert = true).withBack
                        }
                        this@dismissBackground.native.doOnLayout { dismissBackground ->
                            val overlayContainer = this@stack.native
                            val anchor = this@MenuButton.native

                            val parentBoundsInWindow = dismissBackground.getBoundariesInWindow()
                            val offset = preferredDirection.calculatePopoverPosition(
                                anchor.getBoundariesInWindow(),
                                overlayContainer.getBoundariesInWindow(),
                                parentBoundsInWindow
                            )

                            overlayContainer.offsetLeftAndRight((offset.first - parentBoundsInWindow.left).toInt())
                            overlayContainer.offsetTopAndBottom((offset.second - parentBoundsInWindow.top).toInt())
                        }
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

    override fun hasAlternateBackedStates(): Boolean = true
    override fun applyState(theme: ThemeAndBack): ThemeAndBack {
        var t = theme
        if(!enabled) t = t[DisabledSemantic]
        return super.applyState(t)
    }

    override fun applyBackground(theme: Theme, fullyApply: Boolean) = applyBackgroundWithRipple(theme, fullyApply)
}
