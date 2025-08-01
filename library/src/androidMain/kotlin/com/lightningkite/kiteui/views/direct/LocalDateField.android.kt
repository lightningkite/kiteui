package com.lightningkite.kiteui.views.direct

import android.widget.FrameLayout
import com.lightningkite.kiteui.locale.renderToString
import com.lightningkite.kiteui.models.ClickableSemantic
import com.lightningkite.kiteui.models.DisabledSemantic
import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.models.ThemeAndBack
import com.lightningkite.signal.ImmediateWritable
import com.lightningkite.signal.Property
import com.lightningkite.signal.invoke
import com.lightningkite.kiteui.views.*
import kotlinx.datetime.*

public actual class LocalDateField public actual constructor(context: RContext) :
    RViewWithAction(context) {
    private val property: Property<LocalDate?> = Property(null)
    public actual val content: ImmediateWritable<LocalDate?> = property
    public actual var range: ClosedRange<LocalDate>? = null

    override val native: FrameLayout = FrameLayout(context.activity).apply {
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

    public var enabled: Boolean
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

    override fun applyTheme(theme: ThemeAndBack): Unit = super.applyThemeWithRipple(theme)
}