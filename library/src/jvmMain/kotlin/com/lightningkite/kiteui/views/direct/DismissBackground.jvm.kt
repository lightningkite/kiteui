package com.lightningkite.kiteui.views.direct

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView
import kotlinx.coroutines.launch

actual class DismissBackground actual constructor(context: RContext) :
    RView(context) {
    private var onClickAction: (suspend () -> Unit)? = null

    actual fun onClick(action: suspend () -> Unit) {
        this.onClickAction = action
    }

    @Composable
    override fun compose() {
        val scope = rememberCoroutineScope()

        Box(modifier = Modifier.clickable {
            onClickAction?.let { action ->
                scope.launch {
                    action()
                }
            }
        }) {
            children.forEach {
                it.compose()
            }
        }
    }
}