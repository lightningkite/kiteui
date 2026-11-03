package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.DialogSemantic
import com.lightningkite.kiteui.models.Icon
import com.lightningkite.kiteui.models.ScreenTransitions
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.l2.rawPopover

@Deprecated("Use coordinatorFrame instead.")
public actual fun ElementContext.openBottomSheet(halfScreenRatio: Float, dim: Boolean, view: ElementWriter.CanAddTheme.() -> Unit) {
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
