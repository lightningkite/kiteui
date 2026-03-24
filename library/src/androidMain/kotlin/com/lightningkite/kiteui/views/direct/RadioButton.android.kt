package com.lightningkite.kiteui.views.direct

import android.R
import android.content.res.ColorStateList
import android.widget.CheckBox
import android.widget.RadioButton
import androidx.core.widget.CompoundButtonCompat
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*

actual class RadioButton actual constructor(context: RContext): RView(context) {
    override val driverValue: String? get() = radioDriverValue()
    override val driverActions get() = super.driverActions + radioDriverActions()
    override val native = android.widget.RadioButton(context.activity)

    init {
        themeChoice += ClickableSemantic
    }

    override fun applyTheme(theme: ThemeAndBack) {
        val selectedTheme = theme[SelectedSemantic]
        val unselectedTheme = theme[UnselectedSemantic]
        val checkedShadows = selectedTheme.theme.shadows ?: emptyList()
        val uncheckedShadows = unselectedTheme.theme.shadows ?: emptyList()

        if (checkedShadows.isNotEmpty() || uncheckedShadows.isNotEmpty()) {
            // Neumorphic mode - use custom drawable for the radio indicator
            val density = native.resources.displayMetrics.density
            val controlSize = (24 * density).toInt()
            val drawable = (native.buttonDrawable as? NeumorphicControlDrawable)
                ?: NeumorphicControlDrawable(controlSize, isCircle = true, drawCheckmark = false, drawDot = true).also {
                    native.buttonDrawable = it
                }
            drawable.update(
                checkedShadows = checkedShadows,
                uncheckedShadows = uncheckedShadows,
                checkedBgColor = selectedTheme.theme.background.colorInt(),
                uncheckedBgColor = unselectedTheme.theme.background.colorInt(),
                indicatorColor = theme.theme.foreground.colorInt(),
                cornerRadiusPx = controlSize / 2f,
            )
        } else {
            // Standard mode - use tinting
            val t = theme.theme
            CompoundButtonCompat.setButtonTintList(
                native, ColorStateList(
                    arrayOf<IntArray>(intArrayOf(-R.attr.state_checked), intArrayOf(R.attr.state_checked)), intArrayOf(
                        t.let { it.iconOverride ?: it.foreground }.closestColor().copy(alpha = 0.75f).colorInt(),
                        t.let { it.iconOverride ?: it.foreground }.colorInt()
                    )
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
