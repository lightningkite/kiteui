package com.lightningkite.kiteui.views.direct

import android.R
import android.content.res.ColorStateList
import android.os.Build
import androidx.annotation.RequiresApi
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.reactive.core.*

actual class Switch actual constructor(context: ElementContext): NativeElement(context) {
    override val driverValue: String? get() = switchDriverValue()
    override val driverActions get() = super.driverActions + switchDriverActions()
    override val native = android.widget.Switch(context.activity)

    init {
        elementSpecificTheming += ElementSpecificTheming {
            var t: ThemeDerivation = ThemeDerivation.None
            if (!enabled) t += DisabledSemantic
            t
        }
    }

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
//        native.thumbTintList = null
//        native.trackTintList = null
//        native.thumbDrawable = drawableWithoutCorners(
//            fill = theme.foreground,
//            stroke = Color.interpolate(
//                theme.background.closestColor(),
//                theme.foreground.closestColor(),
//                0.5f,
//            ),
//            strokeWidth = theme.outlineWidth,
//        ).apply {
//            cornerRadius = 999f
//            setSize(24.dp.px.toInt(), 24.dp.px.toInt())
//        }
//        native.trackDrawable = StateListDrawable().apply {
//            addState(intArrayOf(-R.attr.state_checked), drawableWithoutCorners(
//                fill = Color.interpolate(
//                    theme[CardSemantic].theme.background.closestColor(),
//                    theme[CardSemantic].theme.foreground.closestColor(),
//                    0.5f,
//                ),
//                stroke = Color.interpolate(
//                    theme[CardSemantic].theme.background.closestColor(),
//                    theme[CardSemantic].theme.outline.closestColor(),
//                    0.5f,
//                ),
//                strokeWidth = theme[CardSemantic].theme.outlineWidth,
//            ).apply {
//                cornerRadius = 999f
//                setSize(24.dp.px.toInt(), 24.dp.px.toInt())
//            })
//            addState(intArrayOf(R.attr.state_checked), drawableWithoutCorners(
//                fill = theme[AffirmativeSemantic].theme.background,
//                stroke = Color.interpolate(
//                    theme.background.closestColor(),
//                    theme[AffirmativeSemantic].theme.background.closestColor(),
//                    0.5f,
//                ),
//                strokeWidth = theme.outlineWidth,
//            ).apply {
//                cornerRadius = 999f
//                setSize(24.dp.px.toInt(), 24.dp.px.toInt())
//            })
//        }
    }
    actual var enabled: Boolean
        get() = native.isEnabled
        set(value) {
            native.isEnabled = value
            refreshTheming()
        }

    actual val checked: MutableReactiveValue<Boolean> = native.contentProperty()
}
