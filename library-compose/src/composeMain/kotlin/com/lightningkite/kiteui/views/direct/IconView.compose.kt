package com.lightningkite.kiteui.views.direct

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.lightningkite.kiteui.models.Icon
import com.lightningkite.kiteui.models.toDp
import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.NativeElement
import com.lightningkite.kiteui.views.toBrush
import com.lightningkite.kiteui.views.toCompose

public actual class IconView actual constructor(context: ElementContext) : NativeElement(context) {
    public actual var source: Icon? by mutableStateOf(null)
    public actual var description: String? by mutableStateOf(null)

    @Composable
    override fun Content(modifier: Modifier) {
        val icon = source
        val description = description
        val paint = currentTheme.theme.icon
        if (icon == null) {
            Spacer(modifier)
            return
        }
        val fills = remember(icon) { icon.pathDatas.map { PathParser().parsePathString(it).toPath() } }
        val strokes = remember(icon) { icon.strokePathDatas.map { it to PathParser().parsePathString(it.path).toPath() } }
        val brush = paint.toBrush()
        val strokeColor = paint.closestColor().toCompose()
        Spacer(
            modifier
                .size(icon.width.toDp(), icon.height.toDp())
                .semantics { description?.let { contentDescription = it } }
                .drawBehind {
                    // A stretched IconView is bigger than its icon; draw the icon at its own size, centered.
                    val iconWidth = icon.width.toDp().toPx()
                    val iconHeight = icon.height.toDp().toPx()
                    withTransform({
                        translate((size.width - iconWidth) / 2, (size.height - iconHeight) / 2)
                        scale(iconWidth / icon.viewBoxWidth, iconHeight / icon.viewBoxHeight, pivot = Offset.Zero)
                        translate(-icon.viewBoxMinX.toFloat(), -icon.viewBoxMinY.toFloat())
                    }) {
                        for (path in fills) drawPath(path, brush)
                        for ((data, path) in strokes) {
                            data.fill?.let { drawPath(path, it.toBrush()) }
                            if (data.strokeWidth.value > 0f) drawPath(
                                path, strokeColor,
                                style = Stroke(
                                    width = data.strokeWidth.value,
                                    cap = when (data.strokeLineCap) {
                                        Icon.StrokeLineCap.Butt -> StrokeCap.Butt
                                        Icon.StrokeLineCap.Round -> StrokeCap.Round
                                        Icon.StrokeLineCap.Square -> StrokeCap.Square
                                    },
                                ),
                            )
                        }
                    }
                }
        )
    }
}
