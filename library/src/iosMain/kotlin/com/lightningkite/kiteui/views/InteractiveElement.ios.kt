package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.ExperimentalKiteUi
import com.lightningkite.kiteui.models.ClickableSemantic
import com.lightningkite.kiteui.models.DisabledSemantic
import com.lightningkite.kiteui.models.DownSemantic
import com.lightningkite.kiteui.models.FocusSemantic
import com.lightningkite.kiteui.models.ThemeDerivation
import platform.UIKit.UIControl

@OptIn(ExperimentalKiteUi::class)
actual abstract class NativeInteractiveElement actual constructor(context: ElementContext) : NativeElement(context), InteractiveElement {
    abstract val control: UIControl // TODO: Can this just be an override on native? Do all ios interactives have UIControl natives?

    actual override var enabled: Boolean
        get() = control.enabled
        set(value) {
            control.enabled = value
        }

    init {
        elementSpecificTheming += ElementSpecificTheming {
            var t: ThemeDerivation = ClickableSemantic
            if (!enabled) t += DisabledSemantic
            if (control.highlighted) t += DownSemantic
            if (native.focused) t += FocusSemantic
            t
        }
    }
}

@ExperimentalKiteUi
actual abstract class NativeInteractiveContainerElement actual constructor(context: ElementContext) : NativeContainerElement(context), InteractiveElement {
    abstract val control: UIControl // TODO: Can this just be an override on native? Do all ios interactives have UIControl natives?

    actual override var enabled: Boolean
        get() = control.enabled
        set(value) {
            control.enabled = value
        }

    init {
        elementSpecificTheming += ElementSpecificTheming {
            var t: ThemeDerivation = ClickableSemantic
            if (!enabled) t += DisabledSemantic
            if (control.highlighted) t += DownSemantic
            if (native.focused) t += FocusSemantic
            t
        }
    }
}