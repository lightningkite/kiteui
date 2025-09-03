package com.lightningkite.kiteui.views.direct

import androidx.compose.runtime.Composable
import com.lightningkite.kiteui.models.PopoverPreferredDirection
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView

actual class MenuButton actual constructor(context: RContext) :
    RView(context) {
    actual fun opensMenu(createMenu: Frame.() -> Unit) {
    }

    actual var enabled: Boolean
        get() = TODO("Not yet implemented")
        set(value) {}
    actual var requireClick: Boolean
        get() = TODO("Not yet implemented")
        set(value) {}
    actual var preferredDirection: PopoverPreferredDirection
        get() = TODO("Not yet implemented")
        set(value) {}

    @Composable
    override fun compose() {
        TODO("Not yet implemented")
    }
}