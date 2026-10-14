package com.lightningkite.kiteui.views.direct

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.selection.selectable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.lightningkite.kiteui.ExperimentalKiteUi
import com.lightningkite.kiteui.models.UnselectedSemantic
import com.lightningkite.kiteui.models.rem
import com.lightningkite.kiteui.models.toDp
import com.lightningkite.kiteui.views.*
import com.lightningkite.reactive.context.invoke
import com.lightningkite.reactive.context.reactive
import com.lightningkite.reactive.core.MutableReactiveValue
import com.lightningkite.reactive.core.Signal

@OptIn(ExperimentalKiteUi::class)
public actual class RadioButton actual constructor(context: ElementContext) : NativeInteractiveElement(context) {
    actual override val underlyingNativeElement: RadioButton get() = this
    override val driverValue: String? get() = radioDriverValue()
    override val driverActions: AiDriver.Actions get() = super.driverActions + radioDriverActions()

    public actual val checked: MutableReactiveValue<Boolean> = Signal(false)
    private var isChecked by mutableStateOf(false)

    init {
        // As on the web, where every check control gets the unselected theme, which has no background of its own.
        themePipeline.add(ThemePipeline.Step.elementStyling, UnselectedSemantic)
        reactive { isChecked = checked() }
    }

    // The web forces a radio button's padding to 0; it's a fixed-size circle.
    override fun refreshPadding() {}

    override fun inputModifier(): Modifier = Modifier.selectable(
        selected = isChecked,
        interactionSource = interactions.source,
        indication = null,
        enabled = enabled && !ignoreInteraction,
        role = Role.RadioButton,
        onClick = { checked.value = true },
    )

    @Composable
    override fun Content(modifier: Modifier) {
        interactions.Track()
        val theme = currentTheme.theme
        val ringColor = theme.icon.closestColor().toCompose()
        val dotColor = theme.foreground.closestColor().toCompose()
        val checked = isChecked
        Spacer(
            modifier
                .requiredSize(25.dp)
                .drawBehind {
                    val border = 0.1.rem.toDp().toPx()
                    drawCircle(ringColor, radius = (size.minDimension - border) / 2, style = Stroke(border))
                    if (checked) drawCircle(dotColor, radius = (size.minDimension - 2 * border) / 4)
                }
        )
    }
}
