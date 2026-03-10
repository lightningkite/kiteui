package com.lightningkite.kiteui.views.direct


import com.lightningkite.kiteui.locale.renderToString
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.reactive.Action
import com.lightningkite.kiteui.views.*
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import kotlinx.datetime.*
import platform.Foundation.NSCalendar
import platform.Foundation.NSDate
import platform.Foundation.NSDateComponents
import platform.Foundation.NSTimeZone
import platform.Foundation.localTimeZone
import platform.UIKit.*


actual class LocalDateField actual constructor(context: RContext) : RViewWithAction(context) {
    override val driverValue: String? get() = localDateDriverValue()
    override val driverActions get() = super.driverActions + localDateDriverActions()
    override val native = WrapperView()
    val textField = TextFieldInput(this)
    init { native.addSubview(textField) }

    private val _content = Signal<LocalDate?>(null)
    actual val content: MutableReactiveValue<LocalDate?> get() = _content

    actual var range: ClosedRange<LocalDate>? = null
        set(value) {
            field = value
            val picker = textField.inputView as? UIDatePicker ?: return
            value?.let { range ->
                val current = _content.value ?: range.start
                val clamped = when {
                    current < range.start -> range.start
                    current > range.endInclusive -> range.endInclusive
                    else -> current
                }
                _content.value = clamped
                picker.date = clamped.toNSDate()
            }

            picker.minimumDate = range?.start?.toNSDate()
            picker.maximumDate = range?.endInclusive?.toNSDate()
        }

    init {
        // TODO: need a way to CLEAR the field.
        textField.inputView = UIDatePicker().apply {
            setPreferredDatePickerStyle(UIDatePickerStyle.UIDatePickerStyleInline)
            datePickerMode = UIDatePickerMode.UIDatePickerModeDate
            date = _content.value?.toNSDateComponents()?.date() ?: NSDate()
            onEvent(this@LocalDateField, UIControlEventValueChanged) {
                _content.value = this.date.toKotlinInstant().toLocalDateTime(TimeZone.currentSystemDefault()).date
            }
        }
        reactiveScope {
            textField.text = _content.invoke()?.renderToString() ?: "-"
        }
    }

    var fontAndStyle: FontAndStyle? = null
        set(value) {
            field = value
            updateFont()
            native.informParentOfSizeChange()
        }
    override fun applyTheme(theme: ThemeAndBack) { super.applyTheme(theme); val theme = theme.theme
        textField.textColor = theme.foreground.closestColor().toUiColor()
        fontAndStyle = theme.font
    }
    fun updateFont() {
        val alignment = textField.textAlignment
        textField.font = fontAndStyle?.let {
            it.font.get(it.size.value * preferredScaleFactor(), it.weight.toUIFontWeight(), it.italic)
        } ?: UIFont.systemFontOfSize(16.0)
        textField.textAlignment = alignment
    }

    var enabled: Boolean
        get() = textField.enabled
        set(value) {
            textField.enabled = value
            refreshTheming()
        }
    init {
        onRemove(textField.observe("highlighted", { refreshTheming() }))
        onRemove(textField.observe("selected", { refreshTheming() }))
        onRemove(textField.observe("enabled", { refreshTheming() }))
    }
    override fun applyState(theme: ThemeAndBack): ThemeAndBack {
        var t = theme[ClickableSemantic]
        if(!enabled) t = t[DisabledSemantic]
        if(textField.highlighted) t = t[DownSemantic]
        if(textField.focused) t = t[FocusSemantic]
        return super.applyState(t)
    }
}

actual class LocalTimeField actual constructor(context: RContext) : RViewWithAction(context) {
    override val driverValue: String? get() = localTimeDriverValue()
    override val driverActions get() = super.driverActions + localTimeDriverActions()
    override val native = WrapperView()
    val textField = TextFieldInput(this)
    init { native.addSubview(textField) }

    private val _content = Signal<LocalTime?>(null)
    actual val content: MutableReactiveValue<LocalTime?> get() = _content
    override fun actionSet(value: Action?) {
        super.actionSet(value)
        textField.action = value
    }

    actual var range: ClosedRange<LocalTime>? = null
        set(value) {
            field = value
            val picker = textField.inputView as? UIDatePicker ?: return

            picker.minimumDate = value?.start?.toNSDate()
            picker.maximumDate = value?.endInclusive?.toNSDate()

            _content.value?.let { time ->
                if (value != null && time !in value) {
                    _content.value = value.start
                    picker.date = value.start.toNSDate()
                }
            }
        }




    init {
        textField.inputView = UIDatePicker().apply {
            setPreferredDatePickerStyle(UIDatePickerStyle.UIDatePickerStyleWheels)
            datePickerMode = UIDatePickerMode.UIDatePickerModeTime
            date = _content.value?.atDate(1970, 1, 1)?.toNSDateComponents()?.date() ?: NSDate()
            onEvent(this@LocalTimeField, UIControlEventValueChanged) {
                _content.value = this.date.toKotlinInstant().toLocalDateTime(TimeZone.currentSystemDefault()).time
            }
        }
        reactiveScope {
            textField.text = _content.invoke()?.renderToString() ?: "-"
        }
    }

    var fontAndStyle: FontAndStyle? = null
        set(value) {
            field = value
            updateFont()
            native.informParentOfSizeChange()
        }
    override fun applyTheme(theme: ThemeAndBack) { super.applyTheme(theme); val theme = theme.theme
        textField.textColor = theme.foreground.closestColor().toUiColor()
        fontAndStyle = theme.font
    }
    fun updateFont() {
        val alignment = textField.textAlignment
        textField.font = fontAndStyle?.let {
            it.font.get(it.size.value * preferredScaleFactor(), it.weight.toUIFontWeight(), it.italic)
        } ?: UIFont.systemFontOfSize(16.0)
        textField.textAlignment = alignment
    }

    var enabled: Boolean
        get() = textField.enabled
        set(value) {
            textField.enabled = value
            refreshTheming()
        }
    init {
        onRemove(textField.observe("highlighted", { refreshTheming() }))
        onRemove(textField.observe("selected", { refreshTheming() }))
        onRemove(textField.observe("enabled", { refreshTheming() }))
    }
    override fun applyState(theme: ThemeAndBack): ThemeAndBack {
        var t = theme[ClickableSemantic]
        if(!enabled) t = t[DisabledSemantic]
        if(textField.highlighted) t = t[DownSemantic]
        if(textField.focused) t = t[FocusSemantic]
        return super.applyState(t)
    }
}

