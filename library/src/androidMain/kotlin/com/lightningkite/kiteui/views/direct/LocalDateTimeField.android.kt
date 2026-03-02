package com.lightningkite.kiteui.views.direct

import android.widget.FrameLayout
import com.lightningkite.kiteui.locale.renderToString
import com.lightningkite.kiteui.models.ClickableSemantic
import com.lightningkite.kiteui.models.DisabledSemantic
import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.models.ThemeAndBack
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*
import kotlinx.datetime.*
import kotlin.time.Clock
import kotlin.time.Instant

actual class LocalDateTimeField actual constructor(context: RContext) :
    RViewWithAction(context) {
    private val property: Signal<LocalDateTime?> = Signal(null)
    actual val content: MutableReactiveValue<LocalDateTime?> = property
    
    actual var range: ClosedRange<LocalDateTime>? = null

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

    // by Claude
    override var accessibilityValue: String?
        get() = readNullableValue(content)
        set(value) = writeNullableValue(content, value) { LocalDateTime.parse(it) }
    override val accessibilityActions get() = CLICK_AND_SET_VALUE_ACTIONS
    override fun performAccessibilityAction(action: String, value: String?) =
        performNullableSetValueAction(content, { LocalDateTime.parse(it) }, action, value) { a, v -> super.performAccessibilityAction(a, v) }
}