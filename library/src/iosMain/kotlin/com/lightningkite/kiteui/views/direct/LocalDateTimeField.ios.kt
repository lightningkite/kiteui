package com.lightningkite.kiteui.views.direct


import com.lightningkite.kiteui.locale.renderToString
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.reactive.reactiveScope
import com.lightningkite.kiteui.views.*
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.datetime.*
import platform.Foundation.NSDate
import platform.UIKit.*



actual class LocalDateField actual constructor(context: RContext) : RView(context) {
    override val native = WrapperView()
    val textField = TextFieldInput(this)
    init { native.addSubview(textField) }

    private val _content = Property<LocalDate?>(null)
    actual val content: ImmediateWritable<LocalDate?> get() = _content
    actual var action: Action?
        get() = textField.action
        set(value) {
            textField.action = value
        }
    // TODO
    actual var range: ClosedRange<LocalDate>? = null

    init {
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
    override fun applyForeground(theme: Theme) {
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
    override fun hasAlternateBackedStates(): Boolean = true
    override fun applyState(theme: ThemeAndBack): ThemeAndBack {
        var t = theme
        if(!enabled) t = t[DisabledSemantic]
        if(textField.highlighted) t = t[DownSemantic]
        if(textField.focused) t = t[FocusSemantic]
        return t
    }
}

actual class LocalTimeField actual constructor(context: RContext) : RView(context) {
    override val native = WrapperView()
    val textField = TextFieldInput(this)
    init { native.addSubview(textField) }

    private val _content = Property<LocalTime?>(null)
    actual val content: ImmediateWritable<LocalTime?> get() = _content
    actual var action: Action?
        get() = textField.action
        set(value) {
            textField.action = value
        }
    // TODO
    actual var range: ClosedRange<LocalTime>? = null

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
    override fun applyForeground(theme: Theme) {
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
    override fun hasAlternateBackedStates(): Boolean = true
    override fun applyState(theme: ThemeAndBack): ThemeAndBack {
        var t = theme
        if(!enabled) t = t[DisabledSemantic]
        if(textField.highlighted) t = t[DownSemantic]
        if(textField.focused) t = t[FocusSemantic]
        return t
    }
}

actual class LocalDateTimeField actual constructor(context: RContext) : RView(context) {
    override val native = WrapperView()
    val textField = TextFieldInput(this)
    init { native.addSubview(textField) }

    private val _content = Property<LocalDateTime?>(null)
    actual val content: ImmediateWritable<LocalDateTime?> get() = _content
    actual var action: Action?
        get() = textField.action
        set(value) {
            textField.action = value
        }
    // TODO
    actual var range: ClosedRange<LocalDateTime>? = null

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
    override fun applyForeground(theme: Theme) {
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
    override fun hasAlternateBackedStates(): Boolean = true
    override fun applyState(theme: ThemeAndBack): ThemeAndBack {
        var t = theme
        if(!enabled) t = t[DisabledSemantic]
        if(textField.highlighted) t = t[DownSemantic]
        if(textField.focused) t = t[FocusSemantic]
        return t
    }
}
