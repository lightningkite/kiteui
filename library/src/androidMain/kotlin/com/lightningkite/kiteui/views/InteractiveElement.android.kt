package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.ExperimentalKiteUi
import com.lightningkite.kiteui.models.ClickableSemantic
import com.lightningkite.kiteui.models.DisabledSemantic
import com.lightningkite.kiteui.models.ThemeDerivation

private val enabledTheming = NativeElementCommonCode.ThemePipeline.Operation.Variable { e ->
    val e = e.underlyingNativeElement as InteractiveElement
    if (!e.enabled) DisabledSemantic else ThemeDerivation.None
}

@OptIn(ExperimentalKiteUi::class)
actual abstract class NativeInteractiveElement actual constructor(context: ElementContext) : NativeElement(context), InteractiveElement {
    actual override var enabled: Boolean
        get() = native.isEnabled
        set(value) {
            native.isEnabled = value
            refreshTheming()
        }

    init {
        themePipeline.add(ThemePipeline.Step.elementStatus, enabledTheming)
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
        themePipeline.add(ThemePipeline.Step.elementStatus, enabledTheming)
    }
}