package com.lightningkite.kiteui.views.direct

import android.widget.FrameLayout
import com.lightningkite.kiteui.locale.renderToString
import com.lightningkite.kiteui.models.ClickableSemantic
import com.lightningkite.kiteui.models.DisabledSemantic
import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.models.ThemeAndBack
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RViewWithAction
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*
import kotlinx.datetime.*

public actual class LocalTimeField public actual constructor(context: RContext) :
    RViewWithAction(context) {
    private val property: Signal<LocalTime?> = Signal(null)
    actual val content: MutableReactiveValue<LocalTime?> = property
    
    public actual var range: ClosedRange<LocalTime>? = null

    override val native: FrameLayout = FrameLayout(context.activity).apply {
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