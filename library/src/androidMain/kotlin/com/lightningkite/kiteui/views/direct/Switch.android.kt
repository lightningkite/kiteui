package com.lightningkite.kiteui.views.direct

import android.R
import android.content.res.ColorStateList
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.view.ViewCompat
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.reactive.core.*

public actual class Switch actual constructor(context: ElementContext): NativeInteractiveElement(context) {
    override val driverValue: String? get() = switchDriverValue()
    override val driverActions: Map<String, suspend (List<String>) -> String> get() = super.driverActions + switchDriverActions()
    override val native: android.widget.Switch = android.widget.Switch(context.activity)

    @RequiresApi(Build.VERSION_CODES.M)
    override fun nativeApplyTheme(theme: ThemeAndBack) {
        super.nativeApplyTheme(theme)
        val theme = theme.theme
        native.thumbTintList = ColorStateList(
            arrayOf<IntArray>(intArrayOf(-R.attr.state_checked), intArrayOf(R.attr.state_checked)), intArrayOf(
                theme.background.closestColor().highlight(.3f).colorInt(),
                theme[ImportantSemantic].theme.background.colorInt()
            )
        )
        native.trackTintList = ColorStateList(
            arrayOf<IntArray>(intArrayOf(-R.attr.state_checked), intArrayOf(R.attr.state_checked)), intArrayOf(
                theme.background.closestColor().highlight(.2f).colorInt(),
                theme.background.closestColor().highlight(.2f).colorInt(),
            )
        )
    }

    public actual val checked: MutableReactiveValue<Boolean> = native.contentProperty()

    init {
        checked.addListener {
            ViewCompat.setStateDescription(native, if (checked.state.let { if (it.ready) it.raw else false }) "On" else "Off")
        }
    }
}
