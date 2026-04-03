package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.ExperimentalKiteUi
import com.lightningkite.kiteui.models.ClickableSemantic
import com.lightningkite.kiteui.models.DisabledSemantic
import com.lightningkite.kiteui.models.DownSemantic
import com.lightningkite.kiteui.models.FocusSemantic
import com.lightningkite.kiteui.models.ThemeDerivation
import com.lightningkite.kiteui.views.direct.observe
import com.lightningkite.reactive.context.onRemove
import platform.UIKit.UIControl

// NOTE: If you edit these make sure to edit TextArea as well, it does not inherit but should have similar behavior

@OptIn(ExperimentalKiteUi::class)
actual abstract class NativeInteractiveElement actual constructor(context: ElementContext) : NativeElement(context), InteractiveElement {
    abstract val control: UIControl

    actual override var enabled: Boolean
        get() = control.enabled
        set(value) {
            control.enabled = value
            refreshTheming()
        }

    init {
        elementSpecificTheming += ElementSpecificTheming {
            var t: ThemeDerivation = ClickableSemantic
            if (!enabled) t += DisabledSemantic
            if (control.highlighted) t += DownSemantic
            if (control.focused) t += FocusSemantic
            t
        }
    }

    init {
        // todo: Maybe should be native.observe?
        onRemove(control.observe("highlighted") { refreshTheming() })
        onRemove(control.observe("selected") { refreshTheming() })
        onRemove(control.observe("enabled") { refreshTheming() })
    }
}

@OptIn(ExperimentalKiteUi::class)
actual abstract class NativeInteractiveContainerElement actual constructor(context: ElementContext) : NativeContainerElement(context), InteractiveElement {
    abstract val control: UIControl

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
            if (control.focused) t += FocusSemantic
            t
        }
    }

    init {
        onRemove(control.observe("highlighted") { refreshTheming() })
        onRemove(control.observe("selected") { refreshTheming() })
        onRemove(control.observe("enabled") { refreshTheming() })
    }
}