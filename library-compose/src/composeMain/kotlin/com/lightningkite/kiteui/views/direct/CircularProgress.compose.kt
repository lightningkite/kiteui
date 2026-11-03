package com.lightningkite.kiteui.views.direct

import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.lightningkite.kiteui.models.rem
import com.lightningkite.kiteui.models.toDp
import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.NativeElement
import com.lightningkite.kiteui.views.toCompose

public actual class CircularProgress actual constructor(context: ElementContext) : NativeElement(context) {
    public actual var ratio: Float by mutableStateOf(0f)

    @Composable
    override fun Content(modifier: Modifier) {
        val theme = currentTheme.theme
        val fill = theme.foreground.closestColor().toCompose()
        val track = theme.background.closestColor().toCompose()
        val ratio = ratio.coerceIn(0f, 1f)
        Spacer(
            modifier
                // Square, like the web's aspect-ratio: 1, taking its side from whichever axis is fixed.
                .layout { measurable, constraints ->
                    val side = when {
                        constraints.hasFixedWidth -> constraints.maxWidth
                        constraints.hasFixedHeight -> constraints.maxHeight
                        else -> 2.rem.toDp().roundToPx().coerceIn(constraints.minWidth, constraints.maxWidth)
                    }
                    val placeable = measurable.measure(Constraints.fixed(side, side))
                    layout(side, side) { placeable.place(0, 0) }
                }
                .semantics { progressBarRangeInfo = ProgressBarRangeInfo(ratio, 0f..1f) }
                .drawBehind {
                    // The web's 3px ring: the arc runs clockwise from the top, over the nearest background color.
                    val width = 3.dp.toPx()
                    val arcSize = Size(size.minDimension - width, size.minDimension - width)
                    val topLeft = Offset((size.width - arcSize.width) / 2, (size.height - arcSize.height) / 2)
                    drawArc(track, 0f, 360f, false, topLeft, arcSize, style = Stroke(width))
                    drawArc(fill, -90f, 360f * ratio, false, topLeft, arcSize, style = Stroke(width))
                }
        )
    }
}
