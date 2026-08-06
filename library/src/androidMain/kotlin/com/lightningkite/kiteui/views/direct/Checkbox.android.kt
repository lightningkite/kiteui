package com.lightningkite.kiteui.views.direct

import android.R
import android.content.res.ColorStateList
import androidx.core.view.ViewCompat
import androidx.core.widget.CompoundButtonCompat
import com.lightningkite.kiteui.models.ThemeAndBack
import com.lightningkite.kiteui.views.AiDriver
import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.NativeInteractiveElement
import com.lightningkite.reactive.core.MutableReactiveValue
import android.widget.CheckBox as AndroidCheckBox
import com.lightningkite.kiteui.views.AiDriver


public actual class Checkbox actual constructor(context: ElementContext): NativeInteractiveElement(context) {
    actual override val underlyingNativeElement: Checkbox get() = this

    override val driverValue: String? get() = checkboxDriverValue()
    override val driverActions: AiDriver.Actions get() = super.driverActions + checkboxDriverActions()
    override val native: AndroidCheckBox = AndroidCheckBox(context.activity)

    override fun nativeApplyTheme(theme: ThemeAndBack) {
        super.nativeApplyTheme(theme)
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

    public actual val checked: MutableReactiveValue<Boolean> = native.contentProperty()

    init {
        checked.addListener {
            ViewCompat.setStateDescription(native, if (checked.state.let { if (it.ready) it.raw else false }) "Checked" else "Not checked")
        }
    }
}
