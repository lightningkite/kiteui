package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.Dimension
import com.lightningkite.kiteui.models.TextOverflow
import com.lightningkite.kiteui.views.NView
import com.lightningkite.kiteui.views.RView
import com.lightningkite.kiteui.views.ViewDsl
import com.lightningkite.kiteui.views.ViewWriter
import kotlin.jvm.JvmInline
import kotlin.contracts.*

expect class NTextView : NView

@JvmInline
value class TextView(override val native: NTextView) : RView<NTextView>

@ViewDsl
expect fun ViewWriter.h1Actual(setup: TextView.()->Unit = {}): Unit
@OptIn(ExperimentalContracts::class) @ViewDsl inline fun ViewWriter.h1(noinline setup: TextView.() -> Unit = {}) { contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }; h1Actual(setup) }
@ViewDsl
expect fun ViewWriter.h2Actual(setup: TextView.()->Unit = {}): Unit
@OptIn(ExperimentalContracts::class) @ViewDsl inline fun ViewWriter.h2(noinline setup: TextView.() -> Unit = {}) { contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }; h2Actual(setup) }
@ViewDsl
expect fun ViewWriter.h3Actual(setup: TextView.()->Unit = {}): Unit
@OptIn(ExperimentalContracts::class) @ViewDsl inline fun ViewWriter.h3(noinline setup: TextView.() -> Unit = {}) { contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }; h3Actual(setup) }
@ViewDsl
expect fun ViewWriter.h4Actual(setup: TextView.()->Unit = {}): Unit
@OptIn(ExperimentalContracts::class) @ViewDsl inline fun ViewWriter.h4(noinline setup: TextView.() -> Unit = {}) { contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }; h4Actual(setup) }
@ViewDsl
expect fun ViewWriter.h5Actual(setup: TextView.()->Unit = {}): Unit
@OptIn(ExperimentalContracts::class) @ViewDsl inline fun ViewWriter.h5(noinline setup: TextView.() -> Unit = {}) { contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }; h5Actual(setup) }
@ViewDsl
expect fun ViewWriter.h6Actual(setup: TextView.()->Unit = {}): Unit
@OptIn(ExperimentalContracts::class) @ViewDsl inline fun ViewWriter.h6(noinline setup: TextView.() -> Unit = {}) { contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }; h6Actual(setup) }
@ViewDsl
expect fun ViewWriter.textActual(setup: TextView.()->Unit = {}): Unit
@OptIn(ExperimentalContracts::class) @ViewDsl inline fun ViewWriter.text(noinline setup: TextView.() -> Unit = {}) { contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }; textActual(setup) }
@ViewDsl
expect fun ViewWriter.subtextActual(setup: TextView.()->Unit = {}): Unit
@OptIn(ExperimentalContracts::class) @ViewDsl inline fun ViewWriter.subtext(noinline setup: TextView.() -> Unit = {}) { contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }; subtextActual(setup) }
expect var TextView.content: String
expect var TextView.align: Align
expect var TextView.textSize: Dimension
expect var TextView.ellipsis: Boolean
expect var TextView.wraps: Boolean
