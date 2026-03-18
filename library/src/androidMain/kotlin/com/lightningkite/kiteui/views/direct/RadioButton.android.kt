package com.lightningkite.kiteui.views.direct

import android.R
import android.content.res.ColorStateList
import androidx.core.widget.CompoundButtonCompat
import com.lightningkite.kiteui.models.DisabledSemantic
import com.lightningkite.kiteui.models.ThemeAndBack
import com.lightningkite.kiteui.views.*
import com.lightningkite.reactive.core.*

actual class RadioButton actual constructor(context: ElementContext): RView(context) {
    override val driverValue: String? get() = radioDriverValue()
    override val driverActions get() = super.driverActions + radioDriverActions()
    override val native = android.widget.RadioButton(context.activity)
    override fun applyTheme(theme: ThemeAndBack) {
        val theme = theme.theme
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

    actual val checked: MutableReactiveValue<Boolean> = native.contentProperty()

}
