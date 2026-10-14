package com.lightningkite.kiteui.views.direct

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.NativeElement
import com.lightningkite.kiteui.views.toCompose

public actual class ActivityIndicator actual constructor(context: ElementContext) : NativeElement(context) {
    @Composable
    override fun Content(modifier: Modifier) {
        val color = currentTheme.theme.foreground.closestColor().toCompose()
        val turn = rememberInfiniteTransition().animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Restart),
        )
        // The web's spinner: a 32px ring with one quarter missing, at half opacity, turning every 2 seconds.
        Spacer(
            modifier
                .requiredSize(32.dp)
                .semantics { contentDescription = "Loading" }
                .drawBehind {
                    val width = 5.dp.toPx()
                    drawArc(
                        color = color.copy(alpha = color.alpha * 0.5f),
                        startAngle = turn.value - 45f,
                        sweepAngle = 270f,
                        useCenter = false,
                        topLeft = Offset(width / 2, width / 2),
                        size = Size(size.width - width, size.height - width),
                        style = Stroke(width),
                    )
                }
        )
    }
}
