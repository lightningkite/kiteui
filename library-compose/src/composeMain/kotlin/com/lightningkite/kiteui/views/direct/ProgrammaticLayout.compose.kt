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

public actual class ProgrammaticLayout actual constructor(context: ElementContext) : NativeContainerElement(context), LinearLayoutElement {
    init { throw NotImplementedError("Compose backend: ProgrammaticLayout is not implemented yet") }

    actual override var gap: Dimension? = null
    public actual var delegate: ProgrammaticLayoutDelegate = ProgrammaticLayoutDelegate.AllFull
    public actual fun invalidateLayout() {}
    @Deprecated("Will probably be removed in the future.")
    actual override val spacingForChildCornerRadii: Dimension get() = 0.px

    @Composable
    override fun Content(modifier: Modifier) {}
}
