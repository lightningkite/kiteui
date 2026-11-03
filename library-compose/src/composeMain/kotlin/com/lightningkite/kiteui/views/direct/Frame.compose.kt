package com.lightningkite.kiteui.views.direct

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.FrameMeasurePolicy
import com.lightningkite.kiteui.views.NativeContainerElement

public actual class Frame actual constructor(context: ElementContext) : NativeContainerElement(context) {
    @Composable
    override fun Content(modifier: Modifier) {
        Layout(content = { ShownChildren() }, modifier = modifier, measurePolicy = FrameMeasurePolicy)
    }
}
