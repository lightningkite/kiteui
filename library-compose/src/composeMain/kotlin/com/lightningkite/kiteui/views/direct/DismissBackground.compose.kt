package com.lightningkite.kiteui.views.direct

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import com.lightningkite.kiteui.ExperimentalKiteUi
import com.lightningkite.kiteui.models.DismissSemantic
import com.lightningkite.kiteui.views.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalKiteUi::class)
public actual class DismissBackground actual constructor(context: ElementContext) : NativeContainerElement(context) {
    init {
        themePipeline.add(ThemePipeline.Step.elementStyling, DismissSemantic)
    }

    private var onClick: (suspend () -> Unit)? by mutableStateOf(null)

    public actual fun onClick(action: suspend () -> Unit) {
        onClick = action
    }

    override fun inputModifier(): Modifier = onClick?.let { action ->
        Modifier.clickable(interactionSource = null, indication = null) { launch { action() } }
    } ?: Modifier

    @Composable
    override fun Content(modifier: Modifier) {
        Layout(
            content = {
                for (child in nativeChildren) {
                    // Like the web's stopPropagation: a click anywhere on a child, even on a part that doesn't
                    // handle clicks itself, is not a click on the background.
                    if (child.takesSpace) key(child) {
                        child.Render(Modifier.layoutId(child).pointerInput(Unit) {
                            awaitEachGesture {
                                awaitFirstDown(requireUnconsumed = false)
                                waitForUpOrCancellation()?.consume()
                            }
                        })
                    }
                }
            },
            modifier = modifier,
            measurePolicy = FrameMeasurePolicy,
        )
    }
}
