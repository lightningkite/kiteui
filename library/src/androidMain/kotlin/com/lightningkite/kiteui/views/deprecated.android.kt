package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.models.PopoverPreferredDirection
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.navigation.dialogPageNavigator
import com.lightningkite.kiteui.views.direct.dismissBackground
import com.lightningkite.kiteui.views.direct.frame

@Suppress("DEPRECATION")
@ViewModifierDsl3
public actual fun ElementWriter.hasPopover(
    requiresClick: Boolean,
    preferredDirection: PopoverPreferredDirection,
    setup: ViewWriter.() -> Unit,
): ElementWriter {
    return beforeSetup {
        native.setOnClickListener {
            context.dialogPageNavigator.navigate(Page.Direct {
                dismissBackground {
                    centered.frame {
                        setup()
                    }
                }
            })
        }
    }
}