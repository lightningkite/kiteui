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

public actual class DismissBackground actual constructor(context: ElementContext) : NativeContainerElement(context) {
    init { throw NotImplementedError("Compose backend: DismissBackground is not implemented yet") }

    public actual fun onClick(action: suspend () -> Unit) {}

    @Composable
    override fun Content(modifier: Modifier) {}
}
