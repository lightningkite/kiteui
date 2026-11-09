package com.lightningkite.kiteui.views.direct

import androidx.compose.foundation.focusable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.dp
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import com.lightningkite.kiteui.models.Edges
import com.lightningkite.kiteui.models.rem
import com.lightningkite.kiteui.models.toDp
import com.lightningkite.kiteui.views.*
import com.lightningkite.reactive.context.invoke
import com.lightningkite.reactive.context.reactive
import com.lightningkite.reactive.core.MutableReactiveValue
import com.lightningkite.reactive.core.Release
import com.lightningkite.reactive.core.Signal

public actual class Slider actual constructor(context: ElementContext) : NativeInteractiveElement(context) {
    override val driverValue: String? get() = sliderDriverValue()
    override val driverActions: AiDriver.Actions get() = super.driverActions + sliderDriverActions()

    private val current = Signal(0.5f)
    private var displayed by mutableStateOf(current.value)

    // Like the web's range input, the value always lies within min..max and on a step.
    public actual val value: MutableReactiveValue<Float> = object : MutableReactiveValue<Float> {
        override var value: Float
            get() = current.value
            set(value) {
                current.value = sliderValue(value, min, max, step)
            }

        override fun addListener(listener: () -> Unit): Release = current.addListener(listener)
    }

    private var minState by mutableStateOf(0f)
    private var maxState by mutableStateOf(1f)
    private var stepState by mutableStateOf<Float?>(null)

    public actual var min: Float
        get() = minState
        set(value) {
            minState = value
            this.value.value = this.value.value
        }
    public actual var max: Float
        get() = maxState
        set(value) {
            maxState = value
            this.value.value = this.value.value
        }
    public actual var step: Float?
        get() = stepState
        set(value) {
            stepState = value.validSliderStep()
            this.value.value = this.value.value
        }

    init {
        reactive { displayed = value() }
    }

    // Drawn as Chrome draws the web's range input.  Its thumb is a 16px box whose center travels across the content
    // box, from one radius in from its left edge to one radius in from its right.
    private val thumbRadius = 8.dp

    // On the web, padding narrows only the thumb's travel: the track still spans the whole element.
    private var padding by mutableStateOf(Edges.ZERO)
    override fun refreshPadding() {
        padding = appliedPadding
    }

    override fun inputModifier(takesInput: Boolean): Modifier {
        val active = enabled && takesInput
        val keyStep = step ?: ((max - min) / 100)
        return Modifier
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(displayed, min..max)
                if (active) setProgress { value.value = it; true }
            }
            .focusProperties { canFocus = active }
            .focusable(interactionSource = interactions.source)
            .hoverable(interactions.source, enabled = active)
            .onKeyEvent {
                if (!active || it.type != KeyEventType.KeyDown) return@onKeyEvent false
                when (it.key) {
                    Key.DirectionLeft, Key.DirectionDown -> value.value -= keyStep
                    Key.DirectionRight, Key.DirectionUp -> value.value += keyStep
                    else -> return@onKeyEvent false
                }
                true
            }
            .pointerInput(active) {
                if (!active) return@pointerInput
                fun setFrom(x: Float) {
                    val start = padding.left.toDp().toPx() + thumbRadius.toPx()
                    val end = size.width - padding.right.toDp().toPx() - thumbRadius.toPx()
                    val fraction = ((x - start) / (end - start)).coerceIn(0f, 1f)
                    value.value = min + fraction * (max - min)
                }
                awaitEachGesture {
                    val down = awaitFirstDown()
                    setFrom(down.position.x)
                    drag(down.id) {
                        setFrom(it.position.x)
                        it.consume()
                    }
                }
            }
    }

    @Composable
    override fun Content(modifier: Modifier) {
        val color = currentTheme.theme.foreground.closestColor().toCompose()
        // An empty range has its thumb at the start, as on the web.
        val fraction = if (max > min) (displayed - min) / (max - min) else 0f
        val padding = padding
        val left = padding.left.toDp()
        val top = padding.top.toDp()
        val right = padding.right.toDp()
        val bottom = padding.bottom.toDp()
        // 8rem is about a browser's default range input width; stretched sliders fill their slot instead.
        Spacer(
            modifier
                .size(8.rem.toDp() + left + right, thumbRadius * 2 + top + bottom)
                .drawBehind {
                    val start = left.toPx() + thumbRadius.toPx()
                    val end = size.width - right.toPx() - thumbRadius.toPx()
                    val thumbX = start + fraction * (end - start)
                    // Chrome's track is 8px tall, centered in the whole element and a pixel short of each end.
                    val track = RoundRect(
                        left = 1.dp.toPx(),
                        top = size.height / 2 - 4.dp.toPx(),
                        right = size.width - 1.dp.toPx(),
                        bottom = size.height / 2 + 4.dp.toPx(),
                        cornerRadius = CornerRadius(4.dp.toPx()),
                    )
                    val trackPath = Path().apply { addRoundRect(track) }
                    drawPath(trackPath, Color(0xFFEFEFEF))
                    clipPath(trackPath) {
                        drawRect(color, Offset(track.left, track.top), Size(thumbX - track.left, track.height))
                    }
                    // Chrome's #767676 border at half opacity, drawn over the fill.
                    val border = 1.dp.toPx()
                    drawRoundRect(
                        color = Color(0x80767676),
                        topLeft = Offset(track.left + border / 2, track.top + border / 2),
                        size = Size(track.width - border, track.height - border),
                        cornerRadius = CornerRadius(track.height / 2 - border / 2),
                        style = Stroke(border),
                    )
                    // Chrome insets the thumb's circle by half a pixel.
                    val thumbY = top.toPx() + (size.height - top.toPx() - bottom.toPx()) / 2
                    drawCircle(color, thumbRadius.toPx() - 0.5.dp.toPx(), Offset(thumbX, thumbY))
                }
        )
    }
}
