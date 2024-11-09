package com.lightningkite.kiteui.views.direct


import com.lightningkite.kiteui.models.CardSemantic
import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.models.card
import com.lightningkite.kiteui.views.*
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreGraphics.CGRectMake
import platform.UIKit.UIProgressView
import platform.UIKit.UIView


actual class CircularProgress actual constructor(context: RContext) : RView(context) {
    override val native = TODO("Not implemented yet")
    override fun applyForeground(theme: Theme) {
        super.applyForeground(theme)


    }
    actual var ratio: Float
        get() =TODO("Not implemented yet")
        set(value) { TODO("Not implemented yet") }
}
