package com.lightningkite.kiteui.views.direct

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.navigation.*
import com.lightningkite.kiteui.reactive.Action
import com.lightningkite.kiteui.views.*
import com.lightningkite.reactive.core.*
import kotlinx.datetime.*
import kotlin.time.Duration

public actual class Link actual constructor(context: ElementContext) : NativeContainerElementWithSecondaryAction(context) {
    init { throw NotImplementedError("Compose backend: Link is not implemented yet") }

    public actual var to: (() -> Page)? = null
    public actual var onNavigator: PageNavigator = context.pageNavigator
    public actual var newTab: Boolean = false
    public actual var resetsStack: Boolean = false

    @Composable
    override fun Content(modifier: Modifier) {}
}
