package com.lightningkite.kiteui.views.direct

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lightningkite.kiteui.models.times
import com.lightningkite.kiteui.models.toDp
import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.NativeElement

public actual class Space actual constructor(context: ElementContext, public val multiplier: Double) : NativeElement(context) {
    @Composable
    override fun Content(modifier: Modifier) {
        val gap = (currentTheme.theme.gap * multiplier).toDp()
        // On the web the gap is a class's min-width and min-height, which the space's own replace.
        val minWidth = if (sizeConstraints?.minWidth == null) gap else 0.dp
        val minHeight = if (sizeConstraints?.minHeight == null) gap else 0.dp
        // Ahead of the element's own sizing, so that like the web's min-width it wins over a smaller explicit size.
        Spacer(Modifier.sizeIn(minWidth = minWidth, minHeight = minHeight).then(modifier))
    }
}
