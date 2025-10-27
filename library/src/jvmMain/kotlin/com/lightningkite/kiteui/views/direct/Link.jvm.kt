package com.lightningkite.kiteui.views.direct

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.navigation.PageNavigator
import com.lightningkite.kiteui.navigation.Routes
import com.lightningkite.reactive.core.Signal
import com.lightningkite.kiteui.reactive.collectAsMutableState
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView
import kotlinx.coroutines.launch

actual class Link actual constructor(context: RContext) :
    RView(context) {
    private val m_enabled = Signal(true)
    actual var enabled: Boolean by m_enabled

    private val m_to = Signal<(() -> Page)?>(null)
    actual var to: (() -> Page)? by m_to

    // TODO: Get from context properly
    private val m_onNavigator = Signal<PageNavigator>(PageNavigator { Routes(listOf(), mapOf()) })
    actual var onNavigator: PageNavigator by m_onNavigator

    private val m_newTab = Signal(false)
    actual var newTab: Boolean by m_newTab

    private val m_resetsStack = Signal(false)
    actual var resetsStack: Boolean by m_resetsStack

    private var onClickAction: (suspend () -> Unit)? = null
    private var onNavigateAction: (suspend () -> Unit)? = null

    @Composable
    override fun compose() {
        val enabled = m_enabled.collectAsMutableState()
        val to = m_to.collectAsMutableState()
        val onNavigator = m_onNavigator.collectAsMutableState()
        val resetsStack = m_resetsStack.collectAsMutableState()
        val scope = rememberCoroutineScope()

        Row(
            modifier = Modifier.clickable(enabled = enabled.value) {
                scope.launch {
                    onClickAction?.invoke()

                    to.value?.let {
                        if (resetsStack.value) {
                            onNavigator.value.reset(it())
                        } else {
                            onNavigator.value.navigate(it())
                        }
                    }

                    onNavigateAction?.invoke()
                }
            }
        ) {
            children.forEach {
                it.compose()
            }
        }
    }

    actual fun onClick(action: suspend () -> Unit) {
        this.onClickAction = action
    }

    actual fun onNavigate(action: suspend () -> Unit) {
        this.onNavigateAction = action
    }
}