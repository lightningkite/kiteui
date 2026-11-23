package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.KeyboardHints
import com.lightningkite.kiteui.models.TextFormat
import com.lightningkite.kiteui.models.toStringNoExponential
import com.lightningkite.kiteui.views.AiDriver
import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.ElementWithAction
import com.lightningkite.reactive.core.MutableReactiveValue


/** An input for a number, shown with commas between each group of three digits. */
public class NumberInput internal constructor(internal val input: FormattedTextInput) : ElementWithAction by input {
    public constructor(context: ElementContext) : this(FormattedTextInput(context))

    /** The typed number, or `null` while the text isn't one yet (empty, or just a '-'). */
    public val content: MutableReactiveValue<Double?> = input.content.lens(
        get = { it.toDoubleOrNull() },
        // Claude: keeps what's typed, like "1." or "01", when the number itself doesn't change.
        set = { if (it == input.content.value.toDoubleOrNull()) input.content.value else it?.toStringNoExponential() ?: "" },
    )

    /** The keyboard to show; a [KeyboardHints.type] that doesn't allow decimals also stops '.' from being typed. */
    public var keyboardHints: KeyboardHints
        get() = input.keyboardHints
        set(value) {
            input.keyboardHints = value
            input.format = TextFormat.number(value.type.allowDecimal)
        }

    public var hint: String by input::hint
    public var align: Align? by input::align

    /** The values this input is meant to hold. Typing isn't held to it; only the AI driver's `setValue` clamps to it. */
    public var range: ClosedRange<Double>? = null

    override val driverValue: String get() = numberInputDriverValue()
    override val driverActions: AiDriver.Actions get() = input.driverActions + numberInputDriverActions()

    // Claude: class delegation doesn't forward toString; the input's version already describes this wrapper.
    override fun toString(): String = input.toString()

    init {
        keyboardHints = KeyboardHints.decimal
    }
}
