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

public actual class RadioToggleButton actual constructor(context: ElementContext) : NativeInteractiveContainerElement(context) {
    init { throw NotImplementedError("Compose backend: RadioToggleButton is not implemented yet") }

    public actual val checked: MutableReactiveValue<Boolean> = Signal(false)

    @Composable
    override fun Content(modifier: Modifier) {}
}
