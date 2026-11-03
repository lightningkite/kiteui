package com.lightningkite.kiteui.views.direct

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
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
    override fun inputModifier(takesInput: Boolean): Modifier = Modifier
        // As on the web, Ctrl, Meta or Shift with Enter or Space runs the secondary action.  Both the press and the
        // release are taken, so the click below doesn't run too.
        .onPreviewKeyEvent {
            val modified = it.isCtrlPressed || it.isMetaPressed || it.isShiftPressed
            if (!enabled || !takesInput || !modified || (it.key != Key.Enter && it.key != Key.NumPadEnter && it.key != Key.Spacebar)) return@onPreviewKeyEvent false
            if (it.type == KeyEventType.KeyDown) (secondaryAction ?: action)?.startAction(this)
            true
        }
        .combinedClickable(
            interactionSource = interactions.source,
            indication = null,
            enabled = enabled && takesInput,
            onLongClick = if (hasSecondaryAction) ({ secondaryAction?.startAction(this) }) else null,
            onClick = { action?.startAction(this) },
        )

    @Composable
    override fun Content(modifier: Modifier) {
        Layout(content = { ShownChildren() }, modifier = modifier, measurePolicy = FrameMeasurePolicy)
    }
}
