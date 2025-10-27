package com.lightningkite.kiteui.views.direct

import androidx.compose.material.CircularProgressIndicator
import androidx.compose.runtime.Composable
import com.lightningkite.reactive.core.Signal
import com.lightningkite.kiteui.reactive.collectAsMutableState
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView

actual class CircularProgress actual constructor(context: RContext) :
    RView(context) {
    private val m_ratio = Signal<Float>(0f)
    actual var ratio: Float by m_ratio

    @Composable
    override fun compose() {
        val progress = m_ratio.collectAsMutableState()
        CircularProgressIndicator(progress.value)
    }
}