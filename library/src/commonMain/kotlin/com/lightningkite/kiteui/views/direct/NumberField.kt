package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.Action
import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.Dimension
import com.lightningkite.kiteui.models.KeyboardHints
import com.lightningkite.kiteui.reactive.Writable
import com.lightningkite.kiteui.views.NView
import com.lightningkite.kiteui.views.RView
import com.lightningkite.kiteui.views.ViewDsl
import com.lightningkite.kiteui.views.ViewWriter
import kotlin.jvm.JvmInline
import kotlin.contracts.*

expect class NNumberField : NView

@JvmInline
value class NumberField(override val native: NNumberField) : RView<NNumberField>

@ViewDsl
expect fun ViewWriter.numberFieldActual(setup: NumberField.()->Unit = {}): Unit
@OptIn(ExperimentalContracts::class) @ViewDsl inline fun ViewWriter.numberField(noinline setup: NumberField.() -> Unit = {}) { contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }; numberFieldActual(setup) }
expect val NumberField.content: Writable<Double?>
expect var NumberField.keyboardHints: KeyboardHints
expect var NumberField.action: Action?
expect var NumberField.hint: String
expect var NumberField.range: ClosedRange<Double>?
expect var NumberField.align: Align
expect var NumberField.textSize: Dimension