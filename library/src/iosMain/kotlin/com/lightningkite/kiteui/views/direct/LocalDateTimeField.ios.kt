package com.lightningkite.kiteui.views.direct


import com.lightningkite.kiteui.locale.renderToString
import com.lightningkite.kiteui.models.Action
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.reactive.reactiveScope
import com.lightningkite.kiteui.views.*
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.datetime.*
import platform.Foundation.NSDate
import platform.UIKit.UIControlEventValueChanged
import platform.UIKit.UIDatePicker
import platform.UIKit.UIDatePickerMode
import platform.UIKit.UIDatePickerStyle


@OptIn(ExperimentalForeignApi::class)
actual class LocalDateField actual constructor(context: RContext) : RView(context) {
    override fun hasAlternateBackedStates(): Boolean = true
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
}
@OptIn(ExperimentalForeignApi::class)
actual class LocalTimeField actual constructor(context: RContext) : RView(context) {
    override fun hasAlternateBackedStates(): Boolean = true
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
            onRemove(onEventNoRemove(UIControlEventValueChanged) {
                _content.value = this.date.toKotlinInstant().toLocalDateTime(TimeZone.currentSystemDefault()).time
            })
        }
        reactiveScope {
            textField.text = _content.invoke()?.renderToString() ?: "-"
        }
    }
}
@OptIn(ExperimentalForeignApi::class)
actual class LocalDateTimeField actual constructor(context: RContext) : RView(context) {
    override fun hasAlternateBackedStates(): Boolean = true
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
            onRemove(onEventNoRemove(UIControlEventValueChanged) {
                _content.value = this.date.toKotlinInstant().toLocalDateTime(TimeZone.currentSystemDefault())
            })
        }
        reactiveScope {
            textField.text = _content.invoke()?.renderToString() ?: "-"
        }
    }
}
