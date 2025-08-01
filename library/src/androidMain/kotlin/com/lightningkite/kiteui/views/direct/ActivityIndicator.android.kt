package com.lightningkite.kiteui.views.direct

import android.content.res.ColorStateList
import android.widget.ProgressBar
import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.models.ThemeAndBack
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView

public actual class ActivityIndicator public actual constructor(context: RContext): RView(context) {
    override val native: ProgressBar = ProgressBar(context.activity)
    override fun applyTheme(theme: ThemeAndBack) { super.applyTheme(theme); val theme = theme.theme
        native.indeterminateTintList = ColorStateList.valueOf(theme.foreground.colorInt())
    }
}