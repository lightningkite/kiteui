@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package com.lightningkite.mppexampleapp.widgets

import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.ElementWriter
import com.lightningkite.kiteui.views.NativeElement
import com.lightningkite.kiteui.views.ViewDsl
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.write
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

expect class Code(context: ElementContext): NativeElement {
    var content: String
}

@OptIn(ExperimentalContracts::class)
inline fun ElementWriter.code(setup: Code.() -> Unit = {}): Code {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(Code(context) , setup)
}

@ViewDsl
fun ElementWriter.code(content: String) = code { this.content = content }