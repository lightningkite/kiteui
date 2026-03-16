package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.dom.KeyboardEvent
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.utils.commaString
import com.lightningkite.kiteui.utils.numberAutocommaRepair
import com.lightningkite.kiteui.views.*
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*
import kotlinx.datetime.*

actual class LocalDateTimeField actual constructor(context: RContext) : RViewWithAction(context) {
    override val driverValue: String? get() = localDateTimeDriverValue()
    override val driverActions get() = super.driverActions + localDateTimeDriverActions()
    companion object {
        val charCount = "2024-06-01T08:30".length
    }
    init {
        native.tag = "input"
        native.attributes.type = "datetime-local"
        native.classes.add("editable")
    }
    actual val content: MutableReactiveValue<LocalDateTime?> = object : MutableReactiveValue<LocalDateTime?>, BaseListenable() {
        init {
            native.addEventListener("input") {
                invokeAllListeners()
            }
        }

        override var value: LocalDateTime?
            get() = native.attributes.valueString?.takeUnless { it.isEmpty() }?.let { LocalDateTime.parse(it) }
            set(value) { native.attributes.valueString = value?.toString()?.take(charCount) }
    }
    init {
        native.addEventListener("keyup") { ev ->
            ev as KeyboardEvent
            if (ev.code == KeyCodes.enter) {
                action?.startAction(this)
            }
        }
    }
    inline var hint: String
        get() = native.attributes.placeholder ?: ""
        set(value) {
            native.attributes.placeholder = value
        }
    var align: Align = Align.Start
        set(value) {
            field = value
            native.style.textAlign = when (value) {
                Align.Start -> "start"
                Align.Center -> "center"
                Align.End -> "end"
                Align.Stretch -> "justify"
            }
        }
    var textSize: Dimension = 1.rem
        set(value) {
            field = value
            native.style.fontSize = value.value.toString()
        }
    actual var range: ClosedRange<LocalDateTime>? = null
        set(value) {
            field = value
            value?.let {
                native.attributes.maxString = it.start.toString().take(charCount)
                native.attributes.maxString = it.endInclusive.toString().take(charCount)
            } ?: run {
                native.attributes.maxString = null
                native.attributes.maxString = null
            }
        }
}


actual class LocalDateField actual constructor(context: RContext) : RViewWithAction(context) {
    override val driverValue: String? get() = localDateDriverValue()
    override val driverActions get() = super.driverActions + localDateDriverActions()
    companion object {
        val charCount = "2024-06-01".length
    }
    init {
        native.tag = "input"
        native.attributes.type = "date"
        native.classes.add("editable")
    }
    actual val content: MutableReactiveValue<LocalDate?> = object : MutableReactiveValue<LocalDate?>, BaseListenable() {
        init {
            native.addEventListener("input") {
                invokeAllListeners()
            }
        }

        override var value: LocalDate?
            get() = native.attributes.valueString?.takeUnless { it.isEmpty() }?.let { LocalDate.parse(it) }
            set(value) {
                native.attributes.valueString = value?.toString()?.take(charCount)
            }
    }
    init {
        native.addEventListener("keyup") { ev ->
            ev as KeyboardEvent
            if (ev.code == KeyCodes.enter) {
                action?.startAction(this)
            }
        }
    }
    inline var hint: String
        get() = native.attributes.placeholder ?: ""
        set(value) {
            native.attributes.placeholder = value
        }
    var align: Align = Align.Start
        set(value) {
            field = value
            native.style.textAlign = when (value) {
                Align.Start -> "start"
                Align.Center -> "center"
                Align.End -> "end"
                Align.Stretch -> "justify"
            }
        }
    var textSize: Dimension = 1.rem
        set(value) {
            field = value
            native.style.fontSize = value.value.toString()
        }
    actual var range: ClosedRange<LocalDate>? = null
        set(value) {
            field = value
            value?.let {
                native.attributes.minString = it.start.toString().take(charCount)
                native.attributes.maxString = it.endInclusive.toString().take(charCount)
            } ?: run {
                native.attributes.minString = null
                native.attributes.maxString = null
            }
        }
}

actual class LocalTimeField actual constructor(context: RContext) : RViewWithAction(context) {
    override val driverValue: String? get() = localTimeDriverValue()
    override val driverActions get() = super.driverActions + localTimeDriverActions()
    companion object {
        val charCount = "08:30".length
    }
    init {
        native.tag = "input"
        native.attributes.type = "time"
        native.classes.add("editable")
    }

    actual val content: MutableReactiveValue<LocalTime?> = object : MutableReactiveValue<LocalTime?>, BaseListenable() {
        init {
            native.addEventListener("input") {
                invokeAllListeners()
            }
        }
        override var value: LocalTime?
            get() = native.attributes.valueString?.takeUnless { it.isEmpty() }?.let { LocalTime.parse(it) }
            set(value) {
                native.attributes.valueString = value?.toString()?.take(charCount)
            }
    }
    init {
        native.addEventListener("keyup") { ev ->
            ev as KeyboardEvent
            if (ev.code == KeyCodes.enter) {
                action?.startAction(this)
            }
        }
    }
    inline var hint: String
        get() = native.attributes.placeholder ?: ""
        set(value) {
            native.attributes.placeholder = value
        }
    var align: Align = Align.Start
        set(value) {
            field = value
            native.style.textAlign = when (value) {
                Align.Start -> "start"
                Align.Center -> "center"
                Align.End -> "end"
                Align.Stretch -> "justify"
            }
        }
    var textSize: Dimension = 1.rem
        set(value) {
            field = value
            native.style.fontSize = value.value.toString()
        }
    actual var range: ClosedRange<LocalTime>? = null
        set(value) {
            field = value
            value?.let {
                native.attributes.minString = it.start.toString().take(charCount)
                native.attributes.maxString = it.endInclusive.toString().take(charCount)
            } ?: run {
                native.attributes.minString = null
                native.attributes.maxString = null
            }
        }
}

