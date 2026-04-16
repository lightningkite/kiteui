package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.ExperimentalKiteUi
import com.lightningkite.kiteui.models.ClickableSemantic
import com.lightningkite.kiteui.models.DisabledSemantic
import com.lightningkite.kiteui.models.ThemeDerivation

private val enabledTheming = NativeElementCommonCode.ThemePipeline.ThemeForElement { e ->
    if (!e.native.isEnabled) DisabledSemantic else ThemeDerivation.None
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
        themePipeline.add(ThemePipeline.Step.elementStyling, ClickableSemantic)
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
        themePipeline.add(ThemePipeline.Step.elementStyling, ClickableSemantic)
        themePipeline.add(ThemePipeline.Step.elementStatus, enabledTheming)
    }
}