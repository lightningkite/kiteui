package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.reactive.ImmediateWritable
import com.lightningkite.kiteui.reactive.Property
import com.lightningkite.kiteui.reactive.Writable
import com.lightningkite.kiteui.reactive.await
import com.lightningkite.kiteui.views.*
import kotlinx.cinterop.ExperimentalForeignApi
import platform.UIKit.UIView


@OptIn(ExperimentalForeignApi::class)
actual class ToggleButton actual constructor(context: RContext) : RView(context) {
    override val native: FrameLayoutButton = FrameLayoutButton(this)
    actual inline var enabled: Boolean
        get() = native.enabled
        set(value) {
            native.enabled = value
        }
    private val _checked = Property(false)
    actual val checked: ImmediateWritable<Boolean> get() = _checked

    init {
        onRemove(native.observe("highlighted", { refreshTheming() }))
        onRemove(native.observe("selected", { refreshTheming() }))
        onRemove(native.observe("enabled", { refreshTheming() }))
        _checked.addListener { refreshTheming() }
        native.onClick = {
            _checked.value = !_checked.value
        }
    }
    override fun hasAlternateBackedStates(): Boolean = true
    override fun applyState(theme: ThemeAndBack): ThemeAndBack {
        var t = theme
        if(_checked.value) t = t[SelectedSemantic]
        else t = t[UnselectedSemantic]
        if(!enabled) t = t[DisabledSemantic]
        if(native.highlighted) t = t[DownSemantic]
        if(native.focused) t = t[FocusSemantic]
        return t
    }
}
