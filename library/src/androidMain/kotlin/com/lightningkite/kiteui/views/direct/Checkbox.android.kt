package com.lightningkite.kiteui.views.direct

import android.R
import android.content.res.ColorStateList
import android.text.Editable
import android.text.TextWatcher
import android.widget.CompoundButton
import android.widget.CompoundButton.OnCheckedChangeListener
import android.widget.ProgressBar
import androidx.core.widget.CompoundButtonCompat
import com.lightningkite.kiteui.models.DisabledSemantic
import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.models.ThemeAndBack
import com.lightningkite.readable.ImmediateWritable
import com.lightningkite.readable.ReadableState
import android.widget.CheckBox as AndroidCheckBox
import com.lightningkite.readable.Writable
import com.lightningkite.readable.await
import com.lightningkite.kiteui.views.*


actual class Checkbox actual constructor(context: RContext): RView(context), InputAccessibility {
    override val native = AndroidCheckBox(context.activity)
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

    actual val checked: ImmediateWritable<Boolean> = native.contentProperty()

    override var ariaRequired: Boolean? = null
        set(value) {
            field = value
            // Android doesn't have a direct equivalent to aria-required
            // We could update the contentDescription to include "required" if needed
            if (value == true && native.contentDescription != null) {
                val currentDesc = native.contentDescription.toString()
                if (!currentDesc.contains("(required)")) {
                    native.contentDescription = "$currentDesc (required)"
                }
            }
        }
}
