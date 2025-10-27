package com.lightningkite.kiteui.views.direct

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.material.DropdownMenu
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.lightningkite.kiteui.models.PopoverPreferredDirection
import com.lightningkite.reactive.core.Signal
import com.lightningkite.kiteui.reactive.collectAsMutableState
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView

actual class MenuButton actual constructor(context: RContext) :
    RView(context) {
    private var createMenu: (Frame.() -> Unit)? = null

    actual fun opensMenu(createMenu: Frame.() -> Unit) {
        this.createMenu = createMenu
    }

    private val m_enabled = Signal(true)
    actual var enabled: Boolean by m_enabled

    private val m_requireClick = Signal(true)
    actual var requireClick: Boolean by m_requireClick

    private val m_preferredDirection = Signal(PopoverPreferredDirection.belowRight)
    actual var preferredDirection: PopoverPreferredDirection by m_preferredDirection

    @Composable
    override fun compose() {
        val enabled = m_enabled.collectAsMutableState()
        val requireClick = m_requireClick.collectAsMutableState()
        val preferredDirection = m_preferredDirection.collectAsMutableState()

        var expanded by remember { mutableStateOf(false) }

        Box(
            modifier = Modifier.clickable(enabled = enabled.value) {
                expanded = true
            }
        ) {
            children.forEach { 
                it.compose()
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                createMenu?.let { menuBuilder ->
                    val frame = Frame(context)
                    menuBuilder(frame)
                    frame.children.forEach { child ->
                        child.compose()
                    }
                }
            }
        }
    }
}