package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.reactive.Action
import com.lightningkite.readable.await
import com.lightningkite.readable.invoke
import com.lightningkite.readable.onRemove
import com.lightningkite.kiteui.views.*
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.launch
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIControlEventTouchUpInside
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.ref.WeakReference

actual class ExternalLink actual constructor(context: RContext): RView(context) {
    override val native = FrameLayoutButton()
    init {
        onRemove(native.setOnClick {
            to?.let { UIApplication.sharedApplication.openURL(NSURL(string = it), mapOf<Any?, Any?>()) {} }
            launch { onNavigate() }
        })
    }


    actual var to: String? = null
    actual var newTab: Boolean = false
    private var onNavigate: suspend () -> Unit = {}
    actual fun onNavigate(action: suspend () -> Unit): Unit {
        onNavigate = action
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

    override fun applyState(theme: ThemeAndBack): ThemeAndBack {
        var t = theme[ClickableSemantic]
        if(!enabled) t = t[DisabledSemantic]
        if(native.highlighted) t = t[DownSemantic]
        if(native.focused) t = t[FocusSemantic]
        return super.applyState(t)
    }
}
