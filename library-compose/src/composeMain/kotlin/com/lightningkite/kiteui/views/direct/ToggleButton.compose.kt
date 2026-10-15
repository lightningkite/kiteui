package com.lightningkite.kiteui.views.direct

import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.Role
import com.lightningkite.kiteui.ExperimentalKiteUi
import com.lightningkite.kiteui.models.SelectedSemantic
import com.lightningkite.kiteui.models.UnselectedSemantic
import com.lightningkite.kiteui.views.*
import com.lightningkite.reactive.context.invoke
import com.lightningkite.reactive.context.reactive
import com.lightningkite.reactive.core.MutableReactiveValue
import com.lightningkite.reactive.core.Signal

@OptIn(ExperimentalKiteUi::class)
public actual class ToggleButton actual constructor(context: ElementContext) : NativeInteractiveContainerElement(context) {
    override val driverValue: String? get() = toggleDriverValue()
    override val driverActions: AiDriver.Actions get() = super.driverActions + toggleDriverActions()

    public actual val checked: MutableReactiveValue<Boolean> = Signal(false)
    private var isChecked by mutableStateOf(false)

    init {
        themePipeline.add(ThemePipeline.Step.elementStatus) { if (checked.value) SelectedSemantic else UnselectedSemantic }
        reactive {
            isChecked = checked()
            refreshTheming()
        }
    }

    override fun inputModifier(): Modifier = Modifier.toggleable(
        value = isChecked,
        interactionSource = interactions.source,
        indication = null,
        enabled = enabled,
        role = Role.Button,
        onValueChange = { checked.value = it },
    )

    @Composable
    override fun Content(modifier: Modifier) {
        Layout(content = { ShownChildren() }, modifier = modifier, measurePolicy = FrameMeasurePolicy)
    }
}
