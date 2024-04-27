package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.Dimension
import com.lightningkite.kiteui.models.px
import com.lightningkite.kiteui.views.*
import kotlin.jvm.JvmInline
import kotlin.contracts.*

expect class NContainingView : NView

@JvmInline
value class ContainingView(override val native: NContainingView) : RView<NContainingView>

@ViewDsl
expect fun ViewWriter.stackActual(setup: ContainingView.()->Unit = {}): Unit
@OptIn(ExperimentalContracts::class) @ViewDsl inline fun ViewWriter.stack(noinline setup: ContainingView.() -> Unit = {}) { contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }; stackActual(setup) }
@ViewDsl
expect fun ViewWriter.colActual(setup: ContainingView.()->Unit = {}): Unit
@OptIn(ExperimentalContracts::class) @ViewDsl inline fun ViewWriter.col(noinline setup: ContainingView.() -> Unit = {}) { contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }; colActual(setup) }
@ViewDsl
expect fun ViewWriter.rowActual(setup: ContainingView.()->Unit = {}): Unit
@OptIn(ExperimentalContracts::class) @ViewDsl inline fun ViewWriter.row(noinline setup: ContainingView.() -> Unit = {}) { contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }; rowActual(setup) }
@ViewDsl
expect fun ViewWriter.rowCollapsingToColumnActual(breakpoint: Dimension, setup: ContainingView.()->Unit = {}): Unit
@OptIn(ExperimentalContracts::class) @ViewDsl inline fun ViewWriter.rowCollapsingToColumn(breakpoint: Dimension, noinline setup: ContainingView.() -> Unit = {}) { contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }; rowCollapsingToColumnActual(breakpoint, setup) }

expect var ContainingView.vertical: Boolean

var ContainingView.spacing: Dimension
    get() = native.spacing
    set(value) {
        native.spacing = value
    }