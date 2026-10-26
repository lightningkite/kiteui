@file:OptIn(ExperimentalKiteUi::class)

package com.lightningkite.kiteui.views

import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.HoverInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.lightningkite.kiteui.ExperimentalKiteUi
import com.lightningkite.kiteui.models.*

/**
 * Hover, press and focus state for an interactive element.  Feeds KiteUI's theme pipeline (as iOS does
 * with UIControl state), so hover and pressed themes come from the same semantics as on every platform.
 * [NativeElement.Render] tracks it; the element's input modifiers report to [source].
 */
public class Interactions internal constructor(private val element: NativeElementCommonCode) {
    public val source: MutableInteractionSource = MutableInteractionSource()
    private var enabledState by mutableStateOf(true)
    public var enabled: Boolean
        get() = enabledState
        set(value) {
            enabledState = value
            element.refreshTheming()
        }
    private var hovered = false
    private var pressed = false
    private var focused = false

    init {
        element.themePipeline.add(NativeElementCommonCode.ThemePipeline.Step.elementStyling, ClickableSemantic)
        element.themePipeline.add(NativeElementCommonCode.ThemePipeline.Step.elementStatus, NativeElementCommonCode.ThemePipeline.ThemeForElement {
            var t: ThemeDerivation = ThemeDerivation.None
            if (!enabled) t += DisabledSemantic
            if (hovered) t += HoverSemantic
            if (pressed) t += DownSemantic
            if (focused) t += FocusSemantic
            t
        })
    }

    @Composable
    internal fun Track() {
        LaunchedEffect(source) {
            source.interactions.collect {
                when (it) {
                    is HoverInteraction.Enter -> hovered = true
                    is HoverInteraction.Exit -> hovered = false
                    is PressInteraction.Press -> pressed = true
                    is PressInteraction.Release, is PressInteraction.Cancel -> pressed = false
                    is FocusInteraction.Focus -> focused = true
                    is FocusInteraction.Unfocus -> focused = false
                }
                element.refreshTheming()
            }
        }
        // Leaving composition cancels the collector, which can miss the matching Exit/Release.
        // It also happens when the element was removed, which leaves nothing to re-theme.
        DisposableEffect(source) {
            onDispose {
                hovered = false
                pressed = false
                focused = false
                if (!element.isShutdown) element.refreshTheming()
            }
        }
    }
}

public actual abstract class NativeInteractiveElement actual constructor(context: ElementContext) : NativeElement(context), InteractiveElement {
    override val interactions: Interactions = Interactions(this)
    actual override var enabled: Boolean by interactions::enabled
    public actual open var required: Boolean = false
}

public actual abstract class NativeInteractiveContainerElement actual constructor(context: ElementContext) : NativeContainerElement(context), InteractiveElement {
    override val interactions: Interactions = Interactions(this)
    actual override var enabled: Boolean by interactions::enabled
    public actual open var accessibleExpanded: Boolean? = null
    public actual open var accessibleOpensDialog: Boolean = false
}
