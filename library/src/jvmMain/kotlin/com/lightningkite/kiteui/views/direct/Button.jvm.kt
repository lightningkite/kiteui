package com.lightningkite.kiteui.views.direct

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.lightningkite.kiteui.models.CornerRadii
import com.lightningkite.kiteui.models.px
import com.lightningkite.reactive.core.Signal
import com.lightningkite.kiteui.reactive.collectAsMutableState
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RViewWithSecondaryAction
import kotlinx.coroutines.launch

actual class Button actual constructor(context: RContext) :
    RViewWithSecondaryAction(context) {
    val m_enabled = Signal(true)
    actual var enabled: Boolean by m_enabled

    @Composable
    override fun compose() {
        val enabled = m_enabled.collectAsMutableState()
        val scope = rememberCoroutineScope()

        Button(
            onClick = {},
            enabled = enabled.value,
            modifier = Modifier.combinedClickable(
                enabled = enabled.value,
                onClick = {
                    this.action?.let { action ->
                        scope.launch {
                            action.invoke()
                        }
                    }
                },
                onLongClick = {
                    this.secondaryAction?.let { action ->
                        scope.launch {
                            action.invoke()
                        }
                    }
                }
            ),
            elevation = ButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp),
            shape = getShape(CornerRadii.Constant(8.px)),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = Color.Transparent,
                contentColor = Color.Transparent,
                disabledBackgroundColor = Color.Transparent,
                disabledContentColor = Color.Transparent
            ),
            contentPadding = PaddingValues(0.dp)
        ) {
            Row(
                modifier = Modifier.align(Alignment.CenterVertically),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
            ) {
                children.forEach {
                    it.compose()
                }
            }
        }
    }
}