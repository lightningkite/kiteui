package com.lightningkite.mppexampleapp.widgets

import androidx.compose.runtime.Composable
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView
import com.lightningkite.kiteui.views.direct.TextView

actual class Code actual constructor(context: RContext) : RView(context) {
    val real = TextView(context)
    actual var content: String = ""
        set(value) {
            field = value
            real.content = value
        }

    @Composable
    override fun compose() {
        return real.compose()
    }
}