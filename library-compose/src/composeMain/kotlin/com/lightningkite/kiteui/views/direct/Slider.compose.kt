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

public actual class Slider actual constructor(context: ElementContext) : NativeInteractiveElement(context) {
    init { throw NotImplementedError("Compose backend: Slider is not implemented yet") }

    public actual val value: MutableReactiveValue<Float> = Signal(0f)
    public actual var min: Float = 0f
    public actual var max: Float = 1f
    public actual var step: Float? = null

    @Composable
    override fun Content(modifier: Modifier) {}
}
