// by Claude
package com.lightningkite.kiteui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import com.lightningkite.kiteui.models.ImageLocal
import com.lightningkite.kiteui.models.ImageRaw
import com.lightningkite.kiteui.views.AndroidAppContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

actual suspend fun ImageLocal.compressed(
    maxWidth: Int,
    maxHeight: Int,
    quality: Float
): ImageRaw = withContext(Dispatchers.IO) {
    val contentResolver = AndroidAppContext.applicationCtx.contentResolver

    // Read EXIF orientation before decoding pixels
    val exifOrientation = contentResolver.openInputStream(file.uri)?.use { stream ->
        try {
            ExifInterface(stream).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
        } catch (_: Exception) {
            ExifInterface.ORIENTATION_NORMAL
        }
    } ?: ExifInterface.ORIENTATION_NORMAL

    // Pre-decode to get dimensions only, then calculate inSampleSize for OOM prevention
    val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    contentResolver.openInputStream(file.uri)?.use { stream ->
        BitmapFactory.decodeStream(stream, null, boundsOptions)
    }
    val inSampleSize = calculateInSampleSize(
        boundsOptions.outWidth, boundsOptions.outHeight, maxWidth, maxHeight
    )

    // Decode at reduced resolution
    val decodeOptions = BitmapFactory.Options().apply { this.inSampleSize = inSampleSize }
    val originalBitmap = contentResolver.openInputStream(file.uri)?.use { stream ->
        BitmapFactory.decodeStream(stream, null, decodeOptions)
    } ?: throw IllegalArgumentException("Could not decode image from ${file.uri}")

    // Apply EXIF rotation
    val rotatedBitmap = applyExifOrientation(originalBitmap, exifOrientation)
    if (rotatedBitmap !== originalBitmap) originalBitmap.recycle()

    // Calculate final target size
    val (targetW, targetH) = calculateScaledSize(
        rotatedBitmap.width, rotatedBitmap.height, maxWidth, maxHeight
    )

    // Scale if needed
    val scaledBitmap = if (targetW == rotatedBitmap.width && targetH == rotatedBitmap.height) {
        rotatedBitmap
    } else {
        val scaled = Bitmap.createScaledBitmap(rotatedBitmap, targetW, targetH, true)
        rotatedBitmap.recycle()
        scaled
    }

    // Compress to JPEG
    val outputStream = ByteArrayOutputStream()
    scaledBitmap.compress(
        Bitmap.CompressFormat.JPEG,
        (quality * 100).toInt().coerceIn(0, 100),
        outputStream
    )
    scaledBitmap.recycle()

    ImageRaw(Blob(outputStream.toByteArray(), "image/jpeg"))
}

private fun calculateInSampleSize(width: Int, height: Int, maxWidth: Int, maxHeight: Int): Int {
    var sampleSize = 1
    while (width / (sampleSize * 2) >= maxWidth && height / (sampleSize * 2) >= maxHeight) {
        sampleSize *= 2
    }
    return sampleSize
}

private fun applyExifOrientation(bitmap: Bitmap, orientation: Int): Bitmap {
    val matrix = Matrix()
    when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
        ExifInterface.ORIENTATION_TRANSPOSE -> {
            matrix.postRotate(90f)
            matrix.preScale(-1f, 1f)
        }
        ExifInterface.ORIENTATION_TRANSVERSE -> {
            matrix.postRotate(270f)
            matrix.preScale(-1f, 1f)
        }
        else -> return bitmap
    }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}