actual class LocalDateTimeField actual constructor(context: RContext) : RViewWithAction(context) {
    override val driverValue: String? get() = localDateTimeDriverValue()
    override val driverActions get() = super.driverActions + localDateTimeDriverActions()
    override val native = WrapperView()
    val textField = TextFieldInput(this)
    init { native.addSubview(textField) }

    private val _content = Signal<LocalDateTime?>(null)
    actual val content: MutableReactiveValue<LocalDateTime?> get() = _content
    override fun actionSet(value: Action?) {
        super.actionSet(value)
        textField.action = value
    }
    actual var range: ClosedRange<LocalDateTime>? = null
        set(value) {
            field = value
            val picker = textField.inputView as? UIDatePicker ?: return

            picker.minimumDate = value?.start?.toNSDate()
            picker.maximumDate = value?.endInclusive?.toNSDate()

            _content.value?.let { dateTime ->
                if (value != null && dateTime !in value) {
                    _content.value = value.start
                    picker.date = value.start.toNSDateComponents().date()!!
                }
            }
        }


    init {
        textField.inputView = UIDatePicker().apply {
            setPreferredDatePickerStyle(UIDatePickerStyle.UIDatePickerStyleWheels)
            datePickerMode = UIDatePickerMode.UIDatePickerModeDateAndTime
            date = _content.value?.toNSDateComponents()?.date() ?: NSDate()
            onEvent(this@LocalDateTimeField, UIControlEventValueChanged) {
                _content.value = this.date.toKotlinInstant().toLocalDateTime(TimeZone.currentSystemDefault())
            }
        }
        reactiveScope {
            textField.text = _content.invoke()?.renderToString() ?: "-"
        }
    }

    var fontAndStyle: FontAndStyle? = null
        set(value) {
            field = value
            updateFont()
            native.informParentOfSizeChange()
        }
    override fun applyTheme(theme: ThemeAndBack) { super.applyTheme(theme); val theme = theme.theme
        textField.textColor = theme.foreground.closestColor().toUiColor()
        fontAndStyle = theme.font
    }
    fun updateFont() {
        val alignment = textField.textAlignment
        textField.font = fontAndStyle?.let {
            it.font.get(it.size.value * preferredScaleFactor(), it.weight.toUIFontWeight(), it.italic)
        } ?: UIFont.systemFontOfSize(16.0)
        textField.textAlignment = alignment
    }

    var enabled: Boolean
        get() = textField.enabled
        set(value) {
            textField.enabled = value
            refreshTheming()
        }
    init {
        onRemove(textField.observe("highlighted", { refreshTheming() }))
        onRemove(textField.observe("selected", { refreshTheming() }))
        onRemove(textField.observe("enabled", { refreshTheming() }))
    }
    override fun applyState(theme: ThemeAndBack): ThemeAndBack {
        var t = theme[ClickableSemantic]
        if(!enabled) t = t[DisabledSemantic]
        if(textField.highlighted) t = t[DownSemantic]
        if(textField.focused) t = t[FocusSemantic]
        return super.applyState(t)
    }
}


//NSDateComponents().date() depends on the calendar property being set.
//If you don’t specify one (like NSCalendar.currentCalendar()), then date() can return null — because the system doesn’t know which calendar/timezone to use to interpret the components.
private fun NSDateComponents.toNSDate(): NSDate {
    val calendar = NSCalendar.currentCalendar
    calendar.timeZone = NSTimeZone.localTimeZone
    return calendar.dateFromComponents(this) ?: NSDate()
}

private fun LocalDate.toNSDate(): NSDate {
    return NSDateComponents().apply {
        year = this@toNSDate.year.toLong()
        month = this@toNSDate.monthNumber.toLong()
        day = this@toNSDate.dayOfMonth.toLong()
    }.toNSDate()
}

private fun LocalTime.toNSDate(): NSDate {
    val referenceDate =  LocalDate(1970, 1, 1)
    return NSDateComponents().apply {
        year = referenceDate.year.toLong()
        month = referenceDate.monthNumber.toLong()
        day = referenceDate.dayOfMonth.toLong()
        hour = this@toNSDate.hour.toLong()
        minute = this@toNSDate.minute.toLong()
        second = this@toNSDate.second.toLong()
    }.toNSDate()
}

private fun LocalDateTime.toNSDate(): NSDate {
    return NSDateComponents().apply {
        year = this@toNSDate.year.toLong()
        month = this@toNSDate.monthNumber.toLong()
        day = this@toNSDate.dayOfMonth.toLong()
        hour = this@toNSDate.hour.toLong()
        minute = this@toNSDate.minute.toLong()
        second = this@toNSDate.second.toLong()
    }.toNSDate()
}