package com.lightningkite.kiteui.views.direct

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.navigation.*
import com.lightningkite.kiteui.reactive.Action
import com.lightningkite.kiteui.views.*
import com.lightningkite.reactive.core.*
import kotlinx.datetime.*
import kotlin.time.Duration

public actual class MenuButton actual constructor(context: ElementContext) : NativeInteractiveContainerElement(context) {
    init { throw NotImplementedError("Compose backend: MenuButton is not implemented yet") }

    public actual fun opensMenu(createMenu: Frame.() -> Unit) {}
    public actual var requireClick: Boolean = false
    public actual var preferredDirection: PopoverPreferredDirection = PopoverPreferredDirection.belowRight

    @Composable
    override fun Content(modifier: Modifier) {}
}
