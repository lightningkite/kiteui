package com.lightningkite.kiteui.views.direct

import androidx.compose.runtime.Composable
import com.lightningkite.reactive.core.Signal
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView

actual class WebView actual constructor(context: RContext) :
    RView(context) {
    private val m_url = Signal("")
    actual var url: String by m_url

    private val m_permitJs = Signal(true)
    actual var permitJs: Boolean by m_permitJs

    private val m_content = Signal("")
    actual var content: String by m_content

    @Composable
    override fun compose() {
        // TODO: Implement WebView with compose-webview-multiplatform library
        // For now, placeholder implementation
    }
}