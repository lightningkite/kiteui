package com.lightningkite.kiteui.views.direct

import android.R
import android.content.res.ColorStateList
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.StateListDrawable
import android.widget.CheckBox
import androidx.core.widget.CompoundButtonCompat
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.reactive.ImmediateWritable
import com.lightningkite.kiteui.reactive.Writable
import com.lightningkite.kiteui.views.*

actual class Switch actual constructor(context: RContext): RView(context) {
    override val native = android.widget.Switch(context.activity)
    override fun applyForeground(theme: Theme) {
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

    override fun applyState(theme: ThemeAndBack): ThemeAndBack {
        var t = theme
        if(!enabled) t = t[DisabledSemantic]
        return t
    }

    actual val checked: ImmediateWritable<Boolean> = native.contentProperty()

    override fun applyBackground(theme: Theme, fullyApply: Boolean) {
        // Never apply a background
    }
}
