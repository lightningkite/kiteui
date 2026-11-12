package com.lightningkite.kiteui.views.direct

import android.widget.FrameLayout
import com.lightningkite.kiteui.ExperimentalKiteUi
import com.lightningkite.kiteui.OverrideOnly
import com.lightningkite.kiteui.locale.renderToString
import com.lightningkite.kiteui.models.ClickableSemantic
import com.lightningkite.kiteui.models.DisabledSemantic
import com.lightningkite.kiteui.models.ThemeAndBack
import com.lightningkite.kiteui.models.ThemeDerivation
import com.lightningkite.kiteui.views.*
import com.lightningkite.reactive.context.reactive
import com.lightningkite.reactive.core.*
import kotlinx.datetime.*
import kotlin.time.Clock
import com.lightningkite.kiteui.views.AiDriver

@OptIn(ExperimentalKiteUi::class)
public actual class LocalDateField actual constructor(context: ElementContext) : NativeElementWithAction(context) {
    override val driverValue: String? get() = localDateDriverValue()
    override val driverActions: AiDriver.Actions get() = super.driverActions + localDateDriverActions()
    private val property: Signal<LocalDate?> = Signal(null)
    public actual val content: MutableReactiveValue<LocalDate?> = property
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

    private val textView = android.widget.TextView(context.activity)

    @OverrideOnly
    override fun onStartup() {
        super.onStartup()
        native.addView(
            textView.apply {
                reactive {
                    text = property()?.renderToString() ?: "Select"
                }
            }
        )
    }

    override fun nativeApplyTheme(theme: ThemeAndBack) {
        super.applyThemeWithRipple(theme)
        val theme = theme.theme
        textView.setTextColor(theme.foreground.colorInt())
        textView.setTypeface(theme.font.typeface(context.activity))
        textView.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, theme.font.size.value)
    }
}