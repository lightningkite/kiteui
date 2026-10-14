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

public actual abstract class RawImageViewLike(context: ElementContext, public actual val source: ImageSource, public actual val description: String, public actual val scaleType: ImageScaleType) : NativeElement(context) {
    public actual abstract val state: Reactive<Unit>
}

public actual class RawImageView actual constructor(context: ElementContext, source: ImageSource, description: String, scaleType: ImageScaleType) : RawImageViewLike(context, source, description, scaleType) {
    init { throw NotImplementedError("Compose backend: RawImageView is not implemented yet") }

    actual override val state: Reactive<Unit> = Constant(Unit)

    @Composable
    override fun Content(modifier: Modifier) {}
}

public actual class SizelessRawImageView actual constructor(context: ElementContext, source: ImageSource, description: String, scaleType: ImageScaleType) : RawImageViewLike(context, source, description, scaleType) {
    init { throw NotImplementedError("Compose backend: SizelessRawImageView is not implemented yet") }

    actual override val state: Reactive<Unit> = Constant(Unit)

    @Composable
    override fun Content(modifier: Modifier) {}
}

public actual class RawImageViewZoomable actual constructor(context: ElementContext, source: ImageSource, description: String, scaleType: ImageScaleType) : RawImageViewLike(context, source, description, scaleType) {
    init { throw NotImplementedError("Compose backend: RawImageViewZoomable is not implemented yet") }

    actual override val state: Reactive<Unit> = Constant(Unit)
    public actual val zoomState: MutableReactiveValue<ZoomState> = Signal(ZoomState())

    @Composable
    override fun Content(modifier: Modifier) {}
}

public actual class ZoomState
