package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.ExperimentalKiteUi
import com.lightningkite.kiteui.models.ClickableSemantic
import com.lightningkite.kiteui.models.DisabledSemantic

@OptIn(ExperimentalKiteUi::class)
actual abstract class NativeInteractiveElement actual constructor(context: ElementContext) : NativeElement(context), InteractiveElement {
    actual override var enabled: Boolean
        get() = native.isEnabled
        set(value) {
            native.isEnabled = value
            refreshTheming()
        }

    init {
        elementSpecificTheming += ElementSpecificTheming { _ ->
            if (!enabled) ClickableSemantic + DisabledSemantic
            else ClickableSemantic
        }
    }
}

@OptIn(ExperimentalKiteUi::class)
actual abstract class NativeInteractiveContainerElement actual constructor(context: ElementContext) : NativeContainerElement(context), InteractiveElement {
    actual override var enabled: Boolean
        get() = native.isEnabled
        set(value) {
            native.isEnabled = value
            refreshTheming()
        }

    init {
        elementSpecificTheming += ElementSpecificTheming { _ ->
            if (!enabled) ClickableSemantic + DisabledSemantic
            else ClickableSemantic
        }
    }
}