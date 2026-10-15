package com.lightningkite.kiteui.views.direct

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
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
public actual class Checkbox actual constructor(context: ElementContext) : NativeInteractiveElement(context) {
    actual override val underlyingNativeElement: Checkbox get() = this
    override val driverValue: String? get() = checkboxDriverValue()
    override val driverActions: AiDriver.Actions get() = super.driverActions + checkboxDriverActions()

    public actual val checked: MutableReactiveValue<Boolean> = Signal(false)
    private var isChecked by mutableStateOf(false)

    init {
        // As on the web, where every check control gets the unselected theme, which has no background of its own.
        themePipeline.add(ThemePipeline.Step.elementStyling, UnselectedSemantic)
        reactive { isChecked = checked() }
    }

    // The web forces a checkbox's padding to 0; it's a fixed-size box.
    override fun refreshPadding() {}

    override fun inputModifier(): Modifier = Modifier.toggleable(
        value = isChecked,
        interactionSource = interactions.source,
        indication = null,
        enabled = enabled,
        role = Role.Checkbox,
        onValueChange = { checked.value = it },
    )

    @Composable
    override fun Content(modifier: Modifier) {
        val theme = currentTheme.theme
        val boxColor = theme.icon.closestColor().toCompose()
        val markColor = theme.foreground.closestColor().toCompose()
        val checked = isChecked
        // Drawn as the web's CSS draws it: a box with a thin border, and for the mark, the left and bottom
        // borders of a small box rotated 45 degrees.
        Spacer(
            modifier
                .requiredSize(25.dp)
                .alpha(if (checked) 1f else 0.75f)
                .drawBehind {
                    val border = 0.1.rem.toDp().toPx()
                    drawRoundRect(
                        color = boxColor,
                        topLeft = Offset(border / 2, border / 2),
                        size = Size(size.width - border, size.height - border),
                        cornerRadius = CornerRadius(size.width * 0.2f),
                        style = Stroke(border),
                    )
                    if (!checked) return@drawBehind
                    // The CSS box's borders sit outside its width and height.
                    val inner = size.width - 2 * border
                    val stroke = 0.2.rem.toDp().toPx()
                    val left = border + inner * 0.2f
                    val top = border + inner * 0.25f
                    val right = left + inner * 0.5f + stroke
                    val bottom = top + inner * 0.2f + stroke
                    rotate(-45f, pivot = Offset((left + right) / 2, (top + bottom) / 2)) {
                        val mark = Path().apply {
                            moveTo(left + stroke / 2, top)
                            lineTo(left + stroke / 2, bottom - stroke / 2)
                            lineTo(right, bottom - stroke / 2)
                        }
                        drawPath(mark, markColor, style = Stroke(stroke))
                    }
                }
        )
    }
}
