package com.lightningkite.kiteui.views.direct

import android.widget.FrameLayout
import com.lightningkite.kiteui.OverrideOnly
import com.lightningkite.kiteui.locale.renderToString
import com.lightningkite.kiteui.models.ClickableSemantic
import com.lightningkite.kiteui.models.DisabledSemantic
import com.lightningkite.kiteui.models.ThemeAndBack
import com.lightningkite.kiteui.views.*
import com.lightningkite.reactive.context.reactive
import com.lightningkite.reactive.core.*
import kotlinx.datetime.*
import kotlin.time.Clock

public actual class LocalDateTimeField actual constructor(context: ElementContext) : NativeElementWithAction(context) {
    override val driverValue: String? get() = localDateTimeDriverValue()
    override val driverActions get() = super.driverActions + localDateTimeDriverActions()
    private val property: Signal<LocalDateTime?> = Signal(null)
    public actual val content: MutableReactiveValue<LocalDateTime?> = property
    
    public actual var range: ClosedRange<LocalDateTime>? = null

    override val native = FrameLayout(context.activity).apply {
        isClickable = true
        setOnClickListener {
            showDatePicker(
                property.value?.date ?: Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date,
                range?.start?.date,
                range?.endInclusive?.date
            ) { date ->
                showTimePicker(
                    property.value?.time ?: Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).time,
                    range?.start?.time,
                    range?.endInclusive?.time
                ) { time ->
                    property.value = date.atTime(time)
                    action?.startAction(this@LocalDateTimeField)
                }
            }
        }
    }

    @OverrideOnly
    override fun onStartup() {
        super.onStartup()
        native.addView(
            android.widget.TextView(context.activity).apply {
                reactive {
                    text = property()?.renderToString() ?: "Select"
                }
            }
        )
    }

    override fun nativeApplyTheme(theme: ThemeAndBack) = super.applyThemeWithRipple(theme)
}