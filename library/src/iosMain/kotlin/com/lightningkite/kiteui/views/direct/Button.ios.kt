package com.lightningkite.kiteui.views.direct


import com.lightningkite.kiteui.launchManualCancel
import com.lightningkite.kiteui.models.SizeConstraints
import com.lightningkite.kiteui.reactive.await
import com.lightningkite.kiteui.reactive.invoke
import com.lightningkite.kiteui.views.*
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSKeyValueObservingOptionNew
import platform.Foundation.addObserver
import platform.UIKit.UIControlEventTouchUpInside
import platform.UIKit.UIView
import platform.objc.sel_registerName
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.ref.WeakReference

@OptIn(ExperimentalForeignApi::class, ExperimentalNativeApi::class)
actual class Button actual constructor(context: RContext): RView(context) {
    init { if(useBackground == UseBackground.No) useBackground = UseBackground.IfChanged }
    override val native = FrameLayoutButton(this)
    init {
        activityIndicator {
            ::opacity.invoke { if (this@Button.working.await()) 1.0 else 0.0 }
            native.extensionSizeConstraints = SizeConstraints(minWidth = null, minHeight = null)
        }
    }

    actual fun onClick(action: suspend () -> Unit): Unit {
        native.onClick = action
    }

    actual var enabled: Boolean
        get() = native.enabled
        set(value) {
            native.enabled = value
        }

    init {
        onRemove(native.observe("highlighted", { refreshTheming() }))
        onRemove(native.observe("selected", { refreshTheming() }))
        onRemove(native.observe("enabled", { refreshTheming() }))
    }
    override fun getStateThemeChoice() = when {
        !enabled -> ThemeChoice.Derive { it.disabled() }
        native.highlighted -> ThemeChoice.Derive { it.down() }
        native.focused -> ThemeChoice.Derive { it.hover() }
        else -> null
    }
}