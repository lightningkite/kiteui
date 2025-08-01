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
import com.lightningkite.signal.ImmediateWritable
import com.lightningkite.signal.ReadableState
import android.widget.CheckBox as AndroidCheckBox
import com.lightningkite.signal.Writable
import com.lightningkite.signal.await
import com.lightningkite.kiteui.views.*


public actual class Checkbox public actual constructor(context: RContext): RView(context) {
    override val native: AndroidCheckBox = AndroidCheckBox(context.activity)
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
    public actual var enabled: Boolean
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

    public actual val checked: ImmediateWritable<Boolean> = native.contentProperty()
}
