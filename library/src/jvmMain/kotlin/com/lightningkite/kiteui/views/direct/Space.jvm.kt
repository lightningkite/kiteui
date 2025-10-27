package com.lightningkite.kiteui.views.direct

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView

actual class Space actual constructor(context: RContext, private val multiplier: Double) :
    RView(context) {
    @Composable
    override fun compose() {
        Spacer(Modifier.width((16 * multiplier).dp).height((16 * multiplier).dp))
    }
}