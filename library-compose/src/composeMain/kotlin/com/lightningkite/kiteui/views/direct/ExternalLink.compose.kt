package com.lightningkite.kiteui.views.direct

import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import com.lightningkite.kiteui.externalServices
import com.lightningkite.kiteui.utils.safeLinkUrlOrNull
import com.lightningkite.kiteui.views.*
import kotlinx.coroutines.launch

public actual class ExternalLink actual constructor(context: ElementContext) : NativeContainerElementWithSecondaryAction(context) {
    override val driverActions: AiDriver.Actions get() = super.driverActions + externalLinkDriverActions()

    // Validated here, as on the web and Android: this is the sink that hands the URL to the OS.
    public actual var to: String? = null
        set(value) {
            field = safeLinkUrlOrNull(value)
        }
    public actual var newTab: Boolean = false

    override fun inputModifier(): Modifier = Modifier.clickable(
        interactionSource = interactions.source,
        indication = null,
        enabled = enabled && !ignoreInteraction,
    ) {
        launch {
            action?.startAction(this)
            to?.let {
                onNavigateAction?.startAction(this)
                context.externalServices.openLink(it, newTab)
            }
        }
    }

    @Composable
    override fun Content(modifier: Modifier) {
        interactions.Track()
        Layout(content = { ShownChildren() }, modifier = modifier, measurePolicy = FrameMeasurePolicy)
    }
}
