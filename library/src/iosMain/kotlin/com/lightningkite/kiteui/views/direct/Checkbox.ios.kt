package com.lightningkite.kiteui.views.direct


import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.l2.icon
import kotlinx.cinterop.ExperimentalForeignApi


actual class Checkbox actual constructor(context: RContext) : RView(context) {
    override val native: FrameLayoutButton = FrameLayoutButton(this)
    actual inline var enabled: Boolean
        get() = native.enabled
        set(value) {
            native.enabled = value
        }
    private val _checked = Property(false)
    actual val checked: ImmediateWritable<Boolean> get() = _checked

    init {
        themeChoice = ThemeDerivation {
            it.copy(
                id = "rad",
                outline = it.foreground,
                outlineWidth = maxOf(it.outlineWidth, 1.dp),
                spacing = it.spacing / 4,
                derivations = mapOf(
                    SelectedSemantic to { it.withBack },
                    UnselectedSemantic to { it.withBack },
                )
            ).withBack
        }
        icon(Icon.done, "") {
            ::visible.invoke { checked() }
        }
    }

    init {
        onRemove(native.observe("highlighted", { refreshTheming() }))
        onRemove(native.observe("selected", { refreshTheming() }))
        onRemove(native.observe("enabled", { refreshTheming() }))
        _checked.addListener { refreshTheming() }
        native.onClick = {
            _checked.value = !_checked.value
        }
    }

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
