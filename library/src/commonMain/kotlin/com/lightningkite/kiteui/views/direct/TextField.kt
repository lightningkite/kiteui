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

expect class NTextField : NView

@JvmInline
value class TextField(override val native: NTextField) : RView<NTextField>

@ViewDsl
expect fun ViewWriter.textFieldActual(setup: TextField.()->Unit = {}): Unit
@OptIn(ExperimentalContracts::class) @ViewDsl inline fun ViewWriter.textField(noinline setup: TextField.() -> Unit = {}) { contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }; textFieldActual(setup) }
expect val TextField.content: Writable<String>
expect var TextField.keyboardHints: KeyboardHints
expect var TextField.action: Action?
expect var TextField.hint: String
expect var TextField.align: Align
expect var TextField.textSize: Dimension
