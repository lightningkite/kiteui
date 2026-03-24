package com.lightningkite.kiteui.views.direct

import android.R
import android.content.res.ColorStateList
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.StateListDrawable
import android.widget.CheckBox
import androidx.core.widget.CompoundButtonCompat
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*

actual class Switch actual constructor(context: RContext): RView(context) {
    override val driverValue: String? get() = switchDriverValue()
    override val driverActions get() = super.driverActions + switchDriverActions()
    override val native = android.widget.Switch(context.activity)

    init {
        themeChoice += ClickableSemantic
    }

    override fun applyTheme(theme: ThemeAndBack) {
        val selectedTheme = theme[SelectedSemantic]
        val unselectedTheme = theme[UnselectedSemantic]
        val checkedShadows = selectedTheme.theme.shadows ?: emptyList()
        val uncheckedShadows = unselectedTheme.theme.shadows ?: emptyList()

        if (checkedShadows.isNotEmpty() || uncheckedShadows.isNotEmpty()) {
            // Neumorphic mode - create custom track drawable
            val density = native.resources.displayMetrics.density
            val trackHeight = (24 * density).toInt()
            val trackWidth = (48 * density).toInt()

            // Track uses a neumorphic control drawable (pill shape, 2:1 ratio like web's 3rem x 1.5rem)
            val track = (native.trackDrawable as? NeumorphicControlDrawable)
                ?: NeumorphicControlDrawable(
                    trackWidth,
                    trackHeight,
                    isCircle = false,
                    drawCheckmark = false,
                    drawDot = false
                ).also {
                    native.trackDrawable = it
                }
            track.update(
                checkedShadows = checkedShadows,
                uncheckedShadows = uncheckedShadows,
                checkedBgColor = selectedTheme.theme.background.colorInt(),
                uncheckedBgColor = unselectedTheme.theme.background.colorInt(),
                indicatorColor = theme.theme.foreground.colorInt(),
                cornerRadiusPx = trackHeight / 2f,
            )

            // Thumb colors - use background highlight for unchecked, accent for checked
            val t = theme.theme
            native.thumbTintList = ColorStateList(
                arrayOf<IntArray>(intArrayOf(-R.attr.state_checked), intArrayOf(R.attr.state_checked)), intArrayOf(
                    t.background.closestColor().highlight(.3f).colorInt(),
                    t[ImportantSemantic].theme.background.colorInt()
                )
            )
            native.trackTintList = null
        } else {
            // Standard mode
            val t = theme.theme
            native.thumbTintList = ColorStateList(
                arrayOf<IntArray>(intArrayOf(-R.attr.state_checked), intArrayOf(R.attr.state_checked)), intArrayOf(
                    t.background.closestColor().highlight(.3f).colorInt(),
                    t[ImportantSemantic].theme.background.colorInt()
                )
            )
            native.trackTintList = ColorStateList(
                arrayOf<IntArray>(intArrayOf(-R.attr.state_checked), intArrayOf(R.attr.state_checked)), intArrayOf(
                    t.background.closestColor().highlight(.2f).colorInt(),
                    t.background.closestColor().highlight(.2f).colorInt(),
                )
            )
        }
    }

    actual var enabled: Boolean
        get() = native.isEnabled
        set(value) {
            native.isEnabled = value
            refreshTheming()
        }

    override fun applyState(theme: ThemeAndBack): ThemeAndBack {
        var t = theme
        if(!enabled) t = t[DisabledSemantic]
        return super.applyState(t)
    }

    actual val checked: MutableReactiveValue<Boolean> = native.contentProperty()
}
