package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.Action
import com.lightningkite.kiteui.reactive.Property
import com.lightningkite.kiteui.reactive.Writable
import com.lightningkite.kiteui.reactive.await
import com.lightningkite.kiteui.views.*
import kotlinx.datetime.*
import platform.Foundation.NSDate
import platform.UIKit.UIControlEventValueChanged
import platform.UIKit.UIDatePicker
import platform.UIKit.UIDatePickerMode
import platform.UIKit.UIDatePickerStyle

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual typealias NLocalDateField = TextFieldInput

@ViewDsl
actual inline fun ViewWriter.localDateFieldActual(crossinline setup: LocalDateField.() -> Unit): Unit = stack {
    element(TextFieldInput()) {
        handleTheme(this, foreground = { textColor = it.foreground.closestColor().toUiColor() },) {
            val p = Property<LocalDate?>(null)
            currentValue = p
            inputView = UIDatePicker().apply {
                setPreferredDatePickerStyle(UIDatePickerStyle.UIDatePickerStyleInline)
                datePickerMode = UIDatePickerMode.UIDatePickerModeDate
                date = p.value?.toNSDateComponents()?.date() ?: NSDate()
                this@element.calculationContext.onRemove(onEventNoRemove(UIControlEventValueChanged) {
                    p.value = this.date.toKotlinInstant().toLocalDateTime(TimeZone.currentSystemDefault()).date
                })
            }
            setup(LocalDateField(this))
            reactiveScope { text = p.await()?.toString() ?: "Pick one" }
        }
    }
}

actual var LocalDateField.action: Action?
    get() = native.action
    set(value) {
        native.action = value
    }
actual val LocalDateField.content: Writable<LocalDate?>
    get() {
        @Suppress("UNCHECKED_CAST")
        return native.currentValue as Property<LocalDate?>
    }
actual inline var LocalDateField.range: ClosedRange<LocalDate>?
    get() {
        @Suppress("UNCHECKED_CAST")
        return native.valueRange as ClosedRange<LocalDate>
    }
    set(value) {
        native.valueRange = value
    }
@Suppress("ACTUAL_WITHOUT_EXPECT")
actual typealias NLocalTimeField = TextFieldInput

@ViewDsl
actual inline fun ViewWriter.localTimeFieldActual(crossinline setup: LocalTimeField.() -> Unit): Unit = stack {
    element(TextFieldInput()) {
        handleTheme(this, foreground = { textColor = it.foreground.closestColor().toUiColor() },) {
            val p = Property<LocalTime?>(null)
            currentValue = p
            inputView = UIDatePicker().apply {
                setPreferredDatePickerStyle(UIDatePickerStyle.UIDatePickerStyleWheels)
                datePickerMode = UIDatePickerMode.UIDatePickerModeTime
                date = p.value?.atDate(1970, 1, 1)?.toNSDateComponents()?.date() ?: NSDate()
                this@element.calculationContext.onRemove(onEventNoRemove(UIControlEventValueChanged) {
                    p.value = this.date.toKotlinInstant().toLocalDateTime(TimeZone.currentSystemDefault()).time
                })
            }
            setup(LocalTimeField(this))
            reactiveScope { text = p.await()?.toString() ?: "Pick one" }
        }
    }
}

actual var LocalTimeField.action: Action?
    get() = native.action
    set(value) {
        native.action = value
    }
actual val LocalTimeField.content: Writable<LocalTime?>
    get() {
        @Suppress("UNCHECKED_CAST")
        return native.currentValue as Property<LocalTime?>
    }
actual inline var LocalTimeField.range: ClosedRange<LocalTime>?
    get() {
        @Suppress("UNCHECKED_CAST")
        return native.valueRange as ClosedRange<LocalTime>
    }
    set(value) {
        native.valueRange = value
    }
@Suppress("ACTUAL_WITHOUT_EXPECT")
actual typealias NLocalDateTimeField = TextFieldInput

@ViewDsl
actual inline fun ViewWriter.localDateTimeFieldActual(crossinline setup: LocalDateTimeField.() -> Unit): Unit = stack {
    element(TextFieldInput()) {
        handleTheme(this, foreground = { textColor = it.foreground.closestColor().toUiColor() },) {
            val p = Property<LocalDateTime?>(null)
            currentValue = p
            inputView = UIDatePicker().apply {
                setPreferredDatePickerStyle(UIDatePickerStyle.UIDatePickerStyleWheels)
                datePickerMode = UIDatePickerMode.UIDatePickerModeDateAndTime
                date = p.value?.toNSDateComponents()?.date() ?: NSDate()
                this@element.calculationContext.onRemove(onEventNoRemove(UIControlEventValueChanged) {
                    p.value = this.date.toKotlinInstant().toLocalDateTime(TimeZone.currentSystemDefault())
                })
            }
            setup(LocalDateTimeField(this))
            reactiveScope { text = p.await()?.toString() ?: "Pick one" }
        }
    }
}

actual var LocalDateTimeField.action: Action?
    get() = native.action
    set(value) {
        native.action = value
    }
actual val LocalDateTimeField.content: Writable<LocalDateTime?>
    get() {
        @Suppress("UNCHECKED_CAST")
        return native.currentValue as Property<LocalDateTime?>
    }
actual inline var LocalDateTimeField.range: ClosedRange<LocalDateTime>?
    get() {
        @Suppress("UNCHECKED_CAST")
        return native.valueRange as ClosedRange<LocalDateTime>
    }
    set(value) {
        native.valueRange = value
    }