package com.lightningkite.kiteui.views.direct

import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.navigation.PageNavigator
import com.lightningkite.kiteui.navigation.pageNavigator
import com.lightningkite.kiteui.views.*
import kotlinx.coroutines.launch

public actual class Link actual constructor(context: ElementContext) : NativeContainerElementWithSecondaryAction(context) {
    override val driverActions: AiDriver.Actions get() = super.driverActions + linkDriverActions()

    public actual var to: (() -> Page)? = null
    public actual var onNavigator: PageNavigator = context.pageNavigator
    // A desktop app has no tabs, so this navigates in place, as on Android.
    public actual var newTab: Boolean = false
    public actual var resetsStack: Boolean = false

    override fun inputModifier(takesInput: Boolean): Modifier = Modifier.clickable(
        interactionSource = interactions.source,
        indication = null,
        enabled = enabled && takesInput,
    ) {
        launch {
            action?.startAction(this)
            to?.invoke()?.let { page ->
                onNavigateAction?.startAction(this)
                if (resetsStack) onNavigator.reset(page) else onNavigator.navigate(page)
            }
        }
    }

    @Composable
    override fun Content(modifier: Modifier) {
        Layout(content = { ShownChildren() }, modifier = modifier, measurePolicy = FrameMeasurePolicy)
    }
}
