package com.lightningkite.kiteui.views.direct

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.lightningkite.kiteui.FileReference
import com.lightningkite.kiteui.models.ImageResource
import com.lightningkite.kiteui.views.loadResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Image

internal actual fun decodeImage(bytes: ByteArray): ImageBitmap = Image.makeFromEncoded(bytes).toComposeImageBitmap()
internal actual suspend fun FileReference.readBytes(): ByteArray = withContext(Dispatchers.IO) { file.readBytes() }
internal actual suspend fun ImageResource.readBytes(): ByteArray = withContext(Dispatchers.IO) { loadResource(relativeUrl) }
