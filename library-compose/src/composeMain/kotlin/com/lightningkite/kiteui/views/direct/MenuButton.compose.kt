package com.lightningkite.kiteui.views.direct

import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import com.lightningkite.kiteui.models.PopoverPreferredDirection
import com.lightningkite.kiteui.views.*
import com.lightningkite.reactive.context.invoke
import com.lightningkite.reactive.context.reactive

public actual class MenuButton actual constructor(context: ElementContext) : NativeInteractiveContainerElement(context) {
    override val driverActions: AiDriver.Actions get() = super.driverActions + menuDriverActions(click = createMenu?.let { ::openAndBlock })

    private val popover = Popover(source = this, anchor = this)
    private var createMenu: (Frame.() -> Unit)? = null

    public actual fun opensMenu(createMenu: Frame.() -> Unit) {
        this.createMenu = createMenu
    }

    public actual var requireClick: Boolean = false
    public actual var preferredDirection: PopoverPreferredDirection = PopoverPreferredDirection.belowCenter

    init {
        reactive {
            if (hovered() && !requireClick) createMenu?.let { popover.open(preferredDirection, it) }
        }
    }

    private fun openAndBlock() {
        popover.open(preferredDirection, createMenu ?: return)
        popover.block()
    }

    override fun inputModifier(): Modifier = Modifier.clickable(
        interactionSource = interactions.source,
        indication = null,
        enabled = enabled,
        onClick = ::openAndBlock,
    )

    @Composable
    override fun Content(modifier: Modifier) {
        Layout(content = { ShownChildren() }, modifier = modifier, measurePolicy = FrameMeasurePolicy)
    }
}
