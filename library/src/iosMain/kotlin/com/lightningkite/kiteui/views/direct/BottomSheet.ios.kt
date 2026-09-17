package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.DialogSemantic
import com.lightningkite.kiteui.models.Icon
import com.lightningkite.kiteui.models.ScreenTransitions
import com.lightningkite.kiteui.views.ElementWriter
import com.lightningkite.kiteui.views.closePopovers
import com.lightningkite.kiteui.views.expanding
import com.lightningkite.kiteui.views.l2.rawPopover
import com.lightningkite.kiteui.views.themed
import com.lightningkite.kiteui.views.ElementContext

public actual fun ElementContext.openBottomSheet(
    halfScreenRatio: Float,
    dim: Boolean,
    view: ElementWriter.CanAddTheme.() -> Unit
) {
    // TODO: native bottom sheet
    rawPopover(ScreenTransitions.VerticalSlide) {
        col {
            expanding.space()
            expanding.themed(DialogSemantic).col {
                row {
                    expanding.space()
                    button {
                        icon(Icon.close, "close")
                        onClick { context.closePopovers() }
                    }
                }
                expanding.view()
            }
        }
    }
}