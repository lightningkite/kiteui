package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.models.PopoverPreferredDirection
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.navigation.dialogPageNavigator
import com.lightningkite.kiteui.navigation.pageNavigator
import com.lightningkite.kiteui.views.direct.Button
import com.lightningkite.kiteui.views.direct.dismissBackground
import com.lightningkite.kiteui.views.direct.frame
import com.lightningkite.kiteui.views.direct.onClick
import kotlinx.cinterop.ObjCAction
import platform.UIKit.UITapGestureRecognizer
import platform.darwin.NSObject
import platform.objc.sel_registerName


@ViewModifierDsl3
public actual fun ElementWriter.hasPopover(
    requiresClick: Boolean,
    preferredDirection: PopoverPreferredDirection,
    setup: ViewWriter.() -> Unit
): ElementWriter {
    return beforeSetup {
        val originalNavigator = context.pageNavigator
        fun openDialog() {
            context.dialogPageNavigator.navigate(object : Page {
                override fun ElementWriter.CanAddTheme.render(): Unit = run {
                    dismissBackground {
                        centered.frame {
                            with(split()) {
                                context.pageNavigator = originalNavigator
                                setup()
                            }
                        }
                    }
                }
            })
        }
        if (this is Button) {
            onClick { openDialog() }
        } else {
            val actionHolder = object : NSObject() {
                @ObjCAction
                fun eventHandler() = openDialog()
            }
            val rec = UITapGestureRecognizer(actionHolder, sel_registerName("eventHandler"))
            native.addGestureRecognizer(rec)
        }
    }
}