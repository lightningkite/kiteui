package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.dom.KeyboardEvent
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.reactive.core.*
import kotlinx.datetime.*
import com.lightningkite.kiteui.views.AiDriver

public actual class LocalDateTimeField actual constructor(context: ElementContext) : NativeElementWithAction(context) {
    override val driverValue: String? get() = localDateTimeDriverValue()
    override val driverActions: AiDriver.Actions get() = super.driverActions + localDateTimeDriverActions()
    public companion object {
        internal val charCount: Int = "2024-06-01T08:30".length
    }
    init {
        native.tag = "input"
        native.attributes.type = "datetime-local"
        native.classes.add("editable")
    }
    public actual val content: MutableReactiveValue<LocalDateTime?> = object : MutableReactiveValue<LocalDateTime?>, BaseListenable() {
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
    public inline var hint: String
        get() = native.attributes.placeholder ?: ""
        set(value) {
            native.attributes.placeholder = value
        }
    internal var align: Align = Align.Start
        set(value) {
            field = value
            native.style.textAlign = when (value) {
                Align.Start -> "start"
                Align.Center -> "center"
                Align.End -> "end"
                Align.Stretch -> "justify"
            }
        }
    internal var textSize: Dimension = 1.rem
        set(value) {
            field = value
            native.style.fontSize = value.value.toString()
        }
    public actual var range: ClosedRange<LocalDateTime>? = null
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


public actual class LocalDateField actual constructor(context: ElementContext) : NativeElementWithAction(context) {
    override val driverValue: String? get() = localDateDriverValue()
    override val driverActions: AiDriver.Actions get() = super.driverActions + localDateDriverActions()
    public companion object {
        internal val charCount: Int = "2024-06-01".length
    }
    init {
        native.tag = "input"
        native.attributes.type = "date"
        native.classes.add("editable")
    }
    public actual val content: MutableReactiveValue<LocalDate?> = object : MutableReactiveValue<LocalDate?>, BaseListenable() {
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
    public inline var hint: String
        get() = native.attributes.placeholder ?: ""
        set(value) {
            native.attributes.placeholder = value
        }
    internal var align: Align = Align.Start
        set(value) {
            field = value
            native.style.textAlign = when (value) {
                Align.Start -> "start"
                Align.Center -> "center"
                Align.End -> "end"
                Align.Stretch -> "justify"
            }
        }
    internal var textSize: Dimension = 1.rem
        set(value) {
            field = value
            native.style.fontSize = value.value.toString()
        }
    public actual var range: ClosedRange<LocalDate>? = null
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

public actual class LocalTimeField actual constructor(context: ElementContext) : NativeElementWithAction(context) {
    override val driverValue: String? get() = localTimeDriverValue()
    override val driverActions: AiDriver.Actions get() = super.driverActions + localTimeDriverActions()
    public companion object {
        internal val charCount: Int = "08:30".length
    }
    init {
        native.tag = "input"
        native.attributes.type = "time"
        native.classes.add("editable")
    }

    public actual val content: MutableReactiveValue<LocalTime?> = object : MutableReactiveValue<LocalTime?>, BaseListenable() {
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
    public inline var hint: String
        get() = native.attributes.placeholder ?: ""
        set(value) {
            native.attributes.placeholder = value
        }
    internal var align: Align = Align.Start
        set(value) {
            field = value
            native.style.textAlign = when (value) {
                Align.Start -> "start"
                Align.Center -> "center"
                Align.End -> "end"
                Align.Stretch -> "justify"
            }
        }
    internal var textSize: Dimension = 1.rem
        set(value) {
            field = value
            native.style.fontSize = value.value.toString()
        }
    public actual var range: ClosedRange<LocalTime>? = null
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
