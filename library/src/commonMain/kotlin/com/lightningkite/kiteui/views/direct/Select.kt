package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.reactive.Readable
import com.lightningkite.kiteui.reactive.Writable
import com.lightningkite.kiteui.views.NView
import com.lightningkite.kiteui.views.RView
import com.lightningkite.kiteui.views.ViewDsl
import com.lightningkite.kiteui.views.ViewWriter
import kotlin.jvm.JvmInline
import kotlin.contracts.*

expect class NSelect : NView

@JvmInline
value class Select(override val native: NSelect) : RView<NSelect>

@ViewDsl
expect fun ViewWriter.selectActual(setup: Select.()->Unit = {}): Unit
@OptIn(ExperimentalContracts::class) @ViewDsl inline fun ViewWriter.select(noinline setup: Select.() -> Unit = {}) { contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }; selectActual(setup) }
expect fun <T> Select.bind(edits: Writable<T>, data: Readable<List<T>>, render: (T)->String)

expect var Select.enabled: Boolean