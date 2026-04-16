package com.lightningkite.kiteui.views.direct

import android.R
import android.content.res.ColorStateList
import androidx.core.view.ViewCompat
import androidx.core.widget.CompoundButtonCompat
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.reactive.core.*

actual class RadioButton actual constructor(context: ElementContext): NativeInteractiveElement(context) {
    actual override val underlyingNativeElement: RadioButton get() = this

    override val driverValue: String? get() = radioDriverValue()
    override val driverActions get() = super.driverActions + radioDriverActions()
    override val native = android.widget.RadioButton(context.activity)

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

    actual val checked: MutableReactiveValue<Boolean> = native.contentProperty()

    init {
        checked.addListener {
            ViewCompat.setStateDescription(native, if (checked.state.let { if (it.ready) it.raw else false }) "Selected" else "Not selected")
        }
    }
}
