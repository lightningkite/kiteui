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
 */
public class Interactions internal constructor(private val element: NativeElementCommonCode) {
    public val source: MutableInteractionSource = MutableInteractionSource()
    public var enabled: Boolean by mutableStateOf(true)
    private var hovered = false
    private var pressed = false
    private var focused = false

    internal val theming = NativeElementCommonCode.ThemePipeline.ThemeForElement {
        var t: ThemeDerivation = ThemeDerivation.None
        if (!enabled) t += DisabledSemantic
        if (hovered) t += HoverSemantic
        if (pressed) t += DownSemantic
        if (focused) t += FocusSemantic
        t
    }

    /** Call from the element's composable, alongside a modifier that uses [source]. */
    @Composable
    public fun Track() {
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
    public val interactions: Interactions = Interactions(this)

    actual override var enabled: Boolean
        get() = interactions.enabled
        set(value) {
            interactions.enabled = value
            refreshTheming()
        }

    init {
        themePipeline.add(ThemePipeline.Step.elementStyling, ClickableSemantic)
        themePipeline.add(ThemePipeline.Step.elementStatus, interactions.theming)
    }
}

public actual abstract class NativeInteractiveContainerElement actual constructor(context: ElementContext) : NativeContainerElement(context), InteractiveElement {
    public val interactions: Interactions = Interactions(this)

    actual override var enabled: Boolean
        get() = interactions.enabled
        set(value) {
            interactions.enabled = value
            refreshTheming()
        }

    init {
        themePipeline.add(ThemePipeline.Step.elementStyling, ClickableSemantic)
        themePipeline.add(ThemePipeline.Step.elementStatus, interactions.theming)
    }
}
