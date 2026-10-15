package com.lightningkite.kiteui.views.direct

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.lightningkite.kiteui.models.ThemeAndBack
import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.NativeElement
import com.lightningkite.kiteui.views.canvas.ComposeDrawingContext2D

public actual class Canvas actual constructor(context: ElementContext) : NativeElement(context) {
    private var redraws by mutableIntStateOf(0)

    public actual var delegate: CanvasDelegate? = null
        set(value) {
            field?.invalidate = {}
            field = value
            value?.theme = themeAndBack.theme
            value?.invalidate = { redraws++ }
            redraws++
        }

    override fun nativeApplyTheme(theme: ThemeAndBack) {
        super.nativeApplyTheme(theme)
        delegate?.theme = theme.theme
        redraws++
    }

    @Composable
    override fun Content(modifier: Modifier) {
        val textMeasurer = rememberTextMeasurer()
        val focusRequester = remember { FocusRequester() }
        Spacer(
            modifier
                // An HTML canvas's default size.
                .size(300.dp, 150.dp)
                .onSizeChanged { delegate?.onResize(it.width.toDouble(), it.height.toDouble()) }
                .onKeyEvent {
                    val delegate = delegate ?: return@onKeyEvent false
                    when (it.type) {
                        KeyEventType.KeyDown -> delegate.onKeyDown(it.key.keyCode)
                        KeyEventType.KeyUp -> delegate.onKeyUp(it.key.keyCode)
                        else -> false
                    }
                }
                // Focusable like the web's canvas (tabIndex), which a click also focuses.
                .focusRequester(focusRequester)
                .focusable()
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            if (event.type == PointerEventType.Press) focusRequester.requestFocus()
                            val delegate = delegate ?: continue
                            val width = size.width.toDouble()
                            val height = size.height.toDouble()
                            for (change in event.changes) {
                                val id = change.id.value.toInt()
                                val x = change.position.x.toDouble()
                                val y = change.position.y.toDouble()
                                val handled = when (event.type) {
                                    PointerEventType.Press -> delegate.onPointerDown(id, x, y, width, height)
                                    PointerEventType.Move -> delegate.onPointerMove(id, x, y, width, height)
                                    PointerEventType.Release -> delegate.onPointerUp(id, x, y, width, height)
                                    // The web cancels on pointerleave too.
                                    PointerEventType.Exit -> delegate.onPointerCancel(id, x, y, width, height)
                                    PointerEventType.Scroll -> delegate.onWheel(change.scrollDelta.x.toDouble(), change.scrollDelta.y.toDouble(), 0.0)
                                    else -> false
                                }
                                if (handled) change.consume()
                            }
                        }
                    }
                }
                // Offscreen, so clearing makes pixels transparent rather than clearing through to the window.
                .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen, clip = true)
                .drawBehind {
                    redraws // Read, so that invalidate() redraws.
                    delegate?.draw(ComposeDrawingContext2D(this, textMeasurer))
                }
        )
    }
}
