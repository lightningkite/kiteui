package com.lightningkite.kiteui.views.direct

import android.content.res.ColorStateList
import android.widget.ProgressBar
import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView

actual class ActivityIndicator actual constructor(context: RContext): RView(context) {
    override val native = ProgressBar(context.activity)
    override fun applyForeground(theme: Theme) {
        native.indeterminateTintList = ColorStateList.valueOf(theme.foreground.colorInt())
    }
}