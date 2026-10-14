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

public actual class LocalTimeField actual constructor(context: ElementContext) : NativeElementWithAction(context) {
    init { throw NotImplementedError("Compose backend: LocalTimeField is not implemented yet") }

    public actual val content: MutableReactiveValue<LocalTime?> = Signal(null)
    public actual var range: ClosedRange<LocalTime>? = null

    @Composable
    override fun Content(modifier: Modifier) {}
}
