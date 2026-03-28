package com.lightningkite.kiteui.views.direct

import android.R
import android.content.res.ColorStateList
import android.text.Editable
import android.text.TextWatcher
import android.widget.CheckBox as AndroidCheckBox
import android.widget.CompoundButton
import android.widget.CompoundButton.OnCheckedChangeListener
import android.widget.ProgressBar
import androidx.core.widget.CompoundButtonCompat
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*


actual class Checkbox actual constructor(context: RContext): RView(context) {
    override val driverValue: String? get() = checkboxDriverValue()
    override val driverActions get() = super.driverActions + checkboxDriverActions()
    override val native = AndroidCheckBox(context.activity)

    init {
        themeChoice += ClickableSemantic
    }

    override fun applyTheme(theme: ThemeAndBack) {
        val selectedTheme = theme[SelectedSemantic]
        val unselectedTheme = theme[UnselectedSemantic]
        val checkedShadows = selectedTheme.theme.shadows ?: emptyList()
        val uncheckedShadows = unselectedTheme.theme.shadows ?: emptyList()

        if (checkedShadows.isNotEmpty() || uncheckedShadows.isNotEmpty()) {
            // Neumorphic mode - use custom drawable for the checkbox indicator
            val density = native.resources.displayMetrics.density
            val controlSize = (24 * density).toInt()
            val cornerRadius = controlSize * 0.2f
            val drawable = (native.buttonDrawable as? NeumorphicControlDrawable)
                ?: NeumorphicControlDrawable(
                    controlSize,
                    isCircle = false,
                    drawCheckmark = true,
                    drawDot = false
                ).also {
                    native.buttonDrawable = it
                }
            drawable.update(
                checkedShadows = checkedShadows,
                uncheckedShadows = uncheckedShadows,
                checkedBgColor = selectedTheme.theme.background.colorInt(),
                uncheckedBgColor = unselectedTheme.theme.background.colorInt(),
                indicatorColor = theme.theme.foreground.colorInt(),
                cornerRadiusPx = cornerRadius,
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
