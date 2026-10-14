package com.lightningkite.kiteui.views.direct

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import com.lightningkite.kiteui.reactive.Action
import com.lightningkite.kiteui.views.AiDriver
import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.FrameMeasurePolicy
import com.lightningkite.kiteui.views.NativeContainerElementWithSecondaryAction

public actual class Button actual constructor(context: ElementContext) : NativeContainerElementWithSecondaryAction(context) {
    override val driverActions: AiDriver.Actions get() = super.driverActions + buttonDriverActions()

    private var hasSecondaryAction by mutableStateOf(false)

    override fun nativeSetSecondaryAction(action: Action?) {
        hasSecondaryAction = action != null
    }

    @OptIn(ExperimentalFoundationApi::class)
    override fun inputModifier(): Modifier = Modifier.combinedClickable(
        interactionSource = interactions.source,
        indication = null,
        enabled = enabled && !ignoreInteraction,
        onLongClick = if (hasSecondaryAction) ({ secondaryAction?.startAction(this) }) else null,
        onClick = { action?.startAction(this) },
    )

    @Composable
    override fun Content(modifier: Modifier) {
        interactions.Track()
        Layout(content = { ShownChildren() }, modifier = modifier, measurePolicy = FrameMeasurePolicy)
    }
}
