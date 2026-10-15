package com.lightningkite.kiteui.views.direct

import androidx.compose.foundation.focusable
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
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
import com.lightningkite.kiteui.models.rem
import com.lightningkite.kiteui.models.toDp
import com.lightningkite.kiteui.views.*
import com.lightningkite.reactive.context.invoke
import com.lightningkite.reactive.context.reactive
import com.lightningkite.reactive.core.MutableReactiveValue
import com.lightningkite.reactive.core.Release
import com.lightningkite.reactive.core.Signal
import kotlin.math.roundToInt

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
                current.value = snapped(value)
            }

        override fun addListener(listener: () -> Unit): Release = current.addListener(listener)
    }

    public actual var min: Float = 0f
        set(value) {
            field = value
            this.value.value = this.value.value
        }
    public actual var max: Float = 1f
        set(value) {
            field = value
            this.value.value = this.value.value
        }
    public actual var step: Float? = null
        set(value) {
            field = value
            this.value.value = this.value.value
        }

    init {
        reactive { displayed = value() }
    }

    private fun snapped(v: Float): Float {
        val clamped = v.coerceIn(min, max)
        val step = step ?: return clamped
        return (min + ((clamped - min) / step).roundToInt() * step).coerceAtMost(max)
    }

    // The thumb's center travels from one radius in from the left edge to one radius in from the right.
    private val thumbRadius = 0.5.rem.toDp()

    // Padding would only narrow the track, and the pointer handling below assumes the track spans the element.
    override fun refreshPadding() {}

    override fun inputModifier(): Modifier {
        if (!enabled) return Modifier
        val keyStep = step ?: ((max - min) / 100)
        return Modifier
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(displayed, min..max)
                setProgress { value.value = it; true }
            }
            .focusable(interactionSource = interactions.source)
            .onKeyEvent {
                if (it.type != KeyEventType.KeyDown) return@onKeyEvent false
                when (it.key) {
                    Key.DirectionLeft, Key.DirectionDown -> value.value -= keyStep
                    Key.DirectionRight, Key.DirectionUp -> value.value += keyStep
                    else -> return@onKeyEvent false
                }
                true
            }
            .pointerInput(Unit) {
                fun setFrom(x: Float) {
                    val radius = thumbRadius.toPx()
                    val fraction = ((x - radius) / (size.width - 2 * radius)).coerceIn(0f, 1f)
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
        val fraction = (displayed - min) / (max - min)
        // 8rem is about a browser's default range input width; stretched sliders fill their slot instead.
        Spacer(
            modifier
                .size(8.rem.toDp(), thumbRadius * 2)
                .drawBehind {
                    val radius = thumbRadius.toPx()
                    val trackHeight = 0.25.rem.toDp().toPx()
                    val trackTop = (size.height - trackHeight) / 2
                    val thumbX = radius + fraction * (size.width - 2 * radius)
                    drawRoundRect(
                        color = color.copy(alpha = color.alpha * 0.25f),
                        topLeft = Offset(0f, trackTop),
                        size = Size(size.width, trackHeight),
                        cornerRadius = CornerRadius(trackHeight / 2),
                    )
                    drawRoundRect(
                        color = color,
                        topLeft = Offset(0f, trackTop),
                        size = Size(thumbX, trackHeight),
                        cornerRadius = CornerRadius(trackHeight / 2),
                    )
                    drawCircle(color, radius, Offset(thumbX, size.height / 2))
                }
        )
    }
}
