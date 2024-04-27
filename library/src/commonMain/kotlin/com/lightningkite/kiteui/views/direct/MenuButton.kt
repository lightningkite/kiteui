package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.PopoverPreferredDirection
import com.lightningkite.kiteui.views.NView
import com.lightningkite.kiteui.views.RView
import com.lightningkite.kiteui.views.ViewDsl
import com.lightningkite.kiteui.views.ViewWriter
import kotlin.jvm.JvmInline
import kotlin.contracts.*

expect class NMenuButton : NView

@JvmInline
value class MenuButton(override val native: NMenuButton) : RView<NMenuButton>

@ViewDsl
expect fun ViewWriter.menuButtonActual(setup: MenuButton.()->Unit = {}): Unit
@OptIn(ExperimentalContracts::class) @ViewDsl inline fun ViewWriter.menuButton(noinline setup: MenuButton.() -> Unit = {}) { contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }; menuButtonActual(setup) }
expect fun MenuButton.opensMenu(action: ViewWriter.() -> Unit)
expect var MenuButton.enabled: Boolean
expect var MenuButton.requireClick: Boolean
expect var MenuButton.preferredDirection: PopoverPreferredDirection
