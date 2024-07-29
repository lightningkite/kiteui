package com.lightningkite.kiteui.views.direct

import android.content.res.ColorStateList
import android.widget.FrameLayout
import com.lightningkite.kiteui.launchManualCancel
import com.lightningkite.kiteui.locale.renderToString
import com.lightningkite.kiteui.models.Action
import com.lightningkite.kiteui.models.DisabledSemantic
import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.models.ThemeAndBack
import com.lightningkite.kiteui.reactive.ImmediateWritable
import com.lightningkite.kiteui.reactive.Property
import com.lightningkite.kiteui.reactive.Writable
import com.lightningkite.kiteui.reactive.invoke
import com.lightningkite.kiteui.views.*
import kotlinx.datetime.*

actual class LocalDateField actual constructor(context: RContext) :
    RView(context) {
    private val property: Property<LocalDate?> = Property(null)
    actual val content: ImmediateWritable<LocalDate?> = property
    actual var action: Action? = null
    actual var range: ClosedRange<LocalDate>? = null

    override val native = FrameLayout(context.activity).apply {
        isClickable = true
        setOnClickListener {
            showDatePicker(
                property.value ?: Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date,
                range?.start,
                range?.endInclusive
            ) { date ->
                property.value = date
//                showTimePicker(
//                    prop.value?.time ?: Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).time,
//                    range?.start,
//                    range?.endInclusive
//                ) { time ->
//                    property.value = date.atTime(time)
//                }
            }
        }
    }

    override fun postSetup() {
        super.postSetup()
        text {
            ::content { property()?.renderToString() ?: "Select" }
        }
    }

    var enabled: Boolean
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

    override fun hasAlternateBackedStates(): Boolean = true
    override fun applyBackground(theme: Theme, fullyApply: Boolean) = applyBackgroundWithRipple(theme, fullyApply)

}