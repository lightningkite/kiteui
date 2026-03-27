package com.lightningkite.kiteui.views.direct

import android.widget.FrameLayout
import com.lightningkite.kiteui.OverrideOnly
import com.lightningkite.kiteui.locale.renderToString
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.reactive.context.reactive
import com.lightningkite.reactive.core.*
import kotlinx.datetime.*
import kotlin.time.Clock

actual class LocalTimeField actual constructor(context: ElementContext) : NativeElementWithAction(context) {
    override val driverValue: String? get() = localTimeDriverValue()
    override val driverActions get() = super.driverActions + localTimeDriverActions()

    private val property: Signal<LocalTime?> = Signal(null)
    actual val content: MutableReactiveValue<LocalTime?> = property

    actual var range: ClosedRange<LocalTime>? = null

    override val native = FrameLayout(context.activity).apply {
        isClickable = true
        setOnClickListener {
            showTimePicker(
                property.value ?: Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).time,
                range?.start,
                range?.endInclusive
            ) { time ->
                property.value = time
                action?.startAction(this@LocalTimeField)
            }
        }
    }

    @OptIn(OverrideOnly::class)
    override fun onStartup() {
        super.onStartup()
        val text = android.widget.TextView(context.activity)
        native.addView(text)
        reactive {
            text.text = property()?.renderToString() ?: "Select"
        }
    }

    override fun nativeApplyTheme(theme: ThemeAndBack) = applyThemeWithRipple(theme)
}