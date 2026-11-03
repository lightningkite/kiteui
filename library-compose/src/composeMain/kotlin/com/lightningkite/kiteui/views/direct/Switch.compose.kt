package com.lightningkite.kiteui.views.direct

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.lightningkite.kiteui.ExperimentalKiteUi
import com.lightningkite.kiteui.models.ThemeAndBack
import com.lightningkite.kiteui.models.UnselectedSemantic
import com.lightningkite.kiteui.models.rem
import com.lightningkite.kiteui.models.toDp
import com.lightningkite.kiteui.views.*
import com.lightningkite.reactive.context.invoke
import com.lightningkite.reactive.context.reactive
import com.lightningkite.reactive.core.MutableReactiveValue
import com.lightningkite.reactive.core.Signal

@OptIn(ExperimentalKiteUi::class)
public actual class Switch actual constructor(context: ElementContext) : NativeInteractiveElement(context) {
    override val driverValue: String? get() = switchDriverValue()
    override val driverActions: AiDriver.Actions get() = super.driverActions + switchDriverActions()

    public actual val checked: MutableReactiveValue<Boolean> = Signal(false)
    private var isChecked by mutableStateOf(false)

    init {
        // As on the web, where every check control gets the unselected theme.
        themePipeline.add(ThemePipeline.Step.elementStyling, UnselectedSemantic)
        reactive { isChecked = checked() }
    }

    // The web's switch overrides the theme's background and padding with its own track.
    override fun nativeApplyTheme(theme: ThemeAndBack) {
        super.nativeApplyTheme(theme.copy(drawBackground = false))
    }

    override fun refreshPadding() {}

    override fun inputModifier(takesInput: Boolean): Modifier = Modifier.toggleable(
        value = isChecked,
        interactionSource = interactions.source,
        indication = null,
        enabled = enabled && takesInput,
        role = Role.Switch,
        onValueChange = { checked.value = it },
    )

    @Composable
    override fun Content(modifier: Modifier) {
        val foreground = currentTheme.theme.foreground.closestColor().toCompose()
        val checked = isChecked
        // The web's colors and geometry, including its fixed green: the thumb overhangs the track slightly.
        Spacer(
            modifier
                .requiredSize(3.rem.toDp(), 1.5.rem.toDp())
                .drawBehind {
                    drawRoundRect(
                        color = if (checked) Color(0xFF20A020) else foreground.copy(alpha = foreground.alpha * 0.2f),
                        cornerRadius = CornerRadius(size.height / 2),
                    )
                    val thumbRadius = 1.6.rem.toDp().toPx() / 2
                    val thumbLeft = if (checked) 1.4.rem.toDp().toPx() else -0.1.rem.toDp().toPx()
                    val center = Offset(thumbLeft + thumbRadius, -0.1.rem.toDp().toPx() + thumbRadius)
                    drawCircle(Color.White, thumbRadius, center)
                    drawCircle(Color(100, 116, 139).copy(alpha = 0.527f), thumbRadius, center, style = Stroke(1.dp.toPx()))
                }
        )
    }
}