//@Suppress("ACTUAL_WITHOUT_EXPECT")
//actual typealias NLocalDateField = HTMLInputElement
//
//@ViewDsl
//actual inline fun ViewWriter.localDateFieldActual(crossinline setup: LocalDateField.() -> Unit): Unit =
//    themedElementEditable<HTMLInputElement>("input") {
//        type = "date"
//        setup(LocalDateField(this))
//    }
//
//actual val LocalDateField.content: MutableReactive<LocalDate?>
//    get() = native.vprop(
//        eventName = "input",
//        get = {
//            (native.valueAsDate as? Date)
//                ?.toKotlinInstant()
//                ?.toLocalDateTime(TimeZone.UTC)
//                ?.date
//        },
//        set = {
//            native.valueAsDate = it?.let { LocalDateTime(it, LocalTime(12, 0, 0)).toInstant(TimeZone.UTC).toJSDate() }
//        }
//    )
//actual var LocalDateField.action: Action?
//    get() = TODO()
//    set(value) {
//        native.onkeyup = if (value == null) null else { ev ->
//            if (ev.keyCode == 13) {
//                launch {
//                    value.onSelect()
//                }
//            }
//        }
//    }
//actual inline var LocalDateField.range: ClosedRange<LocalDate>?
//    get() = TODO()
//    set(value) {
//        value?.let {
//            native.min = it.start.toString()
//            native.max = it.endInclusive.toString()
//        } ?: run {
//            native.removeAttribute("min")
//            native.removeAttribute("max")
//        }
//    }
//@Suppress("ACTUAL_WITHOUT_EXPECT")
//actual typealias NLocalTimeField = HTMLInputElement
//
//@ViewDsl
//actual inline fun ViewWriter.localTimeFieldActual(crossinline setup: LocalTimeField.() -> Unit): Unit =
//    themedElementEditable<HTMLInputElement>("input") {
//        type = "time"
//        setup(LocalTimeField(this))
//    }
//
//actual val LocalTimeField.content: MutableReactive<LocalTime?>
//    get() = native.vprop(
//        "input",
//        {
//            (native.valueAsDate as? Date)?.toKotlinInstant()?.toLocalDateTime(
//                TimeZone.UTC
//            )?.time
//        },
//        {
//            valueAsDate =
//                it?.let { LocalDateTime(LocalDate(1970, 1, 1), it).toInstant(TimeZone.UTC).toJSDate() }
//        }
//    )
//actual var LocalTimeField.action: Action?
//    get() = TODO()
//    set(value) {
//        native.onkeyup = if (value == null) null else { ev ->
//            if (ev.keyCode == 13) {
//                launch {
//                    value.onSelect()
//                }
//            }
//        }
//    }
//actual inline var LocalTimeField.range: ClosedRange<LocalTime>?
//    get() = TODO()
//    set(value) {
//        value?.let {
//            native.min = it.start.toString().take(5)
//            native.max = it.endInclusive.toString().take(5)
//        } ?: run {
//            native.removeAttribute("min")
//            native.removeAttribute("max")
//        }
//    }
//@Suppress("ACTUAL_WITHOUT_EXPECT")
//actual typealias NLocalDateTimeField = HTMLInputElement
//
//@ViewDsl
//actual inline fun ViewWriter.localDateTimeFieldActual(crossinline setup: LocalDateTimeField.() -> Unit): Unit =
//    themedElementEditable<HTMLInputElement>("input") {
//        type = "datetime-local"
//        setup(LocalDateTimeField(this))
//    }
//
//actual val LocalDateTimeField.content: MutableReactive<LocalDateTime?>
//    get() = native.vprop(
//        "input",
//        {
//            (native.valueAsDate as? Date)?.toKotlinInstant()?.toLocalDateTime(
//                TimeZone.UTC
//            )
//        },
//        {
//            valueAsDate = it?.let { it.toInstant(TimeZone.UTC).toJSDate() }
//        }
//    )
//actual var LocalDateTimeField.action: Action?
//    get() = TODO()
//    set(value) {
//        native.onkeyup = if (value == null) null else { ev ->
//            if (ev.keyCode == 13) {
//                launch {
//                    value.onSelect()
//                }
//            }
//        }
//    }
//actual inline var LocalDateTimeField.range: ClosedRange<LocalDateTime>?
//    get() = TODO()
//    set(value) {
//        value?.let {
//            native.min = it.start.toString()
//            native.max = it.endInclusive.toString()
//        } ?: run {
//            native.removeAttribute("min")
//            native.removeAttribute("max")
//        }
//    }