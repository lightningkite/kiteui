package com.lightningkite.kiteui.views.direct

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.lightningkite.kiteui.FileReference
import com.lightningkite.kiteui.models.ImageResource
import com.lightningkite.kiteui.views.loadResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.jetbrains.skia.AnimationDisposalMode
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Codec
import org.jetbrains.skia.Data
import org.jetbrains.skia.Image
import kotlin.time.Duration.Companion.milliseconds

internal actual fun decodeImage(bytes: ByteArray): DecodedImage {
    val firstFrame = Image.makeFromEncoded(bytes).toComposeImageBitmap()
    val animated = Codec.makeFromData(Data.makeFromBytes(bytes)).frameCount > 1
    return DecodedImage(firstFrame, if (animated) { { SkiaAnimation(bytes) } } else null)
}

private class SkiaAnimation(bytes: ByteArray) : ImageAnimation {
    private val codec = Codec.makeFromData(Data.makeFromBytes(bytes))
    private val frames = codec.framesInfo
    private val bitmap = Bitmap().apply { allocPixels(codec.imageInfo) }
    private var inBitmap = -1
    private var shown = 0
    private var repeats = 0

    override suspend fun play(show: (ImageBitmap) -> Unit) {
        while (true) {
            // Like browsers, which treat delays this short as unset.
            val duration = frames[shown].duration.let { if (it <= 10) 100 else it }
            delay(duration.milliseconds)
            if (shown == frames.lastIndex) {
                // A negative count repeats forever.
                if (repeats == codec.repetitionCount) return
                repeats++
            }
            val next = (shown + 1) % frames.size
            show(withContext(Dispatchers.Default) { decode(next) })
            shown = next
        }
    }

    // Frames draw over the one before, which the bitmap still holds unless that one asked to be undone.
    private fun decode(frame: Int): ImageBitmap {
        val prior = frame - 1
        val priorUsable = prior >= 0 && inBitmap == prior && frames[prior].disposalMethod != AnimationDisposalMode.RESTORE_PREVIOUS
        codec.readPixels(bitmap, frame, if (priorUsable) prior else -1)
        inBitmap = frame
        // A mutable bitmap's pixels are copied, so the next frame doesn't draw over this one.
        return Image.makeFromBitmap(bitmap).toComposeImageBitmap()
    }
}

internal actual suspend fun FileReference.readBytes(): ByteArray = withContext(Dispatchers.IO) { file.readBytes() }
internal actual suspend fun ImageResource.readBytes(): ByteArray = withContext(Dispatchers.IO) { loadResource(relativeUrl) }
