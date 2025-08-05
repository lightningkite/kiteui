package com.lightningkite.kiteui.views.direct


import com.lightningkite.kiteui.InternalKiteUi
import com.lightningkite.kiteui.locale.renderToString
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.reactive.Action
import com.lightningkite.kiteui.views.*
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.datetime.*
import platform.Foundation.NSDate
import platform.UIKit.*



@InternalKiteUi
public actual class LocalDateField public actual constructor(context: RContext) : RViewWithAction(context) {
    override val native: WrapperView = WrapperView()
    public val textField: TextFieldInput = TextFieldInput(this)
    init { native.addSubview(textField) }

    private val _content = Signal<LocalDate?>(null)
    public actual val content: MutableReactiveValue<LocalDate?> get() = _content
    // TODO
    public actual var range: ClosedRange<LocalDate>? = null

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

    public var fontAndStyle: FontAndStyle? = null
        set(value) {
            field = value
            updateFont()
            native.informParentOfSizeChange()
        }
    override fun applyTheme(theme: ThemeAndBack) { super.applyTheme(theme); val theme = theme.theme
        textField.textColor = theme.foreground.closestColor().toUiColor()
        fontAndStyle = theme.font
    }
    public fun updateFont() {
        val alignment = textField.textAlignment
        textField.font = fontAndStyle?.let {
            it.font.get(it.size.value * preferredScaleFactor(), it.weight.toUIFontWeight(), it.italic)
        } ?: UIFont.systemFontOfSize(16.0)
        textField.textAlignment = alignment
    }

    public var enabled: Boolean
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

@InternalKiteUi
public actual class LocalTimeField public actual constructor(context: RContext) : RViewWithAction(context) {
    override val native: WrapperView = WrapperView()
    public val textField: TextFieldInput = TextFieldInput(this)
    init { native.addSubview(textField) }

    private val _content = Signal<LocalTime?>(null)
    public actual val content: MutableReactiveValue<LocalTime?> get() = _content
    override fun actionSet(value: Action?) {
        super.actionSet(value)
        textField.action = value
    }
    // TODO
    public actual var range: ClosedRange<LocalTime>? = null

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

    public var fontAndStyle: FontAndStyle? = null
        set(value) {
            field = value
            updateFont()
            native.informParentOfSizeChange()
        }
    override fun applyTheme(theme: ThemeAndBack) { super.applyTheme(theme); val theme = theme.theme
        textField.textColor = theme.foreground.closestColor().toUiColor()
        fontAndStyle = theme.font
    }
    public fun updateFont() {
        val alignment = textField.textAlignment
        textField.font = fontAndStyle?.let {
            it.font.get(it.size.value * preferredScaleFactor(), it.weight.toUIFontWeight(), it.italic)
        } ?: UIFont.systemFontOfSize(16.0)
        textField.textAlignment = alignment
    }

    public var enabled: Boolean
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

@InternalKiteUi
public actual class LocalDateTimeField public actual constructor(context: RContext) : RViewWithAction(context) {
    override val native: WrapperView = WrapperView()
    public val textField: TextFieldInput = TextFieldInput(this)
    init { native.addSubview(textField) }

    private val _content = Signal<LocalDateTime?>(null)
    public actual val content: MutableReactiveValue<LocalDateTime?> get() = _content
    override fun actionSet(value: Action?) {
        super.actionSet(value)
        textField.action = value
    }
    // TODO
    public actual var range: ClosedRange<LocalDateTime>? = null

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

    public var fontAndStyle: FontAndStyle? = null
        set(value) {
            field = value
            updateFont()
            native.informParentOfSizeChange()
        }
    override fun applyTheme(theme: ThemeAndBack) { super.applyTheme(theme); val theme = theme.theme
        textField.textColor = theme.foreground.closestColor().toUiColor()
        fontAndStyle = theme.font
    }
    public fun updateFont() {
        val alignment = textField.textAlignment
        textField.font = fontAndStyle?.let {
            it.font.get(it.size.value * preferredScaleFactor(), it.weight.toUIFontWeight(), it.italic)
        } ?: UIFont.systemFontOfSize(16.0)
        textField.textAlignment = alignment
    }

    public var enabled: Boolean
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
