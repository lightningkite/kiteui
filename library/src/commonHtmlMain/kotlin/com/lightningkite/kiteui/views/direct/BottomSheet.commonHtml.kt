package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.DialogSemantic
import com.lightningkite.kiteui.models.Icon
import com.lightningkite.kiteui.models.ScreenTransitions
import com.lightningkite.kiteui.views.ViewModifiable
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.closePopovers
import com.lightningkite.kiteui.views.expanding
import com.lightningkite.kiteui.views.l2.icon
import com.lightningkite.kiteui.views.l2.rawPopover

actual fun ViewWriter.openBottomSheet(
    halfScreenRatio: Float,
    dim: Boolean,
    view: ViewWriter.() -> ViewModifiable
){
    rawPopover(ScreenTransitions.VerticalSlide) {
        col {
            expanding - space()
            expanding - DialogSemantic.onNext - col {
                row {
                    expanding - space()
                    button {
                        icon(Icon.close, "close")
                        onClick { closePopovers() }
                    }
                }
                expanding - view()
            }
        }
    }
}