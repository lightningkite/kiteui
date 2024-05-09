package com.lightningkite.kiteui.views.direct

import android.R
import android.content.res.ColorStateList
import android.text.Editable
import android.text.TextWatcher
import android.widget.CompoundButton
import android.widget.CompoundButton.OnCheckedChangeListener
import android.widget.ProgressBar
import androidx.core.widget.CompoundButtonCompat
import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.reactive.ReadableState
import android.widget.CheckBox as AndroidCheckBox
import com.lightningkite.kiteui.reactive.Writable
import com.lightningkite.kiteui.reactive.await
import com.lightningkite.kiteui.views.*


actual class Checkbox actual constructor(context: RContext): RView(context) {
    override val native = AndroidCheckBox(context.activity)
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

    override fun beforeRefreshTheming() = when {
        !enabled -> ThemeChoice.Derive { it.disabled() }
        else -> null
    }

    actual val checked: Writable<Boolean> = native.contentProperty()

    override fun applyBackground(theme: Theme, fullyApply: Boolean) {
        // Never apply a background
    }
}
