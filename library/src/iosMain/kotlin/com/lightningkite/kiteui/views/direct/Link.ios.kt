package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.navigation.PageNavigator
import com.lightningkite.kiteui.navigation.mainPageNavigator
import com.lightningkite.signal.onRemove
import com.lightningkite.kiteui.views.*
import kotlinx.coroutines.launch


public actual class Link public actual constructor(context: RContext): RView(context) {
    override val native = FrameLayoutButton()
    override fun childTouches(side: Side, child: RView): Boolean {
        return when(side) {
            Side.Left -> child.native.extensionHorizontalAlign?.touchesStart != false
            Side.Top -> child.native.extensionVerticalAlign?.touchesStart != false
            Side.Right -> child.native.extensionHorizontalAlign?.touchesEnd != false
            Side.Bottom -> child.native.extensionVerticalAlign?.touchesEnd != false
        }
    }
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

    public actual var to: (() -> Page)? = null
    public actual var onNavigator: PageNavigator = mainPageNavigator
    public actual var newTab: Boolean = false
    public actual var resetsStack: Boolean = false

    private var onNavigate: (suspend () -> Unit)? = null
    public actual fun onNavigate(action: suspend () -> Unit): Unit {
        onNavigate = action
    }

    private var onClick: (suspend () -> Unit)? = null
    public actual fun onClick(action: suspend () -> Unit): Unit {
        onClick = action
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

