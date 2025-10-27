package com.lightningkite.kiteui.views.direct

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.lightningkite.reactive.core.Signal
import com.lightningkite.kiteui.reactive.collectAsMutableState
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView
import kotlinx.coroutines.launch
import java.awt.Desktop
import java.net.URI

actual class ExternalLink actual constructor(context: RContext) :
    RView(context) {
    private val m_enabled = Signal(true)
    actual var enabled: Boolean by m_enabled

    private val m_to = Signal<String?>(null)
    actual var to: String? by m_to

    private val m_newTab = Signal(false)
    actual var newTab: Boolean by m_newTab

    private var onNavigateAction: (suspend () -> Unit)? = null

    @Composable
    override fun compose() {
        val enabled = m_enabled.collectAsMutableState()
        val to = m_to.collectAsMutableState()
        val scope = rememberCoroutineScope()

        Row(
            modifier = Modifier.clickable(enabled = enabled.value) {
                scope.launch {
                    to.value?.let {
                        try {
                            Desktop.getDesktop().browse(URI.create(it))
                            onNavigateAction?.invoke()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        ) {
            children.forEach {
                it.compose()
            }
        }
    }

    actual fun onNavigate(action: suspend () -> Unit) {
        this.onNavigateAction = action
    }
}