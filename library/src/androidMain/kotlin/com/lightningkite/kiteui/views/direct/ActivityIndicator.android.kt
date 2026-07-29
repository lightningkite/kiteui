package com.lightningkite.kiteui.views.direct

import android.content.res.ColorStateList
import android.view.View
import android.widget.ProgressBar
import com.lightningkite.kiteui.models.ThemeAndBack
import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.NativeElement

public actual class ActivityIndicator actual constructor(context: ElementContext): NativeElement(context) {
    override val native = ProgressBar(context.activity).apply {
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
        contentDescription = "Loading"
    }
    override fun nativeApplyTheme(theme: ThemeAndBack) {
        super.nativeApplyTheme(theme)
        native.indeterminateTintList = ColorStateList.valueOf(theme.theme.foreground.colorInt())
    }
}