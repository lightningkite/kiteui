package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.*
import com.lightningkite.signal.*
import com.lightningkite.kiteui.views.*
import kotlinx.cinterop.ExperimentalForeignApi
import platform.UIKit.UIView



public actual class ToggleButton public actual constructor(context: RContext) : RView(context) {
    override val native: FrameLayoutButton = FrameLayoutButton()
    override fun childTouches(side: Side, child: RView): Boolean {
        return when(side) {
            Side.Left -> child.native.extensionHorizontalAlign?.touchesStart != false
            Side.Top -> child.native.extensionVerticalAlign?.touchesStart != false
            Side.Right -> child.native.extensionHorizontalAlign?.touchesEnd != false
            Side.Bottom -> child.native.extensionVerticalAlign?.touchesEnd != false
        }
    }
    public actual inline var enabled: Boolean
        get() = native.enabled
        set(value) {
            native.enabled = value
        }
    private val _checked = Property(false)
    public actual val checked: ImmediateWritable<Boolean> get() = _checked

    init {
        onRemove(native.observe("highlighted", { refreshTheming() }))
        onRemove(native.observe("selected", { refreshTheming() }))
        onRemove(native.observe("enabled", { refreshTheming() }))
        _checked.addListener { refreshTheming() }
        onRemove(native.setOnClick {
            _checked.value = !_checked.value
        })
    }
    override fun applyState(theme: ThemeAndBack): ThemeAndBack {
        var t = theme[ClickableSemantic]
        if(_checked.value) t = t[SelectedSemantic]
        else t = t[UnselectedSemantic]
        if(!enabled) t = t[DisabledSemantic]
        if(native.highlighted) t = t[DownSemantic]
        if(native.focused) t = t[FocusSemantic]
        return super.applyState(t)
    }
}
