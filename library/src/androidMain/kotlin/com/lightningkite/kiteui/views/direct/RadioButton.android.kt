package com.lightningkite.kiteui.views.direct

import android.R
import android.content.res.ColorStateList
import android.widget.CheckBox
import android.widget.RadioButton
import androidx.core.widget.CompoundButtonCompat
import com.lightningkite.kiteui.models.DisabledSemantic
import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.models.ThemeAndBack
import com.lightningkite.kiteui.reactive.ImmediateWritable
import com.lightningkite.kiteui.reactive.Writable
import com.lightningkite.kiteui.views.*

actual class RadioButton actual constructor(context: RContext): RView(context) {
    override val native = android.widget.RadioButton(context.activity)
    override fun applyForeground(theme: Theme) {
        CompoundButtonCompat.setButtonTintList(
            native, ColorStateList(
                arrayOf<IntArray>(intArrayOf(-R.attr.state_checked), intArrayOf(R.attr.state_checked)), intArrayOf(
                    theme.let { it.iconOverride ?: it.foreground }.closestColor().copy(alpha = 0.75f).colorInt(),
                    theme.let { it.iconOverride ?: it.foreground }.colorInt()
                )
            )
        )
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

    actual val checked: ImmediateWritable<Boolean> = native.contentProperty()

    override fun applyBackground(theme: Theme, fullyApply: Boolean) {
        // Never apply a background
    }
}
