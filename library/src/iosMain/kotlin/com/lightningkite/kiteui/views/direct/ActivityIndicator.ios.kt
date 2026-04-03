package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.SizeConstraints
import com.lightningkite.kiteui.models.ThemeAndBack
import com.lightningkite.kiteui.models.rem
import com.lightningkite.kiteui.views.*
import platform.UIKit.UIActivityIndicatorView

actual class ActivityIndicator actual constructor(context: ElementContext): NativeElement(context) {
    override val native = UIActivityIndicatorView().apply {
        hidden = false
        startAnimating()
        extensionSizeConstraints = SizeConstraints(minWidth = 1.rem, minHeight = 1.rem)
        userInteractionEnabled = false
    }

    override fun nativeApplyTheme(theme: ThemeAndBack) {
        super.nativeApplyTheme(theme)
        native.color = theme.theme.foreground.closestColor().toUiColor()
    }
}