package com.lightningkite.kiteui.views.direct

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lightningkite.kiteui.models.toDp
import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.NativeElement
import com.lightningkite.kiteui.views.toBrush

public actual class Separator actual constructor(context: ElementContext) : NativeElement(context) {
    @Composable
    override fun Content(modifier: Modifier) {
        val theme = currentTheme.theme
        val thickness = maxOf(theme.outlineWidth.toDp(), 1.dp)
        // Runs across its parent: a vertical bar in a row, a horizontal one anywhere else.
        val inRow = (parent as? NativeLinearLayoutElement)?.layoutVertical == false
        Spacer(
            (if (inRow) modifier.width(thickness) else modifier.height(thickness))
                .background(theme.separator.toBrush())
        )
    }
}
