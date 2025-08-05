package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.InternalKiteUi
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.reactive.Action
import com.lightningkite.kiteui.views.*
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.ref.WeakReference
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.launch
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIControlEventTouchUpInside

@InternalKiteUi
public actual class ExternalLink public actual constructor(context: RContext): RView(context) {
    override val native: FrameLayoutButton = FrameLayoutButton()
    init {
        onRemove(native.setOnClick {
            to?.let { to ->
                launch {
                    onNavigate()
                    UIApplication.sharedApplication.openURL(NSURL(string = to), mapOf<Any?, Any?>()) {}
                }
            }
        })
    }


    public actual var to: String? = null
    public actual var newTab: Boolean = false
    private var onNavigate: suspend () -> Unit = {}
    public actual fun onNavigate(action: suspend () -> Unit): Unit {
        onNavigate = action
    }

    public actual var enabled: Boolean
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
