package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.views.ViewDsl
import com.lightningkite.kiteui.views.ViewWriter
import platform.UIKit.UILabel
import platform.UIKit.UIView

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual typealias NLabel = UIView

@ViewDsl
actual inline fun ViewWriter.labelActual(crossinline setup: Label.() -> Unit): Unit = col {
    subtext {  }
    setup(Label(native))
}

actual inline var Label.content: String
    get() = (native.subviews[0] as UILabelWithGradient).label.text ?: ""
    set(value) { (native.subviews[0] as UILabelWithGradient).label.text = value }