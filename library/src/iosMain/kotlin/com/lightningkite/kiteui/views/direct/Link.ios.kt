package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.navigation.PageNavigator
import com.lightningkite.kiteui.navigation.mainPageNavigator
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*
import kotlinx.coroutines.launch


actual class Link actual constructor(context: RContext): RView(context) {
    override val native = FrameLayoutButton()
    init {
        onRemove(native.setOnClick {
            onClick?.let { launch { it() } }
            to?.invoke()?.let { it ->
                launch {
                    onNavigate?.invoke()
                    if (resetsStack) {
                        onNavigator.reset(it)
                    } else {
                        onNavigator.navigate(it)
                    }
                }
            }
        })
    }

    actual var to: (() -> Page)? = null
    actual var onNavigator: PageNavigator = mainPageNavigator
    actual var newTab: Boolean = false
    actual var resetsStack: Boolean = false

    private var onNavigate: (suspend () -> Unit)? = null
    actual fun onNavigate(action: suspend () -> Unit): Unit {
        onNavigate = action
    }

    private var onClick: (suspend () -> Unit)? = null
    actual fun onClick(action: suspend () -> Unit): Unit {
        onClick = action
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

    // by Claude - Link supports click for AI driver navigation
    override val accessibilityActions: Set<String> get() = setOf("click")
    override fun performAccessibilityAction(action: String, value: String?): String? = when (action) {
        "click" -> {
            onClick?.let { launch { it() } }
            to?.invoke()?.let { destination ->
                launch {
                    onNavigate?.invoke()
                    if (resetsStack) onNavigator.reset(destination) else onNavigator.navigate(destination)
                }
            }
            null
        }
        else -> super.performAccessibilityAction(action, value)
    }
}

