package com.lightningkite.kiteui.views.direct

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import com.lightningkite.kiteui.models.ThemeAndBack
import com.lightningkite.kiteui.models.rem
import com.lightningkite.kiteui.models.toDp
import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.NativeElement
import com.lightningkite.kiteui.views.toCompose

public actual class ProgressBar actual constructor(context: ElementContext) : NativeElement(context) {
    private val shownRatio = mutableStateOf(0f)
    public actual var ratio: Float
        get() = shownRatio.value
        set(value) {
            // As the web's <progress>, whose value throws on a non-finite number.  Compose would otherwise only
            // find it while drawing, which a composition can't recover from.
            require(value.isFinite()) { "ProgressBar.ratio must be a finite number, but was $value" }
            shownRatio.value = value
        }

    // The web's <progress> is always bare: its theme colors the bar, but it draws no background box or padding.
    override fun nativeApplyTheme(theme: ThemeAndBack) {
        super.nativeApplyTheme(theme.copy(drawBackground = false))
    }

    override fun refreshPadding() {}

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
