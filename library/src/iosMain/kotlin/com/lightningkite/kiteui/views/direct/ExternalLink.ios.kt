package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.reactive.Action
import com.lightningkite.kiteui.reactive.await
import com.lightningkite.kiteui.reactive.invoke
import com.lightningkite.kiteui.reactive.onRemove
import com.lightningkite.kiteui.views.*
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.launch
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIControlEventTouchUpInside
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.ref.WeakReference

actual class ExternalLink actual constructor(context: RContext): RView(context) {
    override val native = FrameLayoutButton(this)
    init {
        native.onClick = {
            to?.let { UIApplication.sharedApplication.openURL(NSURL(string = it)) }
            launch { onNavigate() }
        }
    }


    actual var to: String = ""
    actual var newTab: Boolean = false
    private var onNavigate: suspend () -> Unit = {}
    actual fun onNavigate(action: suspend () -> Unit): Unit {
        onNavigate = action
    }

    var enabled: Boolean
        get() = native.enabled
        set(value) {
            native.enabled = value
        }

    init {
        onRemove(native.observe("highlighted", { refreshTheming() }))
        onRemove(native.observe("selected", { refreshTheming() }))
        onRemove(native.observe("enabled", { refreshTheming() }))
    }

    override fun hasAlternateBackedStates(): Boolean = true
    override fun applyState(theme: ThemeAndBack): ThemeAndBack {
        var t = theme
        if(!enabled) t = t[DisabledSemantic]
        if(native.highlighted) t = t[DownSemantic]
        if(native.focused) t = t[FocusSemantic]
        return super.applyState(t)
    }
}
