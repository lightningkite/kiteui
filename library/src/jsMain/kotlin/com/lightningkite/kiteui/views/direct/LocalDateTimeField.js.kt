package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.Action
import com.lightningkite.kiteui.reactive.Writable
import com.lightningkite.kiteui.views.ViewDsl
import ViewWriter
import com.lightningkite.kiteui.views.launch
import kotlinx.datetime.*
import org.w3c.dom.HTMLInputElement
import kotlin.js.Date

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual typealias NLocalDateField = HTMLInputElement

@ViewDsl
actual inline fun ViewWriter.localDateFieldActual(crossinline setup: LocalDateField.() -> Unit): Unit =
    themedElementEditable<HTMLInputElement>("input") {
        type = "date"
        setup(LocalDateField(this))
    }

actual val LocalDateField.content: Writable<LocalDate?>
    get() = native.vprop(
        eventName = "input",
        get = {
            (native.valueAsDate as? Date)
                ?.toKotlinInstant()
                ?.toLocalDateTime(TimeZone.UTC)
                ?.date
        },
        set = {
            native.valueAsDate = it?.let { LocalDateTime(it, LocalTime(12, 0, 0)).toInstant(TimeZone.UTC).toJSDate() }
        }
    )
actual var LocalDateField.action: Action?
    get() = TODO()
    set(value) {
        native.onkeyup = if (value == null) null else { ev ->
            if (ev.keyCode == 13) {
                launch {
                    value.onSelect()
                }
            }
        }
    }
actual inline var LocalDateField.range: ClosedRange<LocalDate>?
    get() = TODO()
    set(value) {
        value?.let {
            native.min = it.start.toString()
            native.max = it.endInclusive.toString()
        } ?: run {
            native.removeAttribute("min")
            native.removeAttribute("max")
        }
    }
@Suppress("ACTUAL_WITHOUT_EXPECT")
actual typealias NLocalTimeField = HTMLInputElement

@ViewDsl
actual inline fun ViewWriter.localTimeFieldActual(crossinline setup: LocalTimeField.() -> Unit): Unit =
    themedElementEditable<HTMLInputElement>("input") {
        type = "time"
        setup(LocalTimeField(this))
    }

actual val LocalTimeField.content: Writable<LocalTime?>
    get() = native.vprop(
        "input",
        {
            (native.valueAsDate as? Date)?.toKotlinInstant()?.toLocalDateTime(
                TimeZone.UTC
            )?.time
        },
        {
            valueAsDate =
                it?.let { LocalDateTime(LocalDate(1970, 1, 1), it).toInstant(TimeZone.UTC).toJSDate() }
        }
    )
actual var LocalTimeField.action: Action?
    get() = TODO()
    set(value) {
        native.onkeyup = if (value == null) null else { ev ->
            if (ev.keyCode == 13) {
                launch {
                    value.onSelect()
                }
            }
        }
    }
actual inline var LocalTimeField.range: ClosedRange<LocalTime>?
    get() = TODO()
    set(value) {
        value?.let {
            native.min = it.start.toString().take(5)
            native.max = it.endInclusive.toString().take(5)
        } ?: run {
            native.removeAttribute("min")
            native.removeAttribute("max")
        }
    }
@Suppress("ACTUAL_WITHOUT_EXPECT")
actual typealias NLocalDateTimeField = HTMLInputElement

@ViewDsl
actual inline fun ViewWriter.localDateTimeFieldActual(crossinline setup: LocalDateTimeField.() -> Unit): Unit =
    themedElementEditable<HTMLInputElement>("input") {
        type = "datetime-local"
        setup(LocalDateTimeField(this))
    }

actual val LocalDateTimeField.content: Writable<LocalDateTime?>
    get() = native.vprop(
        "input",
        {
            (native.valueAsDate as? Date)?.toKotlinInstant()?.toLocalDateTime(
                TimeZone.UTC
            )
        },
        {
            valueAsDate = it?.let { it.toInstant(TimeZone.UTC).toJSDate() }
        }
    )
actual var LocalDateTimeField.action: Action?
    get() = TODO()
    set(value) {
        native.onkeyup = if (value == null) null else { ev ->
            if (ev.keyCode == 13) {
                launch {
                    value.onSelect()
                }
            }
        }
    }
actual inline var LocalDateTimeField.range: ClosedRange<LocalDateTime>?
    get() = TODO()
    set(value) {
        value?.let {
            native.min = it.start.toString()
            native.max = it.endInclusive.toString()
        } ?: run {
            native.removeAttribute("min")
            native.removeAttribute("max")
        }
    }