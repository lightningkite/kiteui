package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.Dimension
import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.models.ThemeAndBack
import com.lightningkite.kiteui.reactive.AppState
import com.lightningkite.readable.invoke
import com.lightningkite.readable.reactiveScope
import com.lightningkite.kiteui.views.*

import platform.UIKit.UIView
import kotlin.math.absoluteValue


actual class RowOrCol actual constructor(context: RContext) : RView(context) {
    override val native = LinearLayout()
    override val cannotBeCovered: Boolean get() = false

    actual var vertical: Boolean
        get() = native.horizontal.not()
        set(value) {
            native.horizontal = !value
        }

    actual fun spacingOverrideBeforeNext(amount: Dimension): Unit {
        beforeNextElementSetup {
            native.extensionSpacingBeforeOverride = amount
        }
    }

    override var spacing: Dimension?
        get() = super.spacing
        set(value) {
            super.spacing = value
            native.gap = (value ?: theme.spacing).value
        }

    override fun applyTheme(theme: ThemeAndBack) {
        super.applyTheme(theme);
        native.gap = (spacing ?: theme.theme.spacing).value
    }
}

actual class RowCollapsingToColumn actual constructor(context: RContext, breakpoints: List<Dimension>) :
    RView(context) {
    override val cannotBeCovered: Boolean get() = false
    override val native = LinearLayout()

    init {
        reactiveScope {
            val w = AppState.windowInfo().width
            val index = breakpoints.indexOfFirst { w > it }
            if (index == -1 || index % 2 == 1) {
                native.horizontal = false
                native.ignoreWeights = true
            } else {
                native.horizontal = true
                native.ignoreWeights = false
            }
        }
    }

    override var spacing: Dimension?
        get() = super.spacing
        set(value) {
            super.spacing = value
            native.gap = (value ?: theme.spacing).value
        }

    override fun applyTheme(theme: ThemeAndBack) {
        super.applyTheme(theme);
        native.gap = (spacing ?: theme.theme.spacing).value
    }
}

actual class Frame actual constructor(context: RContext) : RView(context) {
    override val cannotBeCovered: Boolean get() = false
    override val native = FrameLayout()
}
