package com.lightningkite.kiteui.views.direct

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView

actual class Frame actual constructor(context: RContext) :
    RView(context) {
    @Composable
    override fun compose() {
        Box(modifier = Modifier) {
            children.forEach { 
                it.compose()
            }
        }
    }
}