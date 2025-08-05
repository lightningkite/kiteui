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

public actual class LocalDateTimeField public actual constructor(context: RContext) :
    RViewWithAction(context) {
    private val property: Signal<LocalDateTime?> = Signal(null)
    public actual val content: MutableReactiveValue<LocalDateTime?> = property
    
    public actual var range: ClosedRange<LocalDateTime>? = null

    override val native: FrameLayout = FrameLayout(context.activity).apply {
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