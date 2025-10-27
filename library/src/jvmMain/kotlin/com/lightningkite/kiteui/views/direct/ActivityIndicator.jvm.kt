package com.lightningkite.kiteui.views.direct

import androidx.compose.material.CircularProgressIndicator
import androidx.compose.runtime.Composable
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView

actual class ActivityIndicator actual constructor(context: RContext) :
    RView(context) {
    @Composable
    override fun compose() {
        CircularProgressIndicator()
    }
}