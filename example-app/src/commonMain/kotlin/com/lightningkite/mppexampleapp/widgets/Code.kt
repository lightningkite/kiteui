@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package com.lightningkite.mppexampleapp.widgets

import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.RView
import com.lightningkite.kiteui.views.ViewDsl
import com.lightningkite.kiteui.views.ViewWriter
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

expect class Code constructor(context: ElementContext): RView {
    var content: String
}

@OptIn(ExperimentalContracts::class)
@ViewDsl
inline fun ViewWriter.code(setup: Code.() -> Unit = {}): Code {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(Code(context) , setup)
}
fun ViewWriter.code(content: String) = code { this.content = content }