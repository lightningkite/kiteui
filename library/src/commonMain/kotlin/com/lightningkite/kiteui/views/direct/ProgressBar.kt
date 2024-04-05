package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.views.NView
import com.lightningkite.kiteui.views.RView
import com.lightningkite.kiteui.views.ViewDsl
import com.lightningkite.kiteui.views.ViewWriter
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.jvm.JvmInline
import kotlin.contracts.*

expect class NProgressBar : NView

@JvmInline
value class ProgressBar(override val native: NProgressBar) : RView<NProgressBar>

@ViewDsl
expect fun ViewWriter.progressBarActual(setup: ProgressBar.()->Unit = {}): Unit
@OptIn(ExperimentalContracts::class) @ViewDsl inline fun ViewWriter.progressBar(noinline setup: ProgressBar.() -> Unit = {}) { contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }; progressBarActual(setup) }

expect var ProgressBar.ratio: Float