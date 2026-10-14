package com.lightningkite.kiteui.views.direct

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import com.lightningkite.kiteui.models.rem
import com.lightningkite.kiteui.models.toDp
import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.NativeElement
import com.lightningkite.kiteui.views.toCompose

public actual class ProgressBar actual constructor(context: ElementContext) : NativeElement(context) {
    public actual var ratio: Float by mutableStateOf(0f)

    @Composable
    override fun Content(modifier: Modifier) {
        val theme = currentTheme.theme
        val fill = theme.foreground.closestColor().toCompose()
        val track = theme.background.closestColor().toCompose()
        val ratio = ratio.coerceIn(0f, 1f)
        // The web's look: a bar at most 0.25rem tall (however tall its slot), the width of a browser's default
        // <progress>, with its track in the nearest background color.
        Spacer(
            modifier
                .requiredHeightIn(max = 0.25.rem.toDp())
                .size(10.rem.toDp(), 0.25.rem.toDp())
                .semantics { progressBarRangeInfo = ProgressBarRangeInfo(ratio, 0f..1f) }
                .drawBehind {
                    val radius = CornerRadius(size.height / 2)
                    drawRoundRect(track, cornerRadius = radius)
                    drawRoundRect(fill, size = Size(size.width * ratio, size.height), cornerRadius = radius)
                }
        )
    }
}
