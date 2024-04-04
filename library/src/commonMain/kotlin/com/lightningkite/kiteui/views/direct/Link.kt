package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.navigation.KiteUiNavigator
import com.lightningkite.kiteui.navigation.KiteUiScreen
import com.lightningkite.kiteui.views.NView
import com.lightningkite.kiteui.views.RView
import com.lightningkite.kiteui.views.ViewDsl
import com.lightningkite.kiteui.views.ViewWriter
import kotlin.jvm.JvmInline
import kotlin.contracts.*

expect class NLink : NView

@JvmInline
value class Link(override val native: NLink) : RView<NLink>

@ViewDsl
expect fun ViewWriter.linkActual(setup: Link.()->Unit = {}): Unit
@OptIn(ExperimentalContracts::class) @ViewDsl inline fun ViewWriter.link(noinline setup: Link.() -> Unit = {}) { contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }; linkActual(setup) }
expect var Link.to: KiteUiScreen
expect var Link.navigator: KiteUiNavigator
expect var Link.newTab: Boolean
expect fun Link.onNavigate(action: suspend () -> Unit)
