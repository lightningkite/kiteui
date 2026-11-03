package com.lightningkite.kiteui.camera

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.lightningkite.kiteui.models.ImageLocal
import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.NativeElement
import com.lightningkite.reactive.core.MutableReactive
import com.lightningkite.reactive.core.Signal

// TODO: webcam support.
actual class CameraPreview actual constructor(context: ElementContext) : NativeElement(context) {
    init {
        throw NotImplementedError("Compose backend: CameraPreview is not implemented yet")
    }

    actual val hasPermissions: MutableReactive<Boolean> = Signal(false)
    actual suspend fun capture(): ImageLocal? = null
    actual fun onBarcode(formats: Set<BarcodeFormat>, action: (List<BarcodeResult>) -> Unit) {}

    @Composable
    override fun Content(modifier: Modifier) {}
}
