package com.lightningkite.kiteui.views.direct

import android.widget.FrameLayout
import com.lightningkite.kiteui.locale.renderToString
import com.lightningkite.kiteui.models.ClickableSemantic
import com.lightningkite.kiteui.models.DisabledSemantic
import com.lightningkite.kiteui.models.ThemeAndBack
import com.lightningkite.kiteui.views.*
import com.lightningkite.reactive.core.*
import kotlinx.datetime.*
import kotlin.time.Clock

actual class LocalDateField actual constructor(context: ElementContext) :
    RViewWithAction(context) {
    override val driverValue: String? get() = localDateDriverValue()
    override val driverActions get() = super.driverActions + localDateDriverActions()
    private val property: Signal<LocalDate?> = Signal(null)
    actual val content: MutableReactiveValue<LocalDate?> = property
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
                action?.startAction(this@LocalDateField)
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
        var t = theme[ClickableSemantic]
        if(!enabled) t = t[DisabledSemantic]
        return super.applyState(t)
    }

    override fun applyTheme(theme: ThemeAndBack) = super.applyThemeWithRipple(theme)
}