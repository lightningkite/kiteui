package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.ThemeAndBack
import com.lightningkite.kiteui.models.times
import com.lightningkite.kiteui.views.ClickThroughPanel
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView
import com.lightningkite.kiteui.views.px
import java.awt.Dimension as AwtDimension
import javax.swing.JPanel
import kotlin.math.roundToInt

actual class Space actual constructor(context: RContext, private val multiplier: Double) : RView(context) {
    // Use ClickThroughPanel so spacing doesn't block clicks from parent Link/Button
    override val native = ClickThroughPanel().apply {
        isOpaque = false
    }

    override fun applyTheme(theme: ThemeAndBack) {
        super.applyTheme(theme)
        val size = ((theme.theme.gap * multiplier).px).roundToInt()
        val dimension = AwtDimension(size, size)
        native.minimumSize = dimension
        native.preferredSize = dimension
        native.maximumSize = dimension
    }
}
