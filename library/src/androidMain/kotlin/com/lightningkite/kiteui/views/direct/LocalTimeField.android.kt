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
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RViewWithAction
import kotlinx.datetime.*

actual class LocalTimeField actual constructor(context: RContext) :
    RViewWithAction(context) {
    private val property: Property<LocalTime?> = Property(null)
    actual val content: ImmediateWritable<LocalTime?> = property
    
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